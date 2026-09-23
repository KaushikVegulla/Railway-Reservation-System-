package com.kaushik.railway.data.repository

import com.kaushik.railway.BuildConfig
import com.kaushik.railway.data.SessionStore
import com.kaushik.railway.data.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AuthResult(
    val success: Boolean,
    val name: String = "",
    val email: String = "",
    val needsVerification: Boolean = false,
    val error: String? = null
)

class AuthRepository(private val sessionStore: SessionStore) {
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val baseUrl get() = BuildConfig.BACKEND_BASE_URL.trimEnd('/')

    val sessionFlow: Flow<UserSession> = sessionStore.sessionFlow

    suspend fun register(name: String, email: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            post("/register", JSONObject().put("name", name).put("email", email).put("password", password))
        }

    suspend fun verifyEmail(email: String, code: String): AuthResult =
        withContext(Dispatchers.IO) {
            val result = post("/verify-email", JSONObject().put("email", email).put("code", code))
            if (result.success) {
                sessionStore.saveSession(result.name, result.email)
            }
            result
        }

    suspend fun login(email: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            val result = post("/login", JSONObject().put("email", email).put("password", password))
            if (result.success) {
                sessionStore.saveSession(result.name, result.email)
            }
            result
        }

    suspend fun resendCode(email: String): AuthResult =
        withContext(Dispatchers.IO) {
            post("/resend-code", JSONObject().put("email", email))
        }

    suspend fun logout() {
        sessionStore.clearSession()
    }

    suspend fun updateProfile(name: String, mobile: String) {
        sessionStore.updateProfile(name, mobile)
    }

    private fun post(path: String, body: JSONObject): AuthResult {
        return try {
            val req = Request.Builder()
                .url("$baseUrl$path")
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody(jsonType))
                .build()
            http.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                val root = JSONObject(raw.ifBlank { "{}" })
                AuthResult(
                    success = root.optBoolean("success", false),
                    name = root.optString("name"),
                    email = root.optString("email"),
                    needsVerification = root.optBoolean("needsVerification", false),
                    error = root.optString("error").ifBlank { null }
                )
            }
        } catch (e: Exception) {
            AuthResult(success = false, error = e.message ?: "Network error")
        }
    }
}
