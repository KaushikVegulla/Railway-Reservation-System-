package com.kaushik.railway

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaushik.railway.data.AvailabilityResult
import com.kaushik.railway.data.Booking
import com.kaushik.railway.data.MockData
import com.kaushik.railway.data.Passenger
import com.kaushik.railway.data.PnrResult
import com.kaushik.railway.data.RunningStop
import com.kaushik.railway.data.SessionStore
import com.kaushik.railway.data.Station
import com.kaushik.railway.data.Train
import com.kaushik.railway.data.TrainClassAvail
import com.kaushik.railway.data.db.AppDatabase
import com.kaushik.railway.data.repository.AuthRepository
import com.kaushik.railway.data.repository.BookingRepository
import com.kaushik.railway.data.repository.TrainRepository
import com.kaushik.railway.util.UiState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

class AppViewModel : ViewModel() {

    private val sessionStore = SessionStore(RailApp.instance)
    private val bookingRepo = BookingRepository(AppDatabase.getInstance(RailApp.instance))
    private val trainRepo = TrainRepository()
    private val authRepo = AuthRepository(sessionStore)

    // Auth / session
    var loggedIn by mutableStateOf(false)
    var userName by mutableStateOf("")
    var userEmail by mutableStateOf("")
    var userMobile by mutableStateOf("")
    var userId by mutableStateOf("")
    var otpEmailed by mutableStateOf(true)
    var otpDisplayCode by mutableStateOf<String?>(null)
    var authLoading by mutableStateOf(false)
    var authError by mutableStateOf<String?>(null)
    var needsVerification by mutableStateOf(false)
    var pendingVerifyEmail by mutableStateOf("")

    // Search
    var fromCode by mutableStateOf("NDLS")
    var toCode by mutableStateOf("MMCT")
    var journeyDate by mutableStateOf(defaultDate())
    var selectedClass by mutableStateOf("All Classes")
    var selectedQuota by mutableStateOf("GN - General")
    var returnDate by mutableStateOf<String?>(null) // Phase 4: return journey support

    var selectedTrain by mutableStateOf<Train?>(null)
    var selectedTravelClass by mutableStateOf<TrainClassAvail?>(null)
    var vacancy by mutableStateOf<AvailabilityResult?>(null)
    var passengers = mutableStateListOf(Passenger())
    var mobile by mutableStateOf("")
    var email by mutableStateOf("")
    var insurance by mutableStateOf(true)
    val selectedSeatIds = mutableStateListOf<String>()
    var selectedSeatLabels by mutableStateOf("")
    var lastBooking by mutableStateOf<Booking?>(null)
    val bookings = mutableStateListOf<Booking>()

    val trains = mutableStateListOf<Train>()
    var searchLoading by mutableStateOf(false)
    var searchError by mutableStateOf<String?>(null)

    val classRows = mutableStateListOf<TrainClassAvail>()
    var availLoading by mutableStateOf(false)
    var availError by mutableStateOf<String?>(null)

    var pnrLoading by mutableStateOf(false)
    var pnrError by mutableStateOf<String?>(null)
    var pnrResult by mutableStateOf<PnrResult?>(null)

    var runningLoading by mutableStateOf(false)
    var runningError by mutableStateOf<String?>(null)
    val runningStops = mutableStateListOf<RunningStop>()
    var runningNote by mutableStateOf("")

    val stationSuggestions = mutableStateListOf<Station>()

    // Seat / berth preferences (enhanced)
    val berthOptions = listOf("No Preference", "Lower", "Middle", "Upper", "Side Lower", "Side Upper")
    val genderOptions = listOf("Male", "Female", "Other")
    val concessionOptions = listOf("None", "Senior Citizen", "Student", "Divyangjan")

    init {
        // Restore session from DataStore
        viewModelScope.launch {
            authRepo.sessionFlow.collectLatest { session ->
                loggedIn = session.loggedIn
                userName = session.name
                userEmail = session.email
                userMobile = session.mobile
                if (session.mobile.isNotBlank()) mobile = session.mobile
                if (session.email.isNotBlank()) email = session.email
            }
        }
        // Load persisted bookings
        viewModelScope.launch {
            bookingRepo.observeBookings().collectLatest { list ->
                bookings.clear()
                bookings.addAll(list)
            }
        }
    }

    // ── Auth ──────────────────────────────────────────────
    fun register(name: String, email: String, password: String, onSuccess: () -> Unit) {
        authLoading = true
        authError = null
        viewModelScope.launch {
            val result = authRepo.register(name, email, password)
            authLoading = false
            if (result.success) {
                pendingVerifyEmail = email
                needsVerification = true
                onSuccess()
            } else {
                authError = result.error ?: "Registration failed"
            }
        }
    }

    fun verifyEmail(code: String, onSuccess: () -> Unit) {
        authLoading = true
        authError = null
        viewModelScope.launch {
            val result = authRepo.verifyEmail(pendingVerifyEmail, code)
            authLoading = false
            if (result.success) {
                needsVerification = false
                onSuccess()
            } else {
                authError = result.error ?: "Verification failed"
            }
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        authLoading = true
        authError = null
        viewModelScope.launch {
            val result = authRepo.login(email, password)
            authLoading = false
            if (result.success) {
                onSuccess()
            } else if (result.needsVerification) {
                pendingVerifyEmail = email
                needsVerification = true
                authError = result.error
            } else {
                authError = result.error ?: "Login failed"
            }
        }
    }

    fun resendCode() {
        viewModelScope.launch {
            authRepo.resendCode(pendingVerifyEmail)
        }
    }

    fun logout(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            authRepo.logout()
            onDone()
        }
    }

    fun updateProfile(name: String, mobile: String) {
        viewModelScope.launch {
            authRepo.updateProfile(name, mobile)
            this@AppViewModel.mobile = mobile
        }
    }

    // ── Search / Trains ───────────────────────────────────
    fun stationName(code: String) =
        MockData.stations.find { it.code == code }?.let { "${it.name} (${it.code})" }
            ?: stationSuggestions.find { it.code == code }?.let { "${it.name} (${it.code})" }
            ?: code

    fun swapStations() {
        val tmp = fromCode
        fromCode = toCode
        toCode = tmp
    }

    fun quotaCode() = selectedQuota.take(2)

    fun searchTrainsLive() {
        searchLoading = true
        searchError = null
        trains.clear()
        viewModelScope.launch {
            val result = trainRepo.searchTrains(fromCode, toCode, toApiDate(journeyDate))
            searchLoading = false
            result.onSuccess { list ->
                trains.addAll(list)
                if (list.isEmpty()) searchError = "No trains found for this route/date."
            }.onFailure { e ->
                searchError = e.message ?: "Search failed"
            }
        }
    }

    fun loadAvailability(train: Train) {
        selectedTrain = train
        availLoading = true
        availError = null
        classRows.clear()
        vacancy = null
        viewModelScope.launch {
            val classes = if (selectedClass == "All Classes") {
                listOf("1A", "2A", "3A", "3E", "SL", "CC", "EC", "2S")
            } else listOf(selectedClass)
            val result = trainRepo.loadClassAvailability(
                train, toApiDate(journeyDate), classes, quotaCode()
            )
            availLoading = false
            result.onSuccess { list ->
                classRows.addAll(list)
                if (list.isEmpty()) availError = "No availability for ${train.fromCode} → ${train.toCode}."
            }.onFailure { e ->
                availError = e.message
            }
        }
    }

    fun loadVacancyChart(cls: TrainClassAvail) {
        selectedTravelClass = cls
        val train = selectedTrain ?: return
        viewModelScope.launch {
            val result = trainRepo.getVacancy(
                train.number, train.fromCode, train.toCode,
                toApiDate(journeyDate), cls.code, quotaCode()
            )
            result.onSuccess { vacancy = it }
                .onFailure { availError = it.message }
        }
    }

    fun lookupPnr(pnr: String) {
        pnrLoading = true
        pnrError = null
        pnrResult = null
        viewModelScope.launch {
            val result = trainRepo.checkPnr(pnr)
            pnrLoading = false
            result.onSuccess { pnrResult = it }
                .onFailure { pnrError = it.message ?: "PNR lookup failed" }
        }
    }

    fun loadRunning(trainNo: String) {
        runningLoading = true
        runningError = null
        runningStops.clear()
        viewModelScope.launch {
            val result = trainRepo.trackTrain(trainNo, toApiDate(journeyDate))
            runningLoading = false
            result.onSuccess { stops ->
                runningStops.addAll(stops)
                runningNote = if (stops.isEmpty()) "No live timeline for this train." else "Live from RailRadar"
            }.onFailure { runningError = it.message }
        }
    }

    fun searchStations(query: String) {
        viewModelScope.launch {
            val found = trainRepo.searchStations(query)
            stationSuggestions.clear()
            if (found.isNotEmpty()) {
                stationSuggestions.addAll(found)
            } else if (query.length >= 2) {
                // Offline fallback from MockData
                val q = query.lowercase()
                stationSuggestions.addAll(
                    MockData.stations.filter {
                        it.code.lowercase().contains(q) || it.name.lowercase().contains(q)
                    }.take(20)
                )
            }
        }
    }

    fun payableAmount(): Int {
        val cls = selectedTravelClass ?: return 1
        val ins = if (insurance) passengers.size * 15 else 0
        return (cls.fare * passengers.size + ins).coerceAtLeast(1)
    }

    fun confirmBooking(paymentId: String = "", orderId: String = ""): Booking {
        val train = selectedTrain!!
        val cls = selectedTravelClass!!
        val pnr = (1..10).map { Random.nextInt(0, 10) }.joinToString("")
        val amount = payableAmount()
        val booking = Booking(
            pnr = pnr,
            train = train,
            travelClass = cls,
            date = journeyDate,
            quota = quotaCode(),
            passengers = passengers.toList(),
            contact = mobile.ifBlank { userMobile },
            status = "PAID",
            amount = amount,
            fromName = stationName(fromCode),
            toName = stationName(toCode),
            paymentId = paymentId,
            orderId = orderId
        )
        lastBooking = booking
        viewModelScope.launch { bookingRepo.save(booking) }
        return booking
    }

    fun cancel(pnr: String) {
        viewModelScope.launch { bookingRepo.cancel(pnr) }
        if (lastBooking?.pnr == pnr) lastBooking = lastBooking?.copy(status = "CANCELLED")
    }

    fun toggleSeat(id: String, label: String) {
        if (id in selectedSeatIds) {
            selectedSeatIds.remove(id)
        } else if (selectedSeatIds.size < passengers.size) {
            selectedSeatIds.add(id)
        }
        selectedSeatLabels = selectedSeatIds.joinToString(", ")
    }

    fun clearSeats() {
        selectedSeatIds.clear()
        selectedSeatLabels = ""
    }

    fun addPassenger() {
        if (passengers.size < 6) passengers.add(Passenger())
    }

    fun removePassenger(index: Int) {
        if (passengers.size > 1 && index in passengers.indices) passengers.removeAt(index)
    }

    fun updatePassenger(index: Int, p: Passenger) {
        if (index in passengers.indices) passengers[index] = p
    }

    companion object {
        fun defaultDate(): String {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, 1)
            return SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(cal.time)
        }

        fun upcomingDates(): List<String> {
            val fmt = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
            val cal = Calendar.getInstance()
            return (0 until 14).map {
                if (it > 0) cal.add(Calendar.DAY_OF_YEAR, 1)
                fmt.format(cal.time)
            }
        }

        fun toApiDate(display: String): String {
            return try {
                val inFmt = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                val outFmt = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                outFmt.format(inFmt.parse(display)!!)
            } catch (_: Exception) {
                display
            }
        }
    }
}
