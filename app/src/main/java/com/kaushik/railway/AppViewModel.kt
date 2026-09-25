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
import com.kaushik.railway.data.CognitoAuth
import com.kaushik.railway.data.CognitoCall
import com.kaushik.railway.data.IrctcRules
import com.kaushik.railway.data.LastJourney
import com.kaushik.railway.data.MockData
import com.kaushik.railway.data.Passenger
import com.kaushik.railway.data.PaymentApi
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
import com.kaushik.railway.nav.AccountGate
import com.kaushik.railway.util.UiState
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
    var gateReady by mutableStateOf(false)
    var appUnlocked by mutableStateOf(false)
    var cognitoBusy by mutableStateOf(false)
    var useCognitoOtp by mutableStateOf(false)
    var cognitoUsername by mutableStateOf("")
    var cognitoDelivery by mutableStateOf("")
    private var sessionSeen = false
    private var cognitoPassword = ""
    private var pendingPassword = ""

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
                sessionSeen = true
                if (profilesReady) gateReady = true
            }
        }
        viewModelScope.launch {
            accountStore.profiles.collectLatest {
                profilesReady = true
                refreshAccount()
                if (sessionSeen) gateReady = true
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

    fun verifyEmail(code: String, onDone: (String?) -> Unit) {
        authLoading = true
        authError = null
        viewModelScope.launch {
            if (cognitoUsername.isNotBlank()) {
                val confirmed = CognitoAuth.confirm(cognitoUsername, code)
                if (confirmed is CognitoCall.Err || confirmed is CognitoCall.Offline) {
                    val message = (confirmed as? CognitoCall.Err)?.message
                        ?: (confirmed as CognitoCall.Offline).message
                    authLoading = false
                    authError = message
                    onDone(message)
                    return@launch
                }
                val pass = pendingPassword
                if (pass.isNotBlank()) {
                    val signed = CognitoAuth.signIn(cognitoUsername, pass)
                    pendingPassword = ""
                    if (signed is CognitoCall.Ok) {
                        adoptCognitoUser(signed.username, pass, signed.email, signed.name, signed.phone)
                        appUnlocked = true
                        needsVerification = false
                        authLoading = false
                        onDone(null)
                        return@launch
                    }
                    val message = (signed as? CognitoCall.Err)?.message
                        ?: (signed as? CognitoCall.Offline)?.message
                        ?: "Confirmed. Sign in with your user ID."
                    authLoading = false
                    authError = message
                    onDone(message)
                    return@launch
                }
                needsVerification = false
                authLoading = false
                onDone(null)
                return@launch
            }
            val result = authRepo.verifyEmail(pendingVerifyEmail, code)
            authLoading = false
            if (result.success) {
                needsVerification = false
                appUnlocked = true
                onDone(null)
            } else {
                authError = result.error ?: "Verification failed"
                onDone(authError)
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

    fun resendCode(onDone: (String?) -> Unit = {}) {
        viewModelScope.launch {
            if (cognitoUsername.isNotBlank()) {
                val call = CognitoAuth.resend(cognitoUsername)
                if (call is CognitoCall.Ok) {
                    otpEmailed = true
                    otpDisplayCode = null
                    cognitoDelivery = call.delivery
                    onDone(null)
                } else {
                    onDone((call as? CognitoCall.Err)?.message ?: (call as? CognitoCall.Offline)?.message)
                }
                return@launch
            }
            val result = authRepo.resendCode(pendingVerifyEmail)
            if (result.success) {
                otpEmailed = result.displayCode == null
                otpDisplayCode = result.displayCode
                onDone(null)
            } else {
                onDone(result.error)
            }
        }
    }

    fun logout(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            CognitoAuth.signOut()
            PaymentApi.bearerToken = null
            pendingPassword = ""
            cognitoPassword = ""
            cognitoUsername = ""
            useCognitoOtp = false
            authRepo.logout()
            appUnlocked = false
            onDone()
        }
    }

    fun gateDestination(): String = AccountGate.destination(
        loggedIn = loggedIn,
        profileComplete = profileComplete,
        mpinSet = mpinSet,
        mpinDeferred = mpinDeferred,
        needsUnlock = loggedIn && biometricOn && !appUnlocked
    )

    fun verifyMpin(pin: String): Boolean {
        val profile = currentProfile ?: return false
        return IrctcRules.secretHash(profile.userId, "mpin:$pin") == profile.mpinHash
    }

    fun markUnlocked() {
        appUnlocked = true
    }

    suspend fun refreshPaymentSession() {
        PaymentApi.bearerToken = CognitoAuth.idToken()
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
        useCognitoOtp = false
        cognitoUsername = ""
        cognitoDelivery = ""
        cognitoBusy = false
        cognitoPassword = ""
    }

    fun beginCognitoSignUp(onDone: (String?) -> Unit) {
        if (useCognitoOtp && cognitoUsername.isNotBlank()) {
            onDone(null)
            return
        }
        cognitoBusy = true
        viewModelScope.launch {
            val phone = IrctcRules.toIndianE164(regMobile)
            if (phone == null) {
                cognitoBusy = false
                onDone("Enter a valid 10-digit mobile number")
                return@launch
            }
            var call = CognitoAuth.signUp(
                regUserId.trim(),
                regPassword,
                regFullName.trim(),
                regEmail.trim(),
                phone
            )
            if (call is CognitoCall.Err && call.code == "EXISTS") {
                val username = regUserId.trim()
                val resent = CognitoAuth.resend(username)
                call = if (resent is CognitoCall.Ok) {
                    CognitoCall.NeedsConfirm(username, resent.delivery, regEmail.trim())
                } else {
                    resent
                }
            }
            cognitoBusy = false
            when (call) {
                is CognitoCall.Ok, is CognitoCall.NeedsConfirm -> {
                    useCognitoOtp = true
                    cognitoUsername = when (call) {
                        is CognitoCall.Ok -> call.username
                        is CognitoCall.NeedsConfirm -> call.username
                        else -> regUserId.trim()
                    }
                    cognitoDelivery = when (call) {
                        is CognitoCall.Ok -> call.delivery
                        is CognitoCall.NeedsConfirm -> call.delivery
                        else -> ""
                    }
                    cognitoPassword = regPassword
                    onDone(null)
                }
                is CognitoCall.Offline -> {
                    useCognitoOtp = false
                    cognitoPassword = ""
                    if (regEmailOtp.isBlank() || regMobileOtp.isBlank()) issueOtps()
                    cognitoDelivery = "Cognito is unreachable. Demo codes are on this screen."
                    onDone(null)
                }
                is CognitoCall.Err -> onDone(call.message)
            }
        }
    }

    fun resendRegistrationCode(onDone: (String?) -> Unit) {
        viewModelScope.launch {
            if (useCognitoOtp) {
                val call = CognitoAuth.resend(cognitoUsername.ifBlank { regUserId.trim() })
                if (call is CognitoCall.Ok) {
                    cognitoDelivery = call.delivery
                    onDone(null)
                } else if (call is CognitoCall.Offline) {
                    useCognitoOtp = false
                    issueOtps()
                    onDone(null)
                } else {
                    onDone((call as? CognitoCall.Err)?.message ?: "Could not resend the code")
                }
            } else {
                issueOtps()
                onDone(null)
            }
        }
    }

    fun submitRegistrationCodes(emailCode: String, mobileCode: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            if (useCognitoOtp) {
                val confirmed = CognitoAuth.confirm(cognitoUsername.ifBlank { regUserId.trim() }, emailCode)
                when (confirmed) {
                    is CognitoCall.Err -> {
                        onDone(confirmed.message)
                        return@launch
                    }
                    is CognitoCall.Offline -> {
                        onDone(confirmed.message)
                        return@launch
                    }
                    is CognitoCall.NeedsConfirm -> {
                        onDone("Enter the latest code sent to your email.")
                        return@launch
                    }
                    is CognitoCall.Ok -> Unit
                }
            } else {
                val problem = when {
                    !IrctcRules.otpMatches(regEmailOtp, emailCode) -> "Email OTP does not match"
                    !IrctcRules.otpMatches(regMobileOtp, mobileCode) -> "Mobile OTP does not match"
                    else -> null
                }
                if (problem != null) {
                    onDone(problem)
                    return@launch
                }
            }
            try {
                val password = cognitoPassword.ifBlank { regPassword }
                val profile = accountStore.create(
                    userId = regUserId,
                    fullName = regFullName,
                    email = regEmail,
                    mobile = regMobile,
                    password = password,
                    language = regLanguage
                )
                loginPrefill = profile.userId
                regPassword = ""
                cognitoPassword = ""
                regEmailOtp = ""
                regMobileOtp = ""
                onDone(null)
            } catch (e: Exception) {
                onDone(e.message ?: "Could not create the account")
            }
        }
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
            val cognitoDeferred = async {
                withTimeoutOrNull(12_000) { CognitoAuth.signIn(idOrEmail.trim(), password) }
            }
            var localProfile: RailProfile? = null
            var localError: String? = null
            try {
                localProfile = accountStore.authenticate(idOrEmail, password)
            } catch (e: Exception) {
                localError = e.message ?: "Login failed"
            }
            when (val cognito = cognitoDeferred.await()) {
                is CognitoCall.Ok -> {
                    adoptCognitoUser(cognito.username.ifBlank { idOrEmail.trim() }, password, cognito.email, cognito.name, cognito.phone)
                    appUnlocked = true
                    authLoading = false
                    onSuccess()
                    return@launch
                }
                is CognitoCall.NeedsConfirm -> {
                    cognitoUsername = cognito.username.ifBlank { idOrEmail.trim() }
                    pendingPassword = password
                    pendingVerifyEmail = idOrEmail.trim()
                    useCognitoOtp = true
                    otpEmailed = true
                    otpDisplayCode = null
                    needsVerification = true
                    authLoading = false
                    onNeedVerify(pendingVerifyEmail)
                    return@launch
                }
                is CognitoCall.Err -> if (cognito.code == "INVALID") {
                    authLoading = false
                    authError = cognito.message
                    onError(cognito.message)
                    return@launch
                }
                else -> Unit
            }
            val cognito = cognitoDeferred.await()
            if (localProfile != null) {
                sessionStore.saveSession(localProfile.fullName, localProfile.email, localProfile.mobile, localProfile.userId)
                applyProfile(localProfile)
                loggedIn = true
                appUnlocked = true
                authLoading = false
                onSuccess()
                return@launch
            }
            val result = authRepo.login(idOrEmail, password)
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
                appUnlocked = true
                authLoading = false
                onSuccess()
                return@launch
            }
            if (result.needsVerification) {
                pendingVerifyEmail = result.email.ifBlank { idOrEmail }
                needsVerification = true
                otpEmailed = result.displayCode == null
                otpDisplayCode = result.displayCode
                authError = result.error
                authLoading = false
                onNeedVerify(pendingVerifyEmail)
                return@launch
            }
            val cognitoMessage = (cognito as? CognitoCall.Err)?.message
            val offline = cognito == null || cognito is CognitoCall.Offline ||
                (cognito is CognitoCall.Err && cognito.code == "NOT_FOUND")
            val message = when {
                localError != null && offline -> localError
                cognitoMessage != null -> cognitoMessage
                else -> result.error ?: "No account for that user ID. Register first."
            }
            authLoading = false
            authError = message
            onError(message)
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

    private suspend fun adoptCognitoUser(
        username: String,
        password: String,
        email: String,
        name: String,
        phone: String
    ) {
        val mail = email.trim().ifBlank { username.takeIf { it.contains("@") }.orEmpty() }
        val phoneDigits = phone.filter { it.isDigit() }.let { if (it.length > 10) it.takeLast(10) else it }
        val list = accountStore.profiles.first()
        val match = list.find { it.userId.equals(username, ignoreCase = true) }
            ?: list.find { mail.isNotBlank() && it.email.equals(mail, ignoreCase = true) }
        val profile = if (match != null) {
            accountStore.update(match.userId) {
                it.copy(
                    fullName = name.ifBlank { it.fullName },
                    email = mail.ifBlank { it.email },
                    mobile = phoneDigits.ifBlank { it.mobile },
                    passwordHash = IrctcRules.secretHash(it.userId, password)
                )
            }
        } else {
            val id = if (IrctcRules.validateUserId(username) == null && !accountStore.isUserIdTaken(username)) {
                username.trim()
            } else {
                accountStore.unusedUserId(IrctcRules.userIdFromEmail(mail.ifBlank { username }))
            }
            accountStore.create(
                userId = id,
                fullName = name.ifBlank { id },
                email = mail.ifBlank { "$id@users.railx.local" },
                mobile = phoneDigits,
                password = password,
                language = "English",
                profileComplete = false
            )
        }
        sessionStore.saveSession(profile.fullName, profile.email, profile.mobile, profile.userId)
        applyProfile(profile)
        loggedIn = true
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
        appUnlocked = false
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
