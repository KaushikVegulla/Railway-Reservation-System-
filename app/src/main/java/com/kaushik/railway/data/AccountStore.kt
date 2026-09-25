package com.kaushik.railway.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.accountDataStore by preferencesDataStore(name = "railone_accounts")

data class RailProfile(
    val userId: String,
    val fullName: String,
    val email: String,
    val mobile: String,
    val passwordHash: String,
    val language: String = "English",
    val gender: String = "",
    val dob: String = "",
    val occupation: String = "",
    val maritalStatus: String = "",
    val nationality: String = "India",
    val addressLine: String = "",
    val city: String = "",
    val state: String = "",
    val country: String = "India",
    val pinCode: String = "",
    val profileComplete: Boolean = false,
    val mpinHash: String = "",
    val biometricEnabled: Boolean = false,
    val mpinDeferred: Boolean = false,
    val aadhaarLinked: Boolean = false,
    val aadhaarLast4: String = "",
    val aadhaarKind: String = ""
) {
    val mpinSet: Boolean get() = mpinHash.isNotBlank()
}

/**
 * On-device accounts for the demo. Passwords are stored as hashes.
 * Aadhaar / VID values are never written — only the last 4 digits.
 */
class AccountStore(private val context: Context) {
    private val keyProfiles = stringPreferencesKey("profiles_json")

    val profiles: Flow<List<RailProfile>> = context.accountDataStore.data.map { prefs ->
        parse(prefs[keyProfiles].orEmpty())
    }

    suspend fun isUserIdTaken(id: String): Boolean =
        profiles.first().any { it.userId.equals(id.trim(), ignoreCase = true) }

    suspend fun isEmailTaken(email: String): Boolean =
        profiles.first().any { it.email.equals(email.trim(), ignoreCase = true) }

    suspend fun isMobileTaken(mobile: String): Boolean {
        val digits = mobile.filter { it.isDigit() }
        if (digits.length != 10) return false
        return profiles.first().any { it.mobile == digits }
    }

    suspend fun findUserId(emailOrMobile: String): String? {
        val key = emailOrMobile.trim()
        val digits = key.filter { it.isDigit() }
        return profiles.first().find { profile ->
            profile.email.equals(key, ignoreCase = true) || (digits.length == 10 && profile.mobile == digits)
        }?.userId
    }

    /**
     * @return the profile, null when no account matches, or throws when the password is wrong.
     */
    suspend fun authenticate(idOrEmail: String, password: String): RailProfile? {
        val key = idOrEmail.trim()
        val found = profiles.first().find {
            it.userId.equals(key, ignoreCase = true) || it.email.equals(key, ignoreCase = true)
        } ?: return null
        val hash = IrctcRules.secretHash(found.userId, password)
        if (hash != found.passwordHash) throw IllegalStateException("Invalid user ID or password")
        return found
    }

    suspend fun create(
        userId: String,
        fullName: String,
        email: String,
        mobile: String,
        password: String,
        language: String,
        profileComplete: Boolean = false
    ): RailProfile {
        val id = userId.trim()
        if (isUserIdTaken(id)) throw IllegalStateException("User ID is already taken")
        if (isEmailTaken(email)) throw IllegalStateException("Email is already registered")
        val digits = mobile.filter { it.isDigit() }
        if (digits.isNotBlank() && isMobileTaken(digits)) throw IllegalStateException("Mobile is already registered")
        val profile = RailProfile(
            userId = id,
            fullName = fullName.trim(),
            email = email.trim(),
            mobile = digits,
            passwordHash = IrctcRules.secretHash(id, password),
            language = language,
            profileComplete = profileComplete
        )
        upsert(profile)
        return profile
    }

    suspend fun update(userId: String, block: (RailProfile) -> RailProfile): RailProfile {
        val current = profiles.first().find { it.userId.equals(userId, ignoreCase = true) }
            ?: throw IllegalStateException("Account not found")
        val next = block(current)
        upsert(next)
        return next
    }

    suspend fun unusedUserId(seed: String): String {
        var candidate = seed
        var n = 2
        while (isUserIdTaken(candidate)) {
            val suffix = n.toString()
            candidate = (seed.take(35 - suffix.length) + suffix).take(35)
            n++
        }
        return candidate
    }

    private suspend fun upsert(profile: RailProfile) {
        val next = profiles.first().filterNot { it.userId.equals(profile.userId, ignoreCase = true) } + profile
        context.accountDataStore.edit { prefs ->
            val arr = JSONArray()
            next.forEach { arr.put(it.toJson()) }
            prefs[keyProfiles] = arr.toString()
        }
    }

    private fun parse(raw: String): List<RailProfile> {
        if (raw.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { index -> arr.getJSONObject(index).toProfile() }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

private fun RailProfile.toJson(): JSONObject = JSONObject()
    .put("userId", userId)
    .put("fullName", fullName)
    .put("email", email)
    .put("mobile", mobile)
    .put("passwordHash", passwordHash)
    .put("language", language)
    .put("gender", gender)
    .put("dob", dob)
    .put("occupation", occupation)
    .put("maritalStatus", maritalStatus)
    .put("nationality", nationality)
    .put("addressLine", addressLine)
    .put("city", city)
    .put("state", state)
    .put("country", country)
    .put("pinCode", pinCode)
    .put("profileComplete", profileComplete)
    .put("mpinHash", mpinHash)
    .put("biometricEnabled", biometricEnabled)
    .put("mpinDeferred", mpinDeferred)
    .put("aadhaarLinked", aadhaarLinked)
    .put("aadhaarLast4", aadhaarLast4)
    .put("aadhaarKind", aadhaarKind)

private fun JSONObject.toProfile(): RailProfile = RailProfile(
    userId = optString("userId"),
    fullName = optString("fullName"),
    email = optString("email"),
    mobile = optString("mobile"),
    passwordHash = optString("passwordHash"),
    language = optString("language", "English"),
    gender = optString("gender"),
    dob = optString("dob"),
    occupation = optString("occupation"),
    maritalStatus = optString("maritalStatus"),
    nationality = optString("nationality", "India"),
    addressLine = optString("addressLine"),
    city = optString("city"),
    state = optString("state"),
    country = optString("country", "India"),
    pinCode = optString("pinCode"),
    profileComplete = optBoolean("profileComplete"),
    mpinHash = optString("mpinHash"),
    biometricEnabled = optBoolean("biometricEnabled"),
    mpinDeferred = optBoolean("mpinDeferred"),
    aadhaarLinked = optBoolean("aadhaarLinked"),
    aadhaarLast4 = optString("aadhaarLast4"),
    aadhaarKind = optString("aadhaarKind")
)
