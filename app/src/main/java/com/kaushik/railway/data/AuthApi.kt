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

class UnverifiedException(
    message: String,
    val email: String = "",
    val emailed: Boolean = false,
    val displayCode: String? = null
) : Exception(message)

/**
 * Auth — keys masked via BuildConfig.
 * use.local.keys=true → pure local demo (no backend).
 * Otherwise tries backend, falls back to local in-memory store.
 */
object AuthApi {
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private val baseUrl get() = BuildConfig.BACKEND_BASE_URL.trimEnd('/')
    private val useLocal get() = BuildConfig.USE_LOCAL_KEYS

    private data class LocalUser(
        val name: String,
        val email: String,
        val password: String,
        var verified: Boolean = false,
        var otp: String = ""
    )
    private val localUsers = ConcurrentHashMap<String, LocalUser>()

    fun register(name: String, email: String, password: String): OtpResult {
        if (useLocal) return registerLocal(name, email, password)
        return try {
            val root = post("/register", JSONObject()
                .put("name", name).put("email", email).put("password", password))
            if (!root.optBoolean("success", false)) {
                throw IllegalStateException(root.optString("error").ifBlank { "Registration failed" })
            }
            OtpResult(emailed = true)
        } catch (_: Exception) {
            registerLocal(name, email, password)
        }
    }

    fun login(email: String, password: String): AuthUser {
        if (useLocal) return loginLocal(email, password)
        return try {
            val root = post("/login", JSONObject().put("email", email).put("password", password))
            if (root.optBoolean("needsVerification", false)) {
                throw UnverifiedException(
                    root.optString("error").ifBlank { "Email not verified" },
                    email = email,
                    emailed = true,
                    displayCode = null
                )
            }
            if (!root.optBoolean("success", false)) {
                throw IllegalStateException(root.optString("error").ifBlank { "Login failed" })
            }
            AuthUser(name = root.optString("name"), email = root.optString("email"))
        } catch (e: UnverifiedException) {
            throw e
        } catch (_: Exception) {
            loginLocal(email, password)
        }
    }

    fun verifyEmail(email: String, code: String): AuthUser {
        if (useLocal) return verifyLocal(email, code)
        return try {
            val root = post("/verify-email", JSONObject().put("email", email).put("code", code))
            if (!root.optBoolean("success", false)) {
                throw IllegalStateException(root.optString("error").ifBlank { "Verification failed" })
            }
            AuthUser(name = root.optString("name"), email = root.optString("email"))
        } catch (e: Exception) {
            verifyLocal(email, code)
        }
    }

    fun resendCode(email: String): OtpResult {
        if (useLocal) return resendLocal(email)
        return try {
            val root = post("/resend-code", JSONObject().put("email", email))
            if (!root.optBoolean("success", false)) {
                throw IllegalStateException(root.optString("error").ifBlank { "Could not resend" })
            }
            OtpResult(emailed = true)
        } catch (_: Exception) {
            resendLocal(email)
        }
    }

    private fun registerLocal(name: String, email: String, password: String): OtpResult {
        val key = email.lowercase()
        val code = "%06d".format((100000..999999).random())
        localUsers[key] = LocalUser(name, key, password, false, code)
        return OtpResult(emailed = false, code = code)
    }

    private fun loginLocal(email: String, password: String): AuthUser {
        val u = localUsers[email.lowercase()]
            ?: throw IllegalStateException("No account. Register first (local demo mode).")
        if (u.password != password) throw IllegalStateException("Invalid email or password")
        if (!u.verified) {
            u.otp = "%06d".format((100000..999999).random())
            throw UnverifiedException(
                "Email not verified",
                email = email,
                emailed = false,
                displayCode = u.otp
            )
        }
        return AuthUser(name = u.name, email = u.email)
    }

    private fun verifyLocal(email: String, code: String): AuthUser {
        val u = localUsers[email.lowercase()]
            ?: throw IllegalStateException("No account for this email")
        if (u.otp != code) throw IllegalStateException("Invalid verification code")
        u.verified = true
        u.otp = ""
        return AuthUser(name = u.name, email = u.email)
    }

    private fun resendLocal(email: String): OtpResult {
        val u = localUsers[email.lowercase()]
            ?: throw IllegalStateException("No account for this email")
        u.otp = "%06d".format((100000..999999).random())
        return OtpResult(emailed = false, code = u.otp)
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
