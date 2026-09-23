# Railway Reservation System (RailOne) v1.2

Android app inspired by **RailOne (CRIS)** / IRCTC Rail Connect.

Academic demo — **not affiliated with IRCTC, CRIS, or Indian Railways**.

**Repo:** https://github.com/KaushikVegulla/Railway-Reservation-System-

---

## v1.2 — Full improvement pass

### Security ✅
- RailRadar API key via `BuildConfig` + `local.properties` (never in source)
- Razorpay **KEY_SECRET removed from the app** — create-order + verify only via backend
- Secrets gitignored

### Architecture ✅
- `TrainRepository`, `BookingRepository`, `AuthRepository`
- `SessionStore` (DataStore Preferences) for login persistence
- `UiState` sealed class for future expansion
- Cleaner ViewModel that delegates to repositories

### Persistence ✅
- **Room** database for bookings (survives app restart)
- **DataStore** for user session (name, email, mobile, logged-in)

### Features ✅
- Editable **Profile** screen (name + mobile)
- **Berth preference chips** on passenger form
- Add / remove passengers (up to 6)
- Booking cancel with **confirmation** step
- Session restore on cold start
- Auth flows wired to backend + DataStore

### Backend
- Python server for auth (Resend OTP) + Razorpay
- Starts even without Razorpay keys (auth still works)

---

## Features overview

| Area | How it works |
|------|--------------|
| Register / login | Email + password, 6-digit OTP via Resend |
| Session | Persisted with DataStore |
| Train search | Live RailRadar |
| Seat vacancy | 14-day chart + fare |
| PNR / live status | RailRadar |
| Passengers | Multi-pax, berth chips, concessions |
| Payment | Razorpay test via backend |
| Ticket | Local e-ticket + Room persistence |
| Profile | Edit name/mobile, logout |
| Bookings | History + cancel with confirm |

---

## Setup

### 1. Clone
```bash
git clone https://github.com/KaushikVegulla/Railway-Reservation-System-.git
```

### 2. Secrets (`local.properties`)
```bash
cp local.properties.example local.properties
```
```properties
sdk.dir=/path/to/Android/sdk
railradar.api.key=rg_your_key_here
# optional:
# backend.base.url=http://10.0.2.2:8088
```

### 3. Backend
```bash
cd backend
# .env:
#   RAZORPAY_KEY_ID=...
#   RAZORPAY_KEY_SECRET=...
#   RESEND_API_KEY=...
#   RESEND_FROM=RailOne <you@domain.com>
python3 server.py
```
Port **8088**. Emulator → `http://10.0.2.2:8088`.

### 4. Run
Android Studio → Sync → Run.  
Min SDK 24 · Target 35 · version **1.2.0**

Test card: `4111 1111 1111 1111`, any future expiry, any CVV.

---

## Project layout

```
app/src/main/java/com/kaushik/railway/
  AppViewModel.kt
  MainActivity.kt
  RailApp.kt
  data/
    AuthApi.kt
    SessionStore.kt          # DataStore session
    PaymentApi.kt            # backend-only secrets
    RailKitClient.kt         # BuildConfig API key
    Models.kt / MockData.kt
    db/                      # Room
    repository/              # Auth, Booking, Train
  util/UiState.kt
  ui/screens/ ...
backend/server.py
```

---

## Remaining (optional future)

| Item | Notes |
|------|--------|
| Hilt DI | Can replace manual repository construction |
| Seat map visual grid | Currently preference chips |
| FCM notifications | Train status / booking alerts |
| FastAPI + Postgres | Replace file-based users.json |
| Offline cache | Station / recent trains |
| Unit + UI tests | JUnit / Compose |
| CI (GitHub Actions) | Build on PR |

---

## Disclaimer

For learning only. Do not use for real reservations.
