package com.kaushik.railway

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaushik.railway.data.AccountStore
import com.kaushik.railway.data.AvailabilityResult
import com.kaushik.railway.data.Booking
import com.kaushik.railway.data.IrctcRules
import com.kaushik.railway.data.LastJourney
import com.kaushik.railway.data.MockData
import com.kaushik.railway.data.Passenger
import com.kaushik.railway.data.PnrResult
import com.kaushik.railway.data.ProfileDraft
import com.kaushik.railway.data.RailProfile
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

class AppViewModel : ViewModel() {

    private val sessionStore = SessionStore(RailApp.instance)
    private val accountStore = AccountStore(RailApp.instance)
    private val bookingRepo = BookingRepository(AppDatabase.getInstance(RailApp.instance))
    private val trainRepo = TrainRepository()
    private val authRepo = AuthRepository(sessionStore)

    // Auth / session
    var loggedIn by mutableStateOf(false)
    var userName by mutableStateOf("")
    var userEmail by mutableStateOf("")
    var userMobile by mutableStateOf("")
    var userId by mutableStateOf("")
    var profileComplete by mutableStateOf(false)
    var mpinSet by mutableStateOf(false)
    var mpinDeferred by mutableStateOf(false)
    var biometricOn by mutableStateOf(false)
    var aadhaarLinked by mutableStateOf(false)
    var aadhaarLast4 by mutableStateOf("")
    var aadhaarKind by mutableStateOf("")
    var currentProfile by mutableStateOf<RailProfile?>(null)
    var loginPrefill by mutableStateOf("")
    var regVisuallyImpaired by mutableStateOf(false)
    var regUserId by mutableStateOf("")
    var regFullName by mutableStateOf("")
    var regPassword by mutableStateOf("")
    var regMobile by mutableStateOf("")
    var regEmail by mutableStateOf("")
    var regLanguage by mutableStateOf("English")
    var regCaptcha by mutableStateOf("")
    var regEmailOtp by mutableStateOf("")
    var regMobileOtp by mutableStateOf("")
    var regStep by mutableStateOf(0)
    var regIdMessage by mutableStateOf("")
    var regIdChecked by mutableStateOf("")
    private var profilesReady by mutableStateOf(false)
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
    var returnDate by mutableStateOf<String?>(null)
    var lastJourney by mutableStateOf<LastJourney?>(null)
    var savedPassengers = mutableStateListOf<Passenger>()
    var isOfflineHint by mutableStateOf(false)

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
        viewModelScope.launch {
            authRepo.sessionFlow.collectLatest { session ->
                loggedIn = session.loggedIn
                userName = session.name
                userEmail = session.email
                userMobile = session.mobile
                userId = session.userId
                if (session.mobile.isNotBlank()) mobile = session.mobile
                if (session.email.isNotBlank()) email = session.email
                if (!session.loggedIn) clearAccountFlags() else refreshAccount()
            }
        }
        viewModelScope.launch {
            accountStore.profiles.collectLatest {
                profilesReady = true
                refreshAccount()
            }
        }
        viewModelScope.launch {
            sessionStore.lastJourneyFlow.collectLatest { j -> lastJourney = j }
        }
        viewModelScope.launch {
            sessionStore.savedPassengersFlow.collectLatest { list ->
                savedPassengers.clear()
                savedPassengers.addAll(list)
            }
        }
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
                otpEmailed = result.displayCode == null
                otpDisplayCode = result.displayCode
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
                pendingVerifyEmail = result.email.ifBlank { email }
                needsVerification = true
                otpEmailed = result.displayCode == null
                otpDisplayCode = result.displayCode
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
            if (userId.isNotBlank()) {
                val updated = accountStore.update(userId) { it.copy(fullName = name, mobile = mobile.filter { ch -> ch.isDigit() }) }
                applyProfile(updated)
            }
        }
    }

    fun needsTatkalAadhaar(): Boolean = IrctcRules.isTatkalQuota(selectedQuota) && !aadhaarLinked

    fun startRegistration(visuallyImpaired: Boolean) {
        regVisuallyImpaired = visuallyImpaired
        regUserId = ""
        regFullName = ""
        regPassword = ""
        regMobile = ""
        regEmail = ""
        regLanguage = "English"
        regCaptcha = IrctcRules.newCaptcha()
        regEmailOtp = ""
        regMobileOtp = ""
        regStep = 0
        regIdMessage = ""
        regIdChecked = ""
    }

    fun checkUserId() {
        val err = IrctcRules.validateUserId(regUserId)
        if (err != null) {
            regIdMessage = err
            regIdChecked = ""
            return
        }
        viewModelScope.launch {
            val taken = accountStore.isUserIdTaken(regUserId)
            regIdChecked = if (taken) "" else regUserId.trim()
            regIdMessage = if (taken) "User ID is already taken" else "User ID is available"
        }
    }

    fun refreshCaptcha() {
        regCaptcha = IrctcRules.newCaptcha()
    }

    fun issueOtps() {
        regEmailOtp = IrctcRules.newOtp()
        regMobileOtp = IrctcRules.newOtp()
    }

    fun validateNewContact(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val message = when {
                accountStore.isEmailTaken(regEmail) -> "Email is already registered"
                accountStore.isMobileTaken(regMobile) -> "Mobile is already registered"
                else -> null
            }
            onResult(message)
        }
    }

    fun finishRegistration(onDone: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val profile = accountStore.create(
                    userId = regUserId,
                    fullName = regFullName,
                    email = regEmail,
                    mobile = regMobile,
                    password = regPassword,
                    language = regLanguage
                )
                loginPrefill = profile.userId
                regPassword = ""
                regEmailOtp = ""
                regMobileOtp = ""
                onDone(null)
            } catch (e: Exception) {
                onDone(e.message ?: "Could not create the account")
            }
        }
    }

    fun signIn(
        idOrEmail: String,
        password: String,
        onSuccess: () -> Unit,
        onNeedVerify: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        authLoading = true
        authError = null
        viewModelScope.launch {
            val rejected = try {
                val profile = accountStore.authenticate(idOrEmail, password)
                if (profile != null) {
                    sessionStore.saveSession(profile.fullName, profile.email, profile.mobile, profile.userId)
                    applyProfile(profile)
                    loggedIn = true
                    authLoading = false
                    onSuccess()
                    return@launch
                }
                null
            } catch (e: Exception) {
                e.message ?: "Login failed"
            }
            if (rejected != null) {
                authLoading = false
                authError = rejected
                onError(rejected)
                return@launch
            }
            val result = authRepo.login(idOrEmail, password)
            authLoading = false
            if (result.success) {
                try {
                    bridgeLegacyAccount(result.name, result.email.ifBlank { idOrEmail }, password)
                } catch (_: Exception) {
                    profileComplete = true
                    userName = result.name
                    userEmail = result.email.ifBlank { idOrEmail }
                    email = userEmail
                    loggedIn = true
                }
                onSuccess()
            } else if (result.needsVerification) {
                pendingVerifyEmail = result.email.ifBlank { idOrEmail }
                needsVerification = true
                otpEmailed = result.displayCode == null
                otpDisplayCode = result.displayCode
                authError = result.error
                onNeedVerify(pendingVerifyEmail)
            } else {
                val message = result.error ?: "No account for that user ID. Register first."
                authError = message
                onError(message)
            }
        }
    }

    fun lookupUserId(emailOrMobile: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            onResult(accountStore.findUserId(emailOrMobile))
        }
    }

    fun completeProfile(draft: ProfileDraft, onDone: (String?) -> Unit) {
        val problem = IrctcRules.validateProfile(draft)
        if (problem != null) {
            onDone(problem)
            return
        }
        if (userId.isBlank()) {
            onDone("Sign in with your user ID before saving the profile")
            return
        }
        viewModelScope.launch {
            try {
                val updated = accountStore.update(userId) {
                    it.copy(
                        gender = draft.gender,
                        dob = draft.dob.trim(),
                        occupation = draft.occupation,
                        maritalStatus = draft.maritalStatus,
                        nationality = draft.nationality.trim(),
                        addressLine = draft.addressLine.trim(),
                        city = draft.city.trim(),
                        state = draft.state,
                        country = draft.country.trim(),
                        pinCode = draft.pinCode,
                        profileComplete = true
                    )
                }
                sessionStore.saveSession(updated.fullName, updated.email, updated.mobile, updated.userId)
                applyProfile(updated)
                onDone(null)
            } catch (e: Exception) {
                onDone(e.message ?: "Could not save the profile")
            }
        }
    }

    fun saveMpin(pin: String, confirm: String, biometric: Boolean, onDone: (String?) -> Unit) {
        val problem = IrctcRules.validateMpin(pin, confirm)
        if (problem != null) {
            onDone(problem)
            return
        }
        if (userId.isBlank()) {
            onDone("Sign in again to save the PIN")
            return
        }
        viewModelScope.launch {
            try {
                val updated = accountStore.update(userId) {
                    it.copy(
                        mpinHash = IrctcRules.secretHash(it.userId, "mpin:$pin"),
                        biometricEnabled = biometric,
                        mpinDeferred = false
                    )
                }
                applyProfile(updated)
                onDone(null)
            } catch (e: Exception) {
                onDone(e.message ?: "Could not save the PIN")
            }
        }
    }

    fun deferMpin(onDone: () -> Unit) {
        viewModelScope.launch {
            if (userId.isNotBlank()) {
                val updated = accountStore.update(userId) { it.copy(mpinDeferred = true) }
                applyProfile(updated)
            } else {
                mpinDeferred = true
            }
            onDone()
        }
    }

    fun saveAadhaarLink(kind: String, last4: String, onDone: (String?) -> Unit) {
        if (userId.isBlank()) {
            onDone("Register an account before linking Aadhaar")
            return
        }
        if (last4.length != 4 || last4.any { !it.isDigit() }) {
            onDone("Could not save the link")
            return
        }
        viewModelScope.launch {
            try {
                val updated = accountStore.update(userId) {
                    it.copy(aadhaarLinked = true, aadhaarLast4 = last4, aadhaarKind = kind)
                }
                applyProfile(updated)
                onDone(null)
            } catch (e: Exception) {
                onDone(e.message ?: "Could not save the link")
            }
        }
    }

    private suspend fun bridgeLegacyAccount(name: String, email: String, password: String) {
        val mail = email.trim()
        if (!mail.contains("@")) {
            profileComplete = true
            userName = name
            userEmail = mail
            loggedIn = true
            return
        }
        val seed = IrctcRules.userIdFromEmail(mail)
        val id = accountStore.unusedUserId(seed)
        val profile = accountStore.create(
            userId = id,
            fullName = name.ifBlank { id },
            email = mail,
            mobile = userMobile.filter { it.isDigit() },
            password = password,
            language = "English",
            profileComplete = true
        )
        sessionStore.saveSession(profile.fullName, profile.email, profile.mobile, profile.userId)
        applyProfile(profile)
        loggedIn = true
    }

    private suspend fun refreshAccount() {
        if (!profilesReady || !loggedIn) return
        val list = accountStore.profiles.first()
        val match = list.find { userId.isNotBlank() && it.userId.equals(userId, ignoreCase = true) }
            ?: list.find { userEmail.isNotBlank() && it.email.equals(userEmail, ignoreCase = true) }
        if (match != null) applyProfile(match) else if (userId.isBlank()) profileComplete = true
    }

    private fun applyProfile(profile: RailProfile) {
        currentProfile = profile
        userId = profile.userId
        userName = profile.fullName
        userEmail = profile.email
        userMobile = profile.mobile
        email = profile.email
        if (profile.mobile.isNotBlank()) mobile = profile.mobile
        profileComplete = profile.profileComplete
        mpinSet = profile.mpinSet
        mpinDeferred = profile.mpinDeferred
        biometricOn = profile.biometricEnabled
        aadhaarLinked = profile.aadhaarLinked
        aadhaarLast4 = profile.aadhaarLast4
        aadhaarKind = profile.aadhaarKind
    }

    private fun clearAccountFlags() {
        currentProfile = null
        profileComplete = false
        mpinSet = false
        mpinDeferred = false
        biometricOn = false
        aadhaarLinked = false
        aadhaarLast4 = ""
        aadhaarKind = ""
    }

    // ── Search / Trains ───────────────────────────────────
    fun stationName(code: String) =
        MockData.stations.find { it.code == code }?.let { "${it.name} (${it.code})" }
            ?: stationSuggestions.find { it.code == code }?.let { "${it.name} (${it.code})" }
            ?: code

    fun quotaCode() = selectedQuota.take(2)

    fun searchTrainsLive() {
        searchLoading = true
        searchError = null
        isOfflineHint = false
        trains.clear()
        viewModelScope.launch {
            val result = trainRepo.searchTrains(fromCode, toCode, toApiDate(journeyDate))
            searchLoading = false
            result.onSuccess { list ->
                if (list.isNotEmpty()) {
                    trains.addAll(list)
                } else {
                    trains.addAll(MockData.trains(fromCode, toCode))
                    isOfflineHint = true
                }
            }.onFailure {
                trains.addAll(MockData.trains(fromCode, toCode))
                isOfflineHint = true
            }
        }
    }

    fun loadAvailability(train: Train) {
        selectedTrain = train
        availLoading = true
        availError = null
        isOfflineHint = false
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
                if (list.isNotEmpty()) {
                    classRows.addAll(list)
                } else {
                    classRows.addAll(
                        train.classes.ifEmpty {
                            MockData.trains(fromCode, toCode).firstOrNull()?.classes.orEmpty()
                        }
                    )
                    isOfflineHint = true
                }
            }.onFailure {
                classRows.addAll(
                    train.classes.ifEmpty {
                        MockData.trains(fromCode, toCode).firstOrNull()?.classes.orEmpty()
                    }
                )
                isOfflineHint = true
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
        viewModelScope.launch {
            bookingRepo.save(booking)
            sessionStore.saveLastJourney(
                LastJourney(
                    fromCode = fromCode,
                    toCode = toCode,
                    classCode = cls.code,
                    quota = selectedQuota,
                    trainNumber = train.number,
                    trainName = train.name
                )
            )
            sessionStore.savePassengers(passengers.toList())
        }
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

    /** One-tap rebook: restore last route into search form */
    fun applyLastJourney() {
        val j = lastJourney ?: return
        if (j.fromCode.isNotBlank()) fromCode = j.fromCode
        if (j.toCode.isNotBlank()) toCode = j.toCode
        if (j.quota.isNotBlank()) selectedQuota = j.quota
        if (j.classCode.isNotBlank()) selectedClass = j.classCode
        journeyDate = defaultDate()
    }

    fun loadSavedPassengers() {
        if (savedPassengers.isEmpty()) return
        passengers.clear()
        passengers.addAll(savedPassengers.map { it.copy() })
        if (passengers.isEmpty()) passengers.add(Passenger())
    }

    fun swapStations() {
        val tmp = fromCode
        fromCode = toCode
        toCode = tmp
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
