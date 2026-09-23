package com.kaushik.railway.data.repository

import com.kaushik.railway.data.AuthApi
import com.kaushik.railway.data.SessionStore
import com.kaushik.railway.data.UnverifiedException
import com.kaushik.railway.data.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class AuthResult(
    val success: Boolean,
    val name: String = "",
    val email: String = "",
    val needsVerification: Boolean = false,
    val displayCode: String? = null,
    val error: String? = null
)

/**
 * Auth repository — delegates to AuthApi (local keys / offline-capable).
 */
class AuthRepository(private val sessionStore: SessionStore) {

    val sessionFlow: Flow<UserSession> = sessionStore.sessionFlow

    suspend fun register(name: String, email: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            try {
                val otp = AuthApi.register(name, email, password)
                AuthResult(
                    success = true,
                    email = email,
                    needsVerification = true,
                    displayCode = if (otp.emailed) null else otp.code
                )
            } catch (e: Exception) {
                AuthResult(success = false, error = e.message ?: "Registration failed")
            }
        }

    suspend fun verifyEmail(email: String, code: String): AuthResult =
        withContext(Dispatchers.IO) {
            try {
                val user = AuthApi.verifyEmail(email, code)
                sessionStore.saveSession(user.name, user.email)
                AuthResult(success = true, name = user.name, email = user.email)
            } catch (e: Exception) {
                AuthResult(success = false, error = e.message ?: "Verification failed")
            }
        }

    suspend fun login(email: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            try {
                val user = AuthApi.login(email, password)
                sessionStore.saveSession(user.name, user.email)
                AuthResult(success = true, name = user.name, email = user.email)
            } catch (e: UnverifiedException) {
                AuthResult(
                    success = false,
                    needsVerification = true,
                    email = e.email.ifBlank { email },
                    displayCode = e.displayCode,
                    error = e.message
                )
            } catch (e: Exception) {
                AuthResult(success = false, error = e.message ?: "Login failed")
            }
        }

    suspend fun resendCode(email: String): AuthResult =
        withContext(Dispatchers.IO) {
            try {
                val otp = AuthApi.resendCode(email)
                AuthResult(
                    success = true,
                    email = email,
                    displayCode = if (otp.emailed) null else otp.code
                )
            } catch (e: Exception) {
                AuthResult(success = false, error = e.message ?: "Could not resend")
            }
        }

    suspend fun logout() {
        sessionStore.clearSession()
    }

    suspend fun updateProfile(name: String, mobile: String) {
        sessionStore.updateProfile(name, mobile)
    }
}
