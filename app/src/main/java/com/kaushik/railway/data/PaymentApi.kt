package com.kaushik.railway.data

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

object PaymentApi {
    /** Emulator → host machine. Physical device: use your PC LAN IP. */
    const val BASE = "http://10.0.2.2:8088"
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun createOrder(amountRupees: Int, receipt: String = "railone"): RazorOrder {
        val body = JSONObject()
            .put("amount", amountRupees.coerceAtLeast(1))
            .put("receipt", receipt)
            .toString()
            .toRequestBody(jsonType)
        val req = Request.Builder().url("$BASE/create-order").post(body).build()
        http.newCall(req).execute().use { resp ->
            val root = JSONObject(resp.body?.string().orEmpty().ifBlank { "{}" })
            if (!root.optBoolean("success", false)) {
                throw IllegalStateException(root.optString("error", "Create order failed (${resp.code})"))
            }
            return RazorOrder(
                orderId = root.optString("order_id"),
                keyId = root.optString("key_id"),
                amountPaise = root.optInt("amount"),
                currency = root.optString("currency", "INR")
            )
        }
    }

    fun verifyPayment(paymentId: String, orderId: String, signature: String): Boolean {
        val body = JSONObject()
            .put("razorpay_payment_id", paymentId)
            .put("razorpay_order_id", orderId)
            .put("razorpay_signature", signature)
            .toString()
            .toRequestBody(jsonType)
        val req = Request.Builder().url("$BASE/verify-payment").post(body).build()
        http.newCall(req).execute().use { resp ->
            val root = JSONObject(resp.body?.string().orEmpty().ifBlank { "{}" })
            if (!root.optBoolean("success", false) || !root.optBoolean("verified", false)) {
                throw IllegalStateException(root.optString("error", "Payment verification failed"))
            }
            return true
        }
    }
}

object PaymentBridge {
    var onSuccess: ((paymentId: String, orderId: String, signature: String) -> Unit)? = null
    var onError: ((message: String) -> Unit)? = null
}
