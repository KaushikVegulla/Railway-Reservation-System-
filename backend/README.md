# RailOne backend (auth + Razorpay test)

## Auth (Resend email OTP)

| API | Body | Notes |
|---|---|---|
| `POST /register` | `{name, email, password}` | Creates account, emails a 6-digit code |
| `POST /verify-email` | `{email, code}` | Marks the account verified |
| `POST /login` | `{email, password}` | 403 + `needsVerification` if email not verified |
| `POST /resend-code` | `{email}` | Sends a new 10-minute OTP |

Resend’s onboarding sender (`beth.t@example.com`) can only deliver to the email on the Resend account. Verify a domain in Resend and set `RESEND_FROM` to send to any address.

## Payments

### Create Order
`POST /create-order`  
Body: `{ "amount": 1670 }` (INR rupees)  
Creates a Razorpay order and returns `order_id`, `key_id`, `amount` (paise).

### Verify Payment
`POST /verify-payment`  
Body: `{ "razorpay_payment_id", "razorpay_order_id", "razorpay_signature" }`  
HMAC-SHA256 verifies the signature, then confirms the booking.

## Run

```bash
cd backend
python3 server.py
```

Default port **8088**. Android emulator uses `http://10.0.2.2:8088`.

Keys are loaded from `.env`. Never ship `KEY_SECRET` or `RESEND_API_KEY` in the Android app.
