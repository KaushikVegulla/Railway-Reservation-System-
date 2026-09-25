package com.kaushik.railway.data

import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ProfileDraft(
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
    val acceptedTerms: Boolean = false
)

/**
 * Result of a local format check. Holds only the last 4 digits — never the full number.
 */
data class IdentityCheck(
    val ok: Boolean,
    val kind: String = "",
    val last4: String = "",
    val error: String? = null
)

/**
 * Local format checks for the academic RailOne account flow.
 * Nothing here contacts IRCTC or UIDAI.
 */
object IrctcRules {
    val languages = listOf("English", "Hindi")
    val genders = listOf("Male", "Female", "Transgender")
    val occupations = listOf(
        "Government", "Public Sector", "Private", "Business", "Professional",
        "Agriculture", "Student", "Homemaker", "Retired", "Other"
    )
    val maritalStatuses = listOf("Unmarried", "Married")
    val authTypes = listOf("AADHAAR CARD / VID", "PAN CARD")
    val passwordRules = listOf(
        "8 to 15 characters",
        "One uppercase and one lowercase letter",
        "One number",
        "One special character"
    )
    val indianStates = listOf(
        "Andaman and Nicobar Islands", "Andhra Pradesh", "Arunachal Pradesh", "Assam",
        "Bihar", "Chandigarh", "Chhattisgarh", "Dadra and Nagar Haveli and Daman and Diu",
        "Delhi", "Goa", "Gujarat", "Haryana", "Himachal Pradesh", "Jammu and Kashmir",
        "Jharkhand", "Karnataka", "Kerala", "Ladakh", "Lakshadweep", "Madhya Pradesh",
        "Maharashtra", "Manipur", "Meghalaya", "Mizoram", "Nagaland", "Odisha",
        "Puducherry", "Punjab", "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana",
        "Tripura", "Uttar Pradesh", "Uttarakhand", "West Bengal"
    )

    private val captchaAlphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun validateUserId(id: String): String? {
        val v = id.trim()
        if (v.length !in 3..35) return "User ID must be 3 to 35 characters"
        if (!v.first().isLetter()) return "User ID must start with a letter"
        if (!v.all { it.isLetterOrDigit() || it == '_' }) return "Use letters, numbers, or underscore"
        return null
    }

    fun validateFullName(name: String): String? {
        val v = name.trim()
        if (v.length < 2) return "Enter the full name as per Govt. ID"
        if (!v.all { it.isLetter() || it == ' ' || it == '.' }) return "Name can use letters, spaces, and dots"
        return null
    }

    fun validatePassword(pw: String): String? {
        if (pw.length !in 8..15) return "Password must be 8 to 15 characters"
        if (!pw.any { it.isUpperCase() }) return "Add an uppercase letter"
        if (!pw.any { it.isLowerCase() }) return "Add a lowercase letter"
        if (!pw.any { it.isDigit() }) return "Add a number"
        if (!pw.any { !it.isLetterOrDigit() }) return "Add a special character"
        return null
    }

    fun validateMobile(mobile: String): String? {
        val d = mobile.filter { it.isDigit() }
        if (d.length != 10) return "Enter a 10-digit mobile number"
        if (d.first() !in '6'..'9') return "Mobile number must start with 6, 7, 8, or 9"
        return null
    }

    fun validateEmail(email: String): String? {
        val v = email.trim()
        val ok = v.matches(Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
        return if (ok) null else "Enter a valid email address"
    }

    fun maskEmail(email: String): String {
        val parts = email.trim().split("@")
        if (parts.size != 2 || parts[0].isBlank()) return "—"
        val name = parts[0]
        val shown = if (name.length <= 2) name.take(1) + "***" else name.take(1) + "***" + name.takeLast(1)
        val domain = parts[1]
        val dot = domain.lastIndexOf('.')
        val host = if (dot > 0) domain.substring(0, dot) else domain
        val tld = if (dot > 0) domain.substring(dot) else ""
        return "$shown@${host.take(1)}***$tld"
    }

    fun maskMobile(mobile: String): String {
        val d = mobile.filter { it.isDigit() }
        if (d.length < 4) return "—"
        return "******${d.takeLast(4)}"
    }

    fun newCaptcha(): String = (1..5).map { captchaAlphabet.random() }.joinToString("")

    fun captchaMatches(expected: String, input: String): Boolean =
        expected.isNotBlank() && expected.equals(input.trim(), ignoreCase = true)

    fun newOtp(): String = "%06d".format((100000..999999).random())

    fun otpMatches(expected: String, input: String): Boolean =
        expected.length == 6 && expected == input.trim()

    fun isTatkalQuota(quota: String): Boolean {
        val q = quota.uppercase(Locale.ENGLISH)
        return q.startsWith("TQ") || q.startsWith("PT") || q.contains("TATKAL")
    }

    fun validateDob(dob: String, today: Calendar = Calendar.getInstance()): String? {
        val fmt = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH)
        fmt.isLenient = false
        val date = try {
            fmt.parse(dob.trim())
        } catch (_: Exception) {
            null
        } ?: return "Enter date of birth as DD-MM-YYYY"
        if (date.after(today.time)) return "Date of birth cannot be in the future"
        val adult = today.clone() as Calendar
        adult.add(Calendar.YEAR, -18)
        if (date.after(adult.time)) return "You must be 18 or older"
        return null
    }

    fun validateProfile(draft: ProfileDraft): String? {
        if (draft.gender !in genders) return "Select gender"
        validateDob(draft.dob)?.let { return it }
        if (draft.occupation !in occupations) return "Select occupation"
        if (draft.maritalStatus !in maritalStatuses) return "Select marital status"
        if (draft.nationality.isBlank()) return "Enter nationality"
        if (draft.addressLine.trim().length < 5) return "Enter the address"
        if (draft.city.trim().length < 2) return "Enter the city"
        if (draft.state !in indianStates) return "Select a state"
        if (draft.country.isBlank()) return "Enter the country"
        if (draft.pinCode.length != 6 || draft.pinCode.any { !it.isDigit() }) return "Enter a 6-digit PIN code"
        if (!draft.acceptedTerms) return "Accept the terms to continue"
        return null
    }

    fun validateMpin(pin: String, confirm: String): String? {
        if (pin.length != 4 || pin.any { !it.isDigit() }) return "PIN must be 4 digits"
        if (pin != confirm) return "PIN and confirm PIN do not match"
        return null
    }

    /**
     * Local shape check only. PAN does not enable Tatkal.
     * The returned value never includes the full number.
     */
    fun checkIdentity(kind: String, raw: String, consent: Boolean): IdentityCheck {
        if (!consent) return IdentityCheck(ok = false, error = "Confirm the consent checkbox to continue")
        if (kind == "PAN CARD") {
            return IdentityCheck(
                ok = false,
                error = "PAN does not enable Tatkal. Choose Aadhaar Card / VID."
            )
        }
        if (kind != "AADHAAR CARD / VID") {
            return IdentityCheck(ok = false, error = "Select an authentication type")
        }
        val digits = raw.filter { it.isDigit() }
        if (digits.length != raw.trim().length) {
            return IdentityCheck(ok = false, error = "Use digits only")
        }
        return when (digits.length) {
            12 -> checkAadhaar(digits)
            16 -> checkVid(digits)
            else -> IdentityCheck(ok = false, error = "Enter a 12-digit Aadhaar or a 16-digit VID")
        }
    }

    fun secretHash(userId: String, secret: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("${userId.lowercase(Locale.ENGLISH)}:$secret".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun toIndianE164(mobile: String): String? {
        var digits = mobile.filter { it.isDigit() }
        if (digits.length > 10 && digits.startsWith("91")) digits = digits.drop(2)
        if (digits.length != 10 || digits.first() !in '6'..'9') return null
        return "+91$digits"
    }

    fun userIdFromEmail(email: String): String {
        val raw = email.substringBefore("@").filter { it.isLetterOrDigit() }
        val base = if (raw.firstOrNull()?.isLetter() == true) raw else "u$raw"
        val trimmed = base.take(35).ifBlank { "railuser" }
        return if (trimmed.length < 3) (trimmed + "rail").take(35) else trimmed
    }

    private fun checkAadhaar(digits: String): IdentityCheck {
        if (digits.first() !in '2'..'9') {
            return IdentityCheck(ok = false, error = "Aadhaar numbers start from 2 to 9")
        }
        if (digits.all { it == digits.first() }) {
            return IdentityCheck(ok = false, error = "Enter the number from the Aadhaar card")
        }
        return IdentityCheck(ok = true, kind = "AADHAAR", last4 = digits.takeLast(4))
    }

    private fun checkVid(digits: String): IdentityCheck {
        if (digits.all { it == digits.first() }) {
            return IdentityCheck(ok = false, error = "Enter the 16-digit VID")
        }
        return IdentityCheck(ok = true, kind = "VID", last4 = digits.takeLast(4))
    }
}
