package com.kaushik.railway.data

import android.content.Context
import android.util.Log
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession
import com.amplifyframework.auth.options.AuthConfirmSignUpOptions
import com.amplifyframework.auth.options.AuthFetchSessionOptions
import com.amplifyframework.auth.options.AuthResendSignUpCodeOptions
import com.amplifyframework.auth.options.AuthSignInOptions
import com.amplifyframework.auth.options.AuthSignOutOptions
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.auth.result.step.AuthSignUpStep
import com.amplifyframework.core.Amplify
import com.amplifyframework.kotlin.core.Amplify as KotlinAmplify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class CognitoCall {
    data class Ok(
        val username: String = "",
        val email: String = "",
        val name: String = "",
        val phone: String = "",
        val delivery: String = ""
    ) : CognitoCall()

    data class NeedsConfirm(
        val username: String,
        val delivery: String = "",
        val email: String = ""
    ) : CognitoCall()

    data class Offline(val message: String) : CognitoCall()
    data class Err(val message: String, val code: String = "") : CognitoCall()
}

/**
 * Amazon Cognito user pool RailX-Users (ap-south-1) via Amplify Auth.
 * Email confirmation is live. SMS stays off until a TRAI DLT header exists.
 * Profile, MPIN, and Aadhaar last-4 stay on the device.
 */
object CognitoAuth {
    private const val TAG = "CognitoAuth"

    @Volatile
    var ready: Boolean = false
        private set

    fun install(context: Context) {
        if (ready) return
        try {
            Amplify.addPlugin(AWSCognitoAuthPlugin())
            Amplify.configure(context.applicationContext)
            ready = true
            Log.i(TAG, "Amplify Cognito ready")
        } catch (e: Exception) {
            val msg = e.message.orEmpty()
            if (msg.contains("already", ignoreCase = true)) {
                ready = true
            } else {
                Log.e(TAG, "Amplify init failed", e)
            }
        }
    }

    suspend fun signUp(
        username: String,
        password: String,
        name: String,
        email: String,
        phoneE164: String
    ): CognitoCall = withContext(Dispatchers.IO) {
        if (!ready) return@withContext CognitoCall.Offline("Cognito is not configured on this device")
        val first = attemptSignUp(username, password, name, email, phoneE164)
        if (first is CognitoCall.Err && shouldRetryAsEmail(first.message)) {
            attemptSignUp(email, password, name, email, phoneE164)
        } else {
            first
        }
    }

    suspend fun confirm(username: String, code: String): CognitoCall = withContext(Dispatchers.IO) {
        if (!ready) return@withContext CognitoCall.Offline("Cognito is not configured on this device")
        try {
            val result = KotlinAmplify.Auth.confirmSignUp(
                username,
                code.trim(),
                AuthConfirmSignUpOptions.defaults()
            )
            val step = result.nextStep.signUpStep
            if (result.isSignUpComplete || step == AuthSignUpStep.DONE || step == AuthSignUpStep.COMPLETE_AUTO_SIGN_IN) {
                CognitoCall.Ok(username = username)
            } else {
                CognitoCall.NeedsConfirm(username, destination(result.nextStep.codeDeliveryDetails))
            }
        } catch (e: Exception) {
            mapError(e)
        }
    }

    suspend fun resend(username: String): CognitoCall = withContext(Dispatchers.IO) {
        if (!ready) return@withContext CognitoCall.Offline("Cognito is not configured on this device")
        try {
            val details = KotlinAmplify.Auth.resendSignUpCode(
                username,
                AuthResendSignUpCodeOptions.defaults()
            )
            CognitoCall.Ok(username = username, delivery = details.destination.orEmpty())
        } catch (e: Exception) {
            mapError(e)
        }
    }

    suspend fun signIn(username: String, password: String, allowRetry: Boolean = true): CognitoCall =
        withContext(Dispatchers.IO) {
            if (!ready) return@withContext CognitoCall.Offline("Cognito is not configured on this device")
            try {
                val result = KotlinAmplify.Auth.signIn(
                    username,
                    password,
                    AuthSignInOptions.defaults()
                )
                if (!result.isSignedIn && result.nextStep.signInStep == AuthSignInStep.CONFIRM_SIGN_UP) {
                    return@withContext CognitoCall.NeedsConfirm(username)
                }
                if (!result.isSignedIn) {
                    return@withContext CognitoCall.Err(
                        "Sign-in needs another step (${result.nextStep.signInStep}). Email confirmation is the supported step."
                    )
                }
                profileOf(username)
            } catch (e: Exception) {
                val mapped = mapError(e)
                if (mapped is CognitoCall.Err && mapped.code == "UNCONFIRMED") {
                    CognitoCall.NeedsConfirm(username)
                } else if (allowRetry && mapped is CognitoCall.Err && mapped.code == "SIGNED_IN") {
                    runCatching { KotlinAmplify.Auth.signOut(AuthSignOutOptions.builder().build()) }
                    signIn(username, password, allowRetry = false)
                } else {
                    mapped
                }
            }
        }

    suspend fun signOut() {
        if (!ready) return
        withContext(Dispatchers.IO) {
            runCatching { KotlinAmplify.Auth.signOut(AuthSignOutOptions.builder().build()) }
        }
    }

    suspend fun idToken(): String? = withContext(Dispatchers.IO) {
        if (!ready) return@withContext null
        try {
            val session = KotlinAmplify.Auth.fetchAuthSession(AuthFetchSessionOptions.defaults())
            val cognito = session as? AWSCognitoAuthSession ?: return@withContext null
            cognito.userPoolTokensResult.value?.idToken
        } catch (e: Exception) {
            Log.w(TAG, "ID token unavailable", e)
            null
        }
    }

    private suspend fun attemptSignUp(
        username: String,
        password: String,
        name: String,
        email: String,
        phoneE164: String
    ): CognitoCall {
        return try {
            val options = AuthSignUpOptions.builder()
                .userAttribute(AuthUserAttributeKey.name(), name)
                .userAttribute(AuthUserAttributeKey.email(), email)
                .userAttribute(AuthUserAttributeKey.phoneNumber(), phoneE164)
                .build()
            val result = KotlinAmplify.Auth.signUp(username, password, options)
            val delivery = destination(result.nextStep.codeDeliveryDetails)
            val step = result.nextStep.signUpStep
            if (result.isSignUpComplete || step == AuthSignUpStep.DONE) {
                CognitoCall.Ok(
                    username = username,
                    email = email,
                    name = name,
                    phone = phoneE164,
                    delivery = delivery
                )
            } else {
                CognitoCall.NeedsConfirm(username, delivery, email)
            }
        } catch (e: Exception) {
            mapError(e)
        }
    }

    private suspend fun profileOf(username: String): CognitoCall {
        val attrs = runCatching { KotlinAmplify.Auth.fetchUserAttributes() }.getOrDefault(emptyList())
        fun attr(key: String) = attrs.firstOrNull { it.key.keyString.equals(key, ignoreCase = true) }?.value.orEmpty()
        val current = runCatching { KotlinAmplify.Auth.getCurrentUser().username }.getOrDefault(username)
        return CognitoCall.Ok(
            username = current.ifBlank { username },
            email = attr("email"),
            name = attr("name"),
            phone = attr("phone_number")
        )
    }

    private fun destination(details: com.amplifyframework.auth.AuthCodeDeliveryDetails?): String =
        details?.destination?.toString().orEmpty()

    private fun mapError(t: Throwable): CognitoCall {
        if (isOffline(t)) return CognitoCall.Offline(rootMessage(t).ifBlank { "No internet" })
        val raw = chain(t)
        val code = when {
            raw.contains("UsernameExists", true) || raw.contains("User already exists", true) -> "EXISTS"
            raw.contains("AliasExists", true) -> "EMAIL_TAKEN"
            raw.contains("UserNotFound", true) -> "NOT_FOUND"
            raw.contains("NotAuthorized", true) || raw.contains("Incorrect username or password", true) -> "INVALID"
            raw.contains("UserNotConfirmed", true) || raw.contains("not confirmed", true) -> "UNCONFIRMED"
            raw.contains("CodeMismatch", true) -> "BAD_CODE"
            raw.contains("ExpiredCode", true) -> "EXPIRED"
            raw.contains("InvalidPassword", true) || raw.contains("Password did not conform", true) -> "BAD_PASSWORD"
            raw.contains("phone", true) && raw.contains("InvalidParameter", true) -> "BAD_PHONE"
            raw.contains("already a user", true) || raw.contains("signed in", true) && raw.contains("already", true) -> "SIGNED_IN"
            else -> ""
        }
        val message = when (code) {
            "EXISTS" -> "That user ID is already registered."
            "EMAIL_TAKEN" -> "That email or mobile is already registered."
            "NOT_FOUND" -> "No Cognito account for that user ID."
            "INVALID" -> "Invalid user ID or password."
            "UNCONFIRMED" -> "Confirm the email code before signing in."
            "BAD_CODE" -> "That code does not match."
            "EXPIRED" -> "That code expired. Resend it and try again."
            "BAD_PASSWORD" -> "Password must include upper, lower, a number, and a symbol."
            "BAD_PHONE" -> "Enter a valid 10-digit mobile number."
            "SIGNED_IN" -> "A session is already open."
            else -> rootMessage(t).ifBlank { "Cognito request failed" }.take(180)
        }
        return CognitoCall.Err(message, code)
    }

    private fun shouldRetryAsEmail(message: String): Boolean {
        val m = message.lowercase()
        return "username" in m && "email" in m
    }

    private fun chain(t: Throwable): String =
        generateSequence(t) { it.cause }.joinToString(" ") { "${it.javaClass.simpleName} ${it.message.orEmpty()}" }

    private fun rootMessage(t: Throwable): String =
        generateSequence(t) { it.cause }.mapNotNull { it.message?.takeIf(String::isNotBlank) }.lastOrNull()
            ?: t.message.orEmpty()

    private fun isOffline(t: Throwable): Boolean {
        val raw = chain(t)
        return listOf(
            "UnknownHost",
            "ConnectException",
            "SocketTimeout",
            "Unable to resolve",
            "Failed to connect",
            "NetworkError",
            "timeout"
        ).any { raw.contains(it, ignoreCase = true) }
    }
}
