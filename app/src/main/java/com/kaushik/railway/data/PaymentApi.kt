package com.kaushik.railway.data

import com.kaushik.railway.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class RazorOrder(
    val orderId: String,
    val keyId: String,
    val amountPaise: Int,
    val currency: String
)

/**
 * Payment API — ALL secret operations go through the backend.
 * The Android app never holds Razorpay KEY_SECRET.
 */
object PaymentApi {
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val baseUrl: String
        get() = BuildConfig.BACKEND_BASE_URL.trimEnd('/')

    /**
     * Creates a Razorpay order via backend.
     * Backend holds the secret and returns order_id + key_id.
     */
    fun createOrder(amountRupees: Int, receipt: String = "railone"): RazorOrder {
        val body = JSONObject()
            .put("amount", amountRupees.coerceAtLeast(1))
            .put("receipt", receipt)
            .toString()
            .toRequestBody(jsonType)

        val req = Request.Builder()
            .url("$baseUrl/create-order")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        http.newCall(req).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            val root = JSONObject(raw.ifBlank { "{}" })
            if (!resp.isSuccessful || !root.optBoolean("success", false)) {
                val err = root.optString("error").ifBlank { "Create order failed (${resp.code})" }
                throw IllegalStateException(err)
            }
            val orderId = root.optString("order_id")
            if (orderId.isBlank()) throw IllegalStateException("Backend did not return an order id")
            return RazorOrder(
                orderId = orderId,
                keyId = root.optString("key_id"),
                amountPaise = root.optInt("amount"),
                currency = root.optString("currency", "INR")
            )
        }
    }

    /**
     * Verifies payment signature via backend.
     * Backend performs HMAC-SHA256 check with the secret.
     */
    fun verifyPayment(paymentId: String, orderId: String, signature: String): Boolean {
        val body = JSONObject()
            .put("razorpay_payment_id", paymentId)
            .put("razorpay_order_id", orderId)
            .put("razorpay_signature", signature)
            .toString()
            .toRequestBody(jsonType)

        val req = Request.Builder()
            .url("$baseUrl/verify-payment")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        http.newCall(req).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            val root = JSONObject(raw.ifBlank { "{}" })
            if (!resp.isSuccessful || !root.optBoolean("success", false) || !root.optBoolean("verified", false)) {
                val err = root.optString("error").ifBlank { "Payment verification failed" }
                throw IllegalStateException(err)
            }
            return true
        }
    }
}

object PaymentBridge {
    var onSuccess: ((paymentId: String, orderId: String, signature: String) -> Unit)? = null
    var onError: ((message: String) -> Unit)? = null
}
