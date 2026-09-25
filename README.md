# Railway Reservation System (RailOne) v1.5

## v1.5.0 — IRCTC-style account and Tatkal link

Academic walkthrough of the account flow: user ID, password rules, captcha (or OTP when the visually impaired option is on), masked email and mobile, demo OTPs, profile (gender, date of birth, address), 4-digit MPIN, then a local Aadhaar / VID check before Tatkal search.

- Accounts stay on the device. Passwords and the MPIN are stored as hashes.
- The Aadhaar or VID value is not saved and is not sent to UIDAI. Only the last 4 digits remain.
- PAN does not turn on Tatkal. General quota search does not ask for this link.
- Not an official IRCTC, CRIS, or UIDAI service.

## v1.4.0 — Improvements (product plan)

## v1.4.0 — Improvements (product plan)

- **One-tap rebook** last journey on Home
- **Saved passengers** load from last booking
- **Offline-first search** — MockData fallback when API empty/fails
- **System dark mode** support
- **Share e-ticket** via Android share sheet
- **Trust banners** — clear demo / not IRCTC ticket messaging
- Keys stay masked in `local.properties` (no backend required)


Android app inspired by **RailOne (CRIS)** / IRCTC Rail Connect.

Academic demo — **not affiliated with IRCTC, CRIS, or Indian Railways**.

**Repo:** https://github.com/KaushikVegulla/Railway-Reservation-System-

---

## v1.4 highlights

### Masked API keys (no backend required)
All secrets live in **`local.properties`** (gitignored) and are injected via **BuildConfig**:

| Property | Purpose |
|----------|---------|
| `railradar.api.key` | Live train / PNR / availability |
| `razorpay.key.id` | Razorpay test key id |
| `razorpay.key.secret` | Razorpay test secret (demo only) |
| `use.local.keys=true` | Payments run on-device with masked keys |

**Nothing secret is committed to git.**

### Features
- Live RailRadar search, vacancy chart, PNR, running status  
- **Interactive seat map** (demo coach layout)  
- Multi-passenger + berth chips  
- Optional **return journey** date  
- Razorpay test checkout (local masked keys)  
- Room booking history + cancel with confirm  
- DataStore login session  
- Auth works offline (local demo users) when no server is running  
- Offline station suggestions from MockData  
- Loading skeletons component  
- Unit tests for core models  

### Optional backend
The existing `backend/` folder is **optional**. With `use.local.keys=true` the app does **not** need it for payments. Auth falls back to an in-memory local demo if the server is unreachable.

---

## Quick setup

```bash
git clone https://github.com/KaushikVegulla/Railway-Reservation-System-.git
cp local.properties.example local.properties
```

Edit `local.properties`:

```properties
sdk.dir=/path/to/Android/sdk
railradar.api.key=rg_your_key
razorpay.key.id=rzp_test_xxx
razorpay.key.secret=your_test_secret
use.local.keys=true
```

Open in Android Studio → Sync → Run.

- Min SDK 24 · Target 35 · **version 1.5.0**
- Test card: `4111 1111 1111 1111`, any future expiry, any CVV

---

## Layout

```
app/.../railway/
  AppViewModel.kt
  data/
    AuthApi.kt          # backend + local demo fallback
    PaymentApi.kt       # masked keys via BuildConfig
    RailKitClient.kt
    SessionStore.kt
    db/  repository/
  ui/components/SeatMap.kt
  ui/screens/...
backend/                # optional only
```

---

## Disclaimer

Learning project only. Do not use for real reservations. Client-side Razorpay secret is for **test keys** in an academic demo — never ship production secrets in an app.