package com.kaushik.railway.data

import com.kaushik.railway.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class AuthUser(val name: String, val email: String)
data class OtpResult(val emailed: Boolean, val code: String = "")

class UnverifiedException(message: String) : Exception(message)

/**
 * Auth client.
 * - Tries backend when available
 * - Falls back to local demo store when backend is offline (no new backend required)
 * Keys/secrets are never hardcoded; local demo only stores in-memory users for the session.
 */
object AuthApi {
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private val baseUrl get() = BuildConfig.BACKEND_BASE_URL.trimEnd('/')

    // In-memory demo users when backend is unreachable
    private data class LocalUser(
        val name: String,
        val email: String,
        val password: String,
        var verified: Boolean = false,
        var otp: String = ""
    )
    private val localUsers = ConcurrentHashMap<String, LocalUser>()

    fun register(name: String, email: String, password: String): OtpResult {
        return tryRemote {
            val root = post("/register", JSONObject()
                .put("name", name).put("email", email).put("password", password))
            if (!root.optBoolean("success", false)) {
                throw IllegalStateException(root.optString("error").ifBlank { "Registration failed" })
            }
            OtpResult(emailed = true)
        } ?: run {
            val code = "%06d".format((100000..999999).random())
            localUsers[email.lowercase()] = LocalUser(name, email.lowercase(), password, false, code)
            OtpResult(emailed = false, code = code)
        }
    }

    fun login(email: String, password: String): AuthUser {
        return tryRemote {
            val root = post("/login", JSONObject().put("email", email).put("password", password))
            if (root.optBoolean("needsVerification", false)) {
                throw UnverifiedException(root.optString("error").ifBlank { "Email not verified" })
            }
            if (!root.optBoolean("success", false)) {
                throw IllegalStateException(root.optString("error").ifBlank { "Login failed" })
            }
            AuthUser(name = root.optString("name"), email = root.optString("email"))
        } ?: run {
            val u = localUsers[email.lowercase()]
                ?: throw IllegalStateException("No account. Register first (local demo mode).")
            if (u.password != password) throw IllegalStateException("Invalid email or password")
            if (!u.verified) {
                u.otp = "%06d".format((100000..999999).random())
                throw UnverifiedException("Email not verified. Use code: ${u.otp}")
            }
            AuthUser(name = u.name, email = u.email)
        }
    }

    fun verifyEmail(email: String, code: String): AuthUser {
        return tryRemote {
            val root = post("/verify-email", JSONObject().put("email", email).put("code", code))
            if (!root.optBoolean("success", false)) {
                throw IllegalStateException(root.optString("error").ifBlank { "Verification failed" })
            }
            AuthUser(name = root.optString("name"), email = root.optString("email"))
        } ?: run {
            val u = localUsers[email.lowercase()]
                ?: throw IllegalStateException("No account for this email")
            if (u.otp != code) throw IllegalStateException("Invalid verification code")
            u.verified = true
            u.otp = ""
            AuthUser(name = u.name, email = u.email)
        }
    }

    fun resendCode(email: String): OtpResult {
        return tryRemote {
            val root = post("/resend-code", JSONObject().put("email", email))
            if (!root.optBoolean("success", false)) {
                throw IllegalStateException(root.optString("error").ifBlank { "Could not resend" })
            }
            OtpResult(emailed = true)
        } ?: run {
            val u = localUsers[email.lowercase()]
                ?: throw IllegalStateException("No account for this email")
            u.otp = "%06d".format((100000..999999).random())
            OtpResult(emailed = false, code = u.otp)
        }
    }

    private fun <T> tryRemote(block: () -> T): T? {
        return try {
            block()
        } catch (e: Exception) {
            // Network / backend down → local demo
            null
        }
    }

    private fun post(path: String, body: JSONObject): JSONObject {
        val req = Request.Builder()
            .url("$baseUrl$path")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody(jsonType))
            .build()
        http.newCall(req).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            return JSONObject(raw.ifBlank { "{}" })
        }
    }
}
