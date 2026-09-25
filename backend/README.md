# RailX backend (Cognito JWT + Razorpay test)

Account sign-up, email OTP, and sign-in are Amazon Cognito (user pool `RailX-Users` in `ap-south-1`).

`POST /register`, `/verify-email`, `/login`, and `/resend-code` return **410**. The Android app talks to Cognito directly.

Payment routes require `Authorization: Bearer <Cognito ID token>` unless `COGNITO_REQUIRE_JWT=0`.

| Env | Default |
|---|---|
| `COGNITO_REGION` | `ap-south-1` |
| `COGNITO_USER_POOL_ID` | `ap-south-1_SspUmQyId` |
| `COGNITO_CLIENT_ID` | `5s0vjm6vk4lttl2s326qh9mlmt` |
| `COGNITO_REQUIRE_JWT` | `1` |

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
