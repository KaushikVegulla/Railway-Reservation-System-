# Railway Reservation System (Rail Connect)

Android mobile app inspired by **IRCTC Rail Connect**. Academic / demo project — **not affiliated with IRCTC or Indian Railways**.

Repository: https://github.com/KaushikVegulla/Railway-Reservation-System-

## Live RailKit data

The app calls [RailKit](https://railkit.in) for real-time Indian Railways data:

- Train search between stations
- Seat availability + multi-day vacancy chart
- PNR status
- Live running status
- Fare lookup used at booking

**Ticket booking:** RailKit has no IRCTC payment/booking API. The app uses live availability and fare, then saves a **local e-ticket**. Real reserved tickets must still be booked on IRCTC.

## What you can do

1. Login / register (demo user is pre-filled)
2. Search trains (From / To / date / class / quota)
3. See availability (AVAILABLE / RAC / WL) and fares
4. Add passengers (up to 6), berth preference, contact
5. Review fare + optional travel insurance
6. Demo payment (UPI, eWallet, cards, net banking)
7. e-Ticket with PNR
8. My bookings + cancel ticket
9. PNR enquiry
10. Live running status

## Open in Android Studio

1. Open Android Studio
2. **File → Open** → this folder (`Railway-Reservation-System-`)
3. Wait for Gradle sync
4. Run on an emulator or phone (`app` configuration)

Minimum SDK 24 (Android 7). Target SDK 35.

## Project layout

```
app/src/main/java/com/kaushik/railway/
  MainActivity.kt          # navigation
  AppViewModel.kt          # booking state
  data/                    # models + mock trains/stations
  ui/screens/              # login, home, trains, passengers, payment, PNR
  ui/theme/                # navy + orange IRCTC-like palette
```

## Disclaimer

Names, train numbers, and UI patterns are used only as a **learning reference**. Do not use this app for real reservations. Book tickets only on the official IRCTC website or Rail Connect app.
