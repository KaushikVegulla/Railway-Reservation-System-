package com.kaushik.railway.data

import com.kaushik.railway.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AuthUser(val name: String, val email: String)
data class OtpResult(val emailed: Boolean, val code: String = "")

class UnverifiedException(message: String) : Exception(message)

/**
 * Thin HTTP client for auth endpoints on the backend.
 * Session persistence is handled by SessionStore / AuthRepository.
 */
object AuthApi {
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val baseUrl get() = BuildConfig.BACKEND_BASE_URL.trimEnd('/')

    fun register(name: String, email: String, password: String): OtpResult {
        val root = post("/register", JSONObject()
            .put("name", name)
            .put("email", email)
            .put("password", password))
        if (!root.optBoolean("success", false)) {
            throw IllegalStateException(root.optString("error").ifBlank { "Registration failed" })
        }
        return OtpResult(emailed = true)
    }

    fun login(email: String, password: String): AuthUser {
        val root = post("/login", JSONObject().put("email", email).put("password", password))
        if (root.optBoolean("needsVerification", false)) {
            throw UnverifiedException(root.optString("error").ifBlank { "Email not verified" })
        }
        if (!root.optBoolean("success", false)) {
            throw IllegalStateException(root.optString("error").ifBlank { "Login failed" })
        }
        return AuthUser(name = root.optString("name"), email = root.optString("email"))
    }

    fun verifyEmail(email: String, code: String): AuthUser {
        val root = post("/verify-email", JSONObject().put("email", email).put("code", code))
        if (!root.optBoolean("success", false)) {
            throw IllegalStateException(root.optString("error").ifBlank { "Verification failed" })
        }
        return AuthUser(name = root.optString("name"), email = root.optString("email"))
    }

    fun resendCode(email: String): OtpResult {
        val root = post("/resend-code", JSONObject().put("email", email))
        if (!root.optBoolean("success", false)) {
            throw IllegalStateException(root.optString("error").ifBlank { "Could not resend code" })
        }
        return OtpResult(emailed = true)
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
