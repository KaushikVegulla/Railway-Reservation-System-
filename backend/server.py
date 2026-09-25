#!/usr/bin/env python3
"""RailOne payment backend — Razorpay Create Order + Verify Payment."""

from __future__ import annotations

import base64
import hashlib
import hmac
import json
import os
import random
import ssl
import time
import urllib.error
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

ROOT = Path(__file__).resolve().parent
env_path = ROOT / ".env"
if env_path.exists():
    for line in env_path.read_text().splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        os.environ.setdefault(k.strip(), v.strip())

KEY_ID = os.environ.get("RAZORPAY_KEY_ID", "")
KEY_SECRET = os.environ.get("RAZORPAY_KEY_SECRET", "")
RESEND_API_KEY = os.environ.get("RESEND_API_KEY", "")
RESEND_FROM = os.environ.get("RESEND_FROM", "RailX <beth.t@example.com>")
PORT = int(os.environ.get("PORT", "8088"))
COGNITO_REGION = os.environ.get("COGNITO_REGION", "ap-south-1")
COGNITO_USER_POOL_ID = os.environ.get("COGNITO_USER_POOL_ID", "ap-south-1_SspUmQyId")
COGNITO_CLIENT_ID = os.environ.get("COGNITO_CLIENT_ID", "5s0vjm6vk4lttl2s326qh9mlmt")
COGNITO_REQUIRE_JWT = os.environ.get("COGNITO_REQUIRE_JWT", "1").lower() not in ("0", "false", "no")
RAZORPAY_ORDERS = "https://api.razorpay.com/v1/orders"
USERS_FILE = ROOT / "users.json"

ORDERS: dict[str, dict] = {}
PAYMENTS: dict[str, dict] = {}
OTP_TTL_SEC = 10 * 60


def load_users() -> dict:
    if USERS_FILE.exists():
        try:
            return json.loads(USERS_FILE.read_text())
        except json.JSONDecodeError:
            return {}
    return {}


def save_users(users: dict) -> None:
    USERS_FILE.write_text(json.dumps(users, indent=2))


USERS = load_users()


def hash_password(password: str, salt: str | None = None) -> tuple[str, str]:
    salt = salt or hashlib.sha256(os.urandom(16)).hexdigest()[:16]
    digest = hashlib.pbkdf2_hmac("sha256", password.encode(), salt.encode(), 120_000).hex()
    return salt, digest


def send_resend_email(to_email: str, subject: str, html: str) -> dict:
    if not RESEND_API_KEY:
        raise RuntimeError("RESEND_API_KEY not set")
    payload = json.dumps({"from": RESEND_FROM, "to": [to_email], "subject": subject, "html": html}).encode()
    req = urllib.request.Request(
        "https://api.resend.com/emails",
        data=payload,
        method="POST",
        headers={
            "Authorization": f"Bearer {RESEND_API_KEY}",
            "Content-Type": "application/json",
        },
    )
    ctx = ssl.create_default_context()
    with urllib.request.urlopen(req, context=ctx, timeout=30) as resp:
        return json.loads(resp.read().decode())


def issue_otp(email: str, name: str) -> str:
    code = f"{random.randint(0, 999999):06d}"
    user = USERS[email]
    user["otp"] = code
    user["otpExpires"] = time.time() + OTP_TTL_SEC
    save_users(USERS)
    html = f"""
    <div style="font-family:sans-serif;max-width:480px">
      <h2 style="color:#0A3D91">RailOne email verification</h2>
      <p>Hi {name or 'traveller'},</p>
      <p>Your verification code is:</p>
      <p style="font-size:28px;font-weight:bold;letter-spacing:6px;color:#F57C00">{code}</p>
      <p>This code expires in 10 minutes.</p>
      <p style="color:#666;font-size:12px">Centre for Railway Information Systems — student demo</p>
    </div>
    """
    send_resend_email(email, "Verify your RailOne account", html)
    return code


def _auth_header() -> str:
    token = base64.b64encode(f"{KEY_ID}:{KEY_SECRET}".encode()).decode()
    return f"Basic {token}"


def razorpay_create_order(amount_paise: int, receipt: str) -> dict:
    body = json.dumps(
        {
            "amount": amount_paise,
            "currency": "INR",
            "receipt": receipt[:40],
            "payment_capture": 1,
        }
    ).encode()
    req = urllib.request.Request(
        RAZORPAY_ORDERS,
        data=body,
        method="POST",
        headers={
            "Authorization": _auth_header(),
            "Content-Type": "application/json",
        },
    )
    ctx = ssl.create_default_context()
    with urllib.request.urlopen(req, context=ctx, timeout=30) as resp:
        return json.loads(resp.read().decode())


def verify_signature(order_id: str, payment_id: str, signature: str) -> bool:
    msg = f"{order_id}|{payment_id}".encode()
    expected = hmac.new(KEY_SECRET.encode(), msg, hashlib.sha256).hexdigest()
    return hmac.compare_digest(expected, signature or "")


_JWKS: dict = {"keys": None, "fetched": 0.0}


def _b64url(data: str) -> bytes:
    pad = "=" * (-len(data) % 4)
    return base64.urlsafe_b64decode(data + pad)


def verify_cognito_jwt(token: str) -> dict:
    """Validate a Cognito ID or access token. Pool id and client id are public app config."""
    parts = token.split(".")
    if len(parts) != 3 or not all(parts):
        raise ValueError("Malformed token")
    header = json.loads(_b64url(parts[0]))
    if header.get("alg") != "RS256":
        raise ValueError("Unexpected alg")
    payload = json.loads(_b64url(parts[1]))
    now = time.time()
    if _JWKS["keys"] is None or now - float(_JWKS["fetched"]) > 3600:
        url = (
            f"https://cognito-idp.{COGNITO_REGION}.amazonaws.com/"
            f"{COGNITO_USER_POOL_ID}/.well-known/jwks.json"
        )
        with urllib.request.urlopen(url, timeout=10) as resp:
            _JWKS["keys"] = json.loads(resp.read().decode())["keys"]
            _JWKS["fetched"] = now
    kid = header.get("kid")
    jwk = next((key for key in _JWKS["keys"] if key.get("kid") == kid), None)
    if jwk is None:
        raise ValueError("Unknown signing key")
    from cryptography.hazmat.primitives import hashes
    from cryptography.hazmat.primitives.asymmetric import padding, rsa

    numbers = rsa.RSAPublicNumbers(
        int.from_bytes(_b64url(jwk["e"]), "big"),
        int.from_bytes(_b64url(jwk["n"]), "big"),
    )
    numbers.public_key().verify(
        _b64url(parts[2]),
        f"{parts[0]}.{parts[1]}".encode(),
        padding.PKCS1v15(),
        hashes.SHA256(),
    )
    issuer = f"https://cognito-idp.{COGNITO_REGION}.amazonaws.com/{COGNITO_USER_POOL_ID}"
    if payload.get("iss") != issuer:
        raise ValueError("Bad issuer")
    if int(payload.get("exp", 0)) < time.time():
        raise ValueError("Token expired")
    use = payload.get("token_use")
    if use == "id" and payload.get("aud") != COGNITO_CLIENT_ID:
        raise ValueError("Bad audience")
    if use == "access" and payload.get("client_id") != COGNITO_CLIENT_ID:
        raise ValueError("Bad client")
    if use not in ("id", "access"):
        raise ValueError("Bad token_use")
    return payload


class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt: str, *args) -> None:
        print("[pay]", fmt % args)

    def _send(self, code: int, payload: dict) -> None:
        data = json.dumps(payload).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_OPTIONS(self) -> None:  # noqa: N802
        self._send(200, {"ok": True})

    def do_GET(self) -> None:  # noqa: N802
        if self.path in ("/", "/health"):
            self._send(
                200,
                {
                    "ok": True,
                    "service": "railx-backend",
                    "auth": "cognito",
                    "userPoolId": COGNITO_USER_POOL_ID,
                    "keyIdPresent": bool(KEY_ID),
                    "resendPresent": bool(RESEND_API_KEY),
                },
            )
            return
        self._send(404, {"success": False, "error": "Not found"})

    def _read_json(self) -> dict:
        length = int(self.headers.get("Content-Length") or 0)
        raw = self.rfile.read(length) if length else b"{}"
        if not raw:
            return {}
        return json.loads(raw.decode())

    def do_POST(self) -> None:  # noqa: N802
        try:
            body = self._read_json()
        except json.JSONDecodeError:
            self._send(400, {"success": False, "error": "Invalid JSON"})
            return

        if self.path in ("/create-order", "/api/create-order"):
            if self._require_user() is None:
                return
            self._create_order(body)
            return
        if self.path in ("/verify-payment", "/api/verify-payment"):
            if self._require_user() is None:
                return
            self._verify(body)
            return
        if self.path in ("/register", "/api/register", "/verify-email", "/api/verify-email", "/login", "/api/login", "/resend-code", "/api/resend-code"):
            self._send(
                410,
                {
                    "success": False,
                    "error": "Account auth moved to Amazon Cognito (RailX-Users). Sign up in the Android app.",
                },
            )
            return
        self._send(404, {"success": False, "error": "Not found"})

    def _require_user(self) -> dict | None:
        if not COGNITO_REQUIRE_JWT:
            return {}
        header = self.headers.get("Authorization") or ""
        if not header.startswith("Bearer "):
            self._send(401, {"success": False, "error": "Cognito sign-in required"})
            return None
        try:
            return verify_cognito_jwt(header[7:].strip())
        except ValueError as exc:
            self._send(401, {"success": False, "error": str(exc)})
            return None
        except Exception:
            self._send(401, {"success": False, "error": "Invalid Cognito token"})
            return None

    def _create_order(self, body: dict) -> None:
        if not KEY_ID or not KEY_SECRET:
            self._send(500, {"success": False, "error": "Razorpay keys not configured"})
            return
        try:
            rupees = float(body.get("amount", 0))
        except (TypeError, ValueError):
            self._send(400, {"success": False, "error": "amount is required (INR)"})
            return
        if rupees < 1:
            rupees = 1
        paise = int(round(rupees * 100))
        receipt = str(body.get("receipt") or f"railone_{paise}")
        try:
            order = razorpay_create_order(paise, receipt)
        except urllib.error.HTTPError as e:
            err = e.read().decode()[:400]
            self._send(e.code, {"success": False, "error": err or str(e)})
            return
        except Exception as e:  # noqa: BLE001
            self._send(502, {"success": False, "error": str(e)})
            return
        order_id = order.get("id", "")
        ORDERS[order_id] = {"amount": paise, "receipt": receipt}
        self._send(
            200,
            {
                "success": True,
                "order_id": order_id,
                "amount": paise,
                "currency": order.get("currency", "INR"),
                "key_id": KEY_ID,
            },
        )

    def _verify(self, body: dict) -> None:
        payment_id = str(body.get("razorpay_payment_id") or "")
        order_id = str(body.get("razorpay_order_id") or "")
        signature = str(body.get("razorpay_signature") or "")
        if not (payment_id and order_id and signature):
            self._send(
                400,
                {
                    "success": False,
                    "error": "razorpay_payment_id, razorpay_order_id, razorpay_signature required",
                },
            )
            return
        if not verify_signature(order_id, payment_id, signature):
            self._send(400, {"success": False, "verified": False, "error": "Invalid signature"})
            return
        PAYMENTS[payment_id] = {"order_id": order_id, "verified": True}
        self._send(
            200,
            {
                "success": True,
                "verified": True,
                "razorpay_payment_id": payment_id,
                "razorpay_order_id": order_id,
                "bookingConfirmed": True,
            },
        )

    def _register(self, body: dict) -> None:
        name = str(body.get("name") or "").strip()
        email = str(body.get("email") or "").strip().lower()
        password = str(body.get("password") or "")
        if not name or "@" not in email or len(password) < 6:
            self._send(400, {"success": False, "error": "Name, valid email, and password (6+ chars) required"})
            return
        existing = USERS.get(email)
        if existing and existing.get("verified"):
            self._send(409, {"success": False, "error": "Email already registered. Please login."})
            return
        salt, digest = hash_password(password)
        USERS[email] = {
            "name": name,
            "email": email,
            "salt": salt,
            "passwordHash": digest,
            "verified": False,
        }
        save_users(USERS)
        try:
            issue_otp(email, name)
        except urllib.error.HTTPError as e:
            err = e.read().decode()[:400]
            self._send(502, {"success": False, "error": f"Could not send email: {err}"})
            return
        except Exception as e:  # noqa: BLE001
            self._send(502, {"success": False, "error": f"Could not send email: {e}"})
            return
        self._send(200, {"success": True, "email": email, "message": "Verification code sent to email"})

    def _verify_email(self, body: dict) -> None:
        email = str(body.get("email") or "").strip().lower()
        code = str(body.get("code") or "").strip()
        user = USERS.get(email)
        if not user:
            self._send(404, {"success": False, "error": "No account for this email"})
            return
        if user.get("verified"):
            self._send(200, {"success": True, "verified": True, "name": user["name"], "email": email})
            return
        if not user.get("otp") or time.time() > float(user.get("otpExpires") or 0):
            self._send(400, {"success": False, "error": "Code expired. Request a new one."})
            return
        if user.get("otp") != code:
            self._send(400, {"success": False, "error": "Invalid verification code"})
            return
        user["verified"] = True
        user.pop("otp", None)
        user.pop("otpExpires", None)
        save_users(USERS)
        self._send(200, {"success": True, "verified": True, "name": user["name"], "email": email})

    def _login(self, body: dict) -> None:
        email = str(body.get("email") or "").strip().lower()
        password = str(body.get("password") or "")
        user = USERS.get(email)
        if not user:
            self._send(401, {"success": False, "error": "Invalid email or password"})
            return
        salt, digest = hash_password(password, user.get("salt"))
        if digest != user.get("passwordHash"):
            self._send(401, {"success": False, "error": "Invalid email or password"})
            return
        if not user.get("verified"):
            try:
                issue_otp(email, user.get("name", ""))
            except Exception:
                pass
            self._send(
                403,
                {
                    "success": False,
                    "needsVerification": True,
                    "email": email,
                    "error": "Email not verified. We sent a new code.",
                },
            )
            return
        self._send(200, {"success": True, "name": user["name"], "email": email})

    def _resend(self, body: dict) -> None:
        email = str(body.get("email") or "").strip().lower()
        user = USERS.get(email)
        if not user:
            self._send(404, {"success": False, "error": "No account for this email"})
            return
        try:
            issue_otp(email, user.get("name", ""))
        except Exception as e:  # noqa: BLE001
            self._send(502, {"success": False, "error": str(e)})
            return
        self._send(200, {"success": True, "message": "New code sent"})


def main() -> None:
    if not KEY_ID or not KEY_SECRET:
        print("WARNING: RAZORPAY keys missing — payment endpoints will fail. Auth still works.")
    if not RESEND_API_KEY:
        print("WARNING: RESEND_API_KEY missing — email OTP will fail.")
    httpd = ThreadingHTTPServer(("0.0.0.0", PORT), Handler)
    print(f"RailX backend listening on http://0.0.0.0:{PORT}")
    print("  Auth is Amazon Cognito. /register /login /verify-email return 410.")
    print("  POST /create-order   Authorization: Bearer <cognito id token>  {amount}")
    print("  POST /verify-payment Authorization: Bearer <cognito id token>")
    httpd.serve_forever()


if __name__ == "__main__":
    main()
