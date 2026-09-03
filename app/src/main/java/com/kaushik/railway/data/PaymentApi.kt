package com.kaushik.railway.data

import android.util.Base64
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

object PaymentApi {
    private const val KEY_ID = "rzp_test_TX3nfhZmQmQBe6"
    private const val KEY_SECRET = "RMoakqIkdaRbHUaMK0lpp2d9"
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun createOrder(amountRupees: Int, receipt: String = "railone"): RazorOrder {
        val paise = amountRupees.coerceAtLeast(1) * 100
        val body = JSONObject()
            .put("amount", paise)
            .put("currency", "INR")
            .put("receipt", receipt.take(40))
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
                throw IllegalStateException(root.optJSONObject("error")?.optString("description") ?: "Create order failed")
            }
            return RazorOrder(
                orderId = root.optString("id"),
                keyId = KEY_ID,
                amountPaise = root.optInt("amount", paise),
                currency = root.optString("currency", "INR")
            )
        }
    }

    fun verifyPayment(paymentId: String, orderId: String, signature: String): Boolean {
        val msg = "$orderId|$paymentId"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(KEY_SECRET.toByteArray(), "HmacSHA256"))
        val expected = mac.doFinal(msg.toByteArray()).joinToString("") { "%02x".format(it) }
        if (!expected.equals(signature, ignoreCase = true)) {
            throw IllegalStateException("Invalid payment signature")
        }
        return true
    }

    private fun basicAuth(): String {
        val token = Base64.encodeToString("$KEY_ID:$KEY_SECRET".toByteArray(), Base64.NO_WRAP)
        return "Basic $token"
    }
}

object PaymentBridge {
    var onSuccess: ((paymentId: String, orderId: String, signature: String) -> Unit)? = null
    var onError: ((message: String) -> Unit)? = null
}
