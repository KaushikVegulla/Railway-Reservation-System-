# RailOne payment backend (Razorpay test)

## APIs

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

Keys are loaded from `.env` (test key from Razorpay dashboard). Never ship `KEY_SECRET` in the Android app.
