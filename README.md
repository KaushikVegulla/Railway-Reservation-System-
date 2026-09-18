# Railway Reservation System (RailOne)

Android app inspired by **RailOne (CRIS)** / IRCTC Rail Connect.  
Academic demo — **not affiliated with IRCTC, CRIS, or Indian Railways**.

Repository: https://github.com/KaushikVegulla/Railway-Reservation-System-

## Features

| Area | How it works |
|---|---|
| Register / login | Name, email, password. 6-digit code via **Resend**. Works on any internet (4G or Wi‑Fi). No PC server. |
| Train search | Live **RailRadar** between stations |
| Seat vacancy | 14-day chart + fare |
| PNR / live status | RailRadar enquiry |
| Payment | **Razorpay** test checkout: UPI, cards, netbanking |
| Ticket | Local e-ticket after verified payment |

## Run the app

1. Android Studio → **File → Open** → `Railway-Reservation-System-`
2. Gradle sync
3. Run `app` on a phone or emulator

Min SDK 24. Target SDK 35.

You do **not** need `python3 backend/server.py` for login or payment. Those call Resend and Razorpay over HTTPS from the app.

Test card: `4111 1111 1111 1111`, any future expiry, any CVV.

## Project layout

```
app/src/main/java/com/kaushik/railway/
  MainActivity.kt     navigation (starts at login)
  AppViewModel.kt     booking state
  data/               RailRadar, Resend, Razorpay
  ui/screens/         login, home, trains, payment, PNR
backend/              optional local payment server (not required)
```

## Disclaimer

UI and train data are for learning only. Do not use this for real reservations.
