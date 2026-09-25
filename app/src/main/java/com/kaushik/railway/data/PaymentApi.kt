package com.kaushik.railway.data

import android.util.Base64
import com.kaushik.railway.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class RazorOrder(
    val orderId: String,
    val keyId: String,
    val amountPaise: Int,
    val currency: String
)

/**
 * Payment API.
 * Keys are MASKED — loaded from BuildConfig (local.properties), never hardcoded in source.
 *
 * use.local.keys=true  → create order + verify signature on device (demo, no backend)
 * use.local.keys=false → proxy through optional backend
 */
object PaymentApi {
    @Volatile
    var bearerToken: String? = null

    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val keyId get() = BuildConfig.RAZORPAY_KEY_ID
    private val keySecret get() = BuildConfig.RAZORPAY_KEY_SECRET
    private val useLocal get() = BuildConfig.USE_LOCAL_KEYS
    private val baseUrl get() = BuildConfig.BACKEND_BASE_URL.trimEnd('/')

    fun createOrder(amountRupees: Int, receipt: String = "railx"): RazorOrder {
        if (useLocal) return createOrderLocal(amountRupees, receipt)
        return createOrderRemote(amountRupees, receipt)
    }

    fun verifyPayment(paymentId: String, orderId: String, signature: String): Boolean {
        if (useLocal) return verifyLocal(paymentId, orderId, signature)
        return verifyRemote(paymentId, orderId, signature)
    }

    // ── Local (masked keys in BuildConfig) ────────────────

    private fun createOrderLocal(amountRupees: Int, receipt: String): RazorOrder {
        requireConfigured()
        val paise = amountRupees.coerceAtLeast(1) * 100
        val safeReceipt = receipt.filter { it.isLetterOrDigit() || it == '_' }.take(40).ifBlank { "railx" }
        val body = JSONObject()
            .put("amount", paise)
            .put("currency", "INR")
            .put("receipt", safeReceipt)
            .put("payment_capture", 1)
            .toString()
            .toRequestBody(jsonType)
        val req = Request.Builder()
            .url("https://api.razorpay.com/v1/orders")
            .addHeader("Authorization", basicAuth())
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()
        http.newCall(req).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            val root = JSONObject(raw.ifBlank { "{}" })
            if (!resp.isSuccessful) {
                val desc = root.optJSONObject("error")?.optString("description")
                    ?: "Create order failed (${resp.code})"
                throw IllegalStateException(desc)
            }
            val orderId = root.optString("id")
            if (orderId.isBlank()) throw IllegalStateException("Razorpay did not return order id")
            return RazorOrder(
                orderId = orderId,
                keyId = keyId,
                amountPaise = root.optInt("amount", paise),
                currency = root.optString("currency", "INR")
            )
        }
    }

    private fun verifyLocal(paymentId: String, orderId: String, signature: String): Boolean {
        requireConfigured()
        val msg = "$orderId|$paymentId"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(keySecret.toByteArray(), "HmacSHA256"))
        val expected = mac.doFinal(msg.toByteArray()).joinToString("") { "%02x".format(it) }
        if (!expected.equals(signature, ignoreCase = true)) {
            throw IllegalStateException("Invalid payment signature")
        }
        return true
    }

    private fun requireConfigured() {
        if (keyId.isBlank() || keyId.contains("XXXX") ||
            keySecret.isBlank() || keySecret.startsWith("YOUR_")
        ) {
            throw IllegalStateException(
                "Razorpay keys not set. Add razorpay.key.id and razorpay.key.secret to local.properties"
            )
        }
    }

    private fun basicAuth(): String {
        val token = Base64.encodeToString("$keyId:$keySecret".toByteArray(), Base64.NO_WRAP)
        return "Basic $token"
    }

    private fun requireUserToken() {
        if (bearerToken.isNullOrBlank()) {
            throw IllegalStateException("Sign in with your RailX account before paying.")
        }
    }

    private fun Request.Builder.withUser(): Request.Builder {
        val token = bearerToken?.takeIf { it.isNotBlank() } ?: return this
        return addHeader("Authorization", "Bearer $token")
    }

    // ── Optional remote backend ───────────────────────────

    private fun createOrderRemote(amountRupees: Int, receipt: String): RazorOrder {
        requireUserToken()
        val body = JSONObject()
            .put("amount", amountRupees.coerceAtLeast(1))
            .put("receipt", receipt)
            .toString()
            .toRequestBody(jsonType)
        val req = Request.Builder()
            .url("$baseUrl/create-order")
            .addHeader("Content-Type", "application/json")
            .withUser()
            .post(body)
            .build()
        http.newCall(req).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            val root = JSONObject(raw.ifBlank { "{}" })
            if (!resp.isSuccessful || !root.optBoolean("success", false)) {
                throw IllegalStateException(root.optString("error").ifBlank { "Create order failed" })
            }
            return RazorOrder(
                orderId = root.optString("order_id"),
                keyId = root.optString("key_id"),
                amountPaise = root.optInt("amount"),
                currency = root.optString("currency", "INR")
            )
        }
    }

    private fun verifyRemote(paymentId: String, orderId: String, signature: String): Boolean {
        requireUserToken()
        val body = JSONObject()
            .put("razorpay_payment_id", paymentId)
            .put("razorpay_order_id", orderId)
            .put("razorpay_signature", signature)
            .toString()
            .toRequestBody(jsonType)
        val req = Request.Builder()
            .url("$baseUrl/verify-payment")
            .addHeader("Content-Type", "application/json")
            .withUser()
            .post(body)
            .build()
        http.newCall(req).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            val root = JSONObject(raw.ifBlank { "{}" })
            if (!resp.isSuccessful || !root.optBoolean("verified", false)) {
                throw IllegalStateException(root.optString("error").ifBlank { "Verification failed" })
            }
            return true
        }
    }
}

object PaymentBridge {
    var onSuccess: ((paymentId: String, orderId: String, signature: String) -> Unit)? = null
    var onError: ((message: String) -> Unit)? = null
}
