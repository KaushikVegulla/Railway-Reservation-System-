#!/usr/bin/env python3
"""RailOne payment backend — Razorpay Create Order + Verify Payment."""

from __future__ import annotations

import base64
import hashlib
import hmac
import json
import os
import ssl
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
PORT = int(os.environ.get("PORT", "8088"))
RAZORPAY_ORDERS = "https://api.razorpay.com/v1/orders"

# order_id -> {amount_paise, currency, receipt}
ORDERS: dict[str, dict] = {}
# verified payments
PAYMENTS: dict[str, dict] = {}


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


class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt: str, *args) -> None:
        print("[pay]", fmt % args)

    def _send(self, code: int, payload: dict) -> None:
        data = json.dumps(payload).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
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
                    "service": "railone-payments",
                    "keyIdPresent": bool(KEY_ID),
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
            self._create_order(body)
            return
        if self.path in ("/verify-payment", "/api/verify-payment"):
            self._verify(body)
            return
        self._send(404, {"success": False, "error": "Not found"})

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


def main() -> None:
    if not KEY_ID or not KEY_SECRET:
        raise SystemExit("Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET in backend/.env")
    httpd = ThreadingHTTPServer(("0.0.0.0", PORT), Handler)
    print(f"RailOne payments listening on http://0.0.0.0:{PORT}")
    print("  POST /create-order   {amount: <INR>}")
    print("  POST /verify-payment {razorpay_payment_id, razorpay_order_id, razorpay_signature}")
    httpd.serve_forever()


if __name__ == "__main__":
    main()
