# Railway Reservation System (RailOne) v1.1

Android app inspired by **RailOne (CRIS)** / IRCTC Rail Connect.

Academic demo — **not affiliated with IRCTC, CRIS, or Indian Railways**.

Repository: https://github.com/KaushikVegulla/Railway-Reservation-System-

---

## What's New in v1.1 (Improvement Plan Phase 0–2)

### Security (Critical)
- **RailRadar API key** moved out of source → injected via `BuildConfig` from `local.properties`.
- **Razorpay KEY_SECRET removed from the Android app**. All order creation + signature verification now go through the backend only.
- Secrets are never committed (see `local.properties.example`).

### Architecture & Persistence
- Added **Room database** for permanent booking history.
- Introduced `BookingRepository` + `BookingEntity` / `BookingDao`.
- `AppViewModel` now loads and saves bookings via the repository (survives app restarts).
- Cleaner separation of concerns ready for further repository/UseCase expansion.

### Version
- `versionName = 1.1.0`, `versionCode = 2`

---

## Features

| Area | How it works |
|------|--------------|
| Register / login | Name, email, password. 6-digit code via **Resend**. |
| Train search | Live **RailRadar** between stations |
| Seat vacancy | 14-day chart + fare |
| PNR / live status | RailRadar enquiry |
| Payment | **Razorpay** test checkout via backend (UPI, cards, netbanking) |
| Ticket | Local e-ticket after verified payment + **persisted in Room** |

---

## Setup

### 1. Clone & open in Android Studio
```bash
git clone https://github.com/KaushikVegulla/Railway-Reservation-System-.git
```

### 2. Configure secrets
```bash
cp local.properties.example local.properties
# Edit local.properties and set:
#   railradar.api.key=rg_xxxxx
```

### 3. Backend (required for payments & optional for auth)
```bash
cd backend
# Create .env with:
#   RAZORPAY_KEY_ID=rzp_test_...
#   RAZORPAY_KEY_SECRET=...
#   RESEND_API_KEY=re_...
#   RESEND_FROM=RailOne <you@yourdomain.com>
python3 server.py
```
Default port **8088**. Emulator reaches it at `http://10.0.2.2:8088`.

### 4. Run the app
Android Studio → Gradle sync → Run on device/emulator.

Min SDK 24 · Target SDK 35 · Java 17

Test card: `4111 1111 1111 1111`, any future expiry, any CVV.

---

## Project layout (updated)

```
app/src/main/java/com/kaushik/railway/
  MainActivity.kt
  AppViewModel.kt          # now uses BookingRepository
  RailApp.kt
  data/
    Models.kt
    RailKitClient.kt       # API key from BuildConfig
    PaymentApi.kt          # all secrets via backend
    MockData.kt
    db/
      AppDatabase.kt
      BookingEntity.kt
      BookingDao.kt
    repository/
      BookingRepository.kt
  ui/screens/              # Auth, Home, Trains, Booking, Enquiry
backend/                   # Python server (auth + Razorpay)
```

---

## Remaining Roadmap (to full production-quality demo)

| Phase | Status | Items |
|-------|--------|-------|
| 0 Security | ✅ Done | API key + Razorpay secret out of client |
| 1 Architecture | 🟡 Partial | Repository for bookings; full Hilt + UseCases next |
| 2 Persistence | ✅ Done | Room for bookings; DataStore for session next |
| 3 Backend upgrade | ⬜ | FastAPI + PostgreSQL / proper JWT |
| 4 Features | ⬜ | Seat map, better cancellation, FCM, profile, return journey |
| 5 UX polish | ⬜ | Skeletons, offline, accessibility |
| 6 Testing & release | ⬜ | Unit/UI tests, CI, Crashlytics, Play Store |

---

## Disclaimer

UI and train data are for learning only. Do not use this for real reservations.

---

## License

Academic / educational use.
