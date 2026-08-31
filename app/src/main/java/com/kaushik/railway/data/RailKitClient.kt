package com.kaushik.railway.data

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * RailKit REST client (https://api.railkit.in).
 * Requests are signed the same way as the official Node SDK.
 */
object RailKitClient {
    private const val BASE = "https://api.railkit.in"
    private const val API_KEY = "irctc_0e54d7e9d2ed9308fceaa289118b4cb76dffe93fa9632b1c"
    private const val SDK_SECRET = "97c56e08b27b161124f88acd4f24d1bd50f48075f11dc23b9ea6c0bc9b2f8794"

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .addInterceptor(SigningInterceptor())
        .build()

    fun searchTrains(from: String, to: String, date: String): List<Train> {
        val path = "/api/searchTrainBetweenStations/${from.uppercase()}/${to.uppercase()}?date=${enc(date)}"
        val root = getJson(path)
        if (!root.optBoolean("success", false)) {
            throw IllegalStateException(root.optString("error", "Train search failed"))
        }
        val data = root.optJSONArray("data") ?: JSONArray()
        return buildList {
            for (i in 0 until data.length()) {
                val o = data.getJSONObject(i)
                add(
                    Train(
                        number = o.optString("train_no"),
                        name = o.optString("train_name"),
                        from = o.optString("from_stn_name").ifBlank { o.optString("from_stn_code") },
                        to = o.optString("to_stn_name").ifBlank { o.optString("to_stn_code") },
                        fromCode = o.optString("from_stn_code"),
                        toCode = o.optString("to_stn_code"),
                        depart = o.optString("from_time"),
                        arrive = o.optString("to_time"),
                        duration = o.optString("travel_time"),
                        days = runningDays(o.optString("running_days")),
                        distance = o.optString("distance")
                    )
                )
            }
        }
    }

    fun getAvailability(
        trainNo: String,
        from: String,
        to: String,
        date: String,
        coach: String,
        quota: String
    ): AvailabilityResult {
        val path = "/api/getAvailability/$trainNo/${from.uppercase()}/${to.uppercase()}/${enc(date)}/${coach.uppercase()}/${quota.uppercase()}"
        val root = getJson(path)
        if (!root.optBoolean("success", false)) {
            throw IllegalStateException(root.optString("error", "Availability failed"))
        }
        val data = root.getJSONObject("data")
        val fare = data.optJSONObject("fare")?.optInt("totalFare", 0) ?: 0
        val daysArr = data.optJSONArray("availability") ?: JSONArray()
        val days = buildList {
            for (i in 0 until daysArr.length()) {
                val d = daysArr.getJSONObject(i)
                add(
                    VacancyDay(
                        date = d.optString("date"),
                        status = d.optString("status"),
                        text = d.optString("availabilityText"),
                        prediction = d.optString("prediction"),
                        canBook = d.optBoolean("canBook", false)
                    )
                )
            }
        }
        val today = days.firstOrNull()
        return AvailabilityResult(
            fare = fare,
            status = today?.text ?: today?.status ?: "N/A",
            days = days
        )
    }

    fun fareLookup(trainNo: String, from: String, to: String, date: String, coach: String, quota: String): Int {
        val path = "/api/fareLookup/$trainNo/${enc(date)}/${from.uppercase()}/${to.uppercase()}/${coach.uppercase()}/${quota.uppercase()}"
        val root = getJson(path)
        if (!root.optBoolean("success", false)) return 0
        return root.optJSONObject("data")?.optInt("totalFare", 0) ?: 0
    }

    fun checkPnr(pnr: String): PnrResult {
        val digits = pnr.filter { it.isDigit() }
        val root = getJson("/api/checkPNRStatus/$digits")
        if (!root.optBoolean("success", false)) {
            throw IllegalStateException(root.optString("error", "PNR lookup failed"))
        }
        val data = root.getJSONObject("data")
        val train = data.optJSONObject("train") ?: JSONObject()
        val journey = data.optJSONObject("journey") ?: JSONObject()
        val source = journey.optJSONObject("source") ?: JSONObject()
        val dest = journey.optJSONObject("destination") ?: JSONObject()
        val chart = data.optJSONObject("chart")?.optString("status").orEmpty()
        val fare = data.optJSONObject("booking")?.optInt("fare", 0) ?: 0
        val pax = data.optJSONArray("passengers") ?: JSONArray()
        val names = buildList {
            for (i in 0 until pax.length()) {
                val p = pax.getJSONObject(i)
                val cur = p.optJSONObject("current") ?: JSONObject()
                add("${p.optString("serialNumber")}: ${cur.optString("details")}")
            }
        }
        return PnrResult(
            pnr = data.optString("pnr", digits),
            trainNo = train.optString("number"),
            trainName = train.optString("name"),
            fromName = "${source.optString("name")} (${source.optString("code")})",
            toName = "${dest.optString("name")} (${dest.optString("code")})",
            date = journey.optString("dateOfJourney"),
            travelClass = journey.optString("class"),
            quota = journey.optString("quota"),
            chart = chart,
            fare = fare,
            passengers = names
        )
    }

    fun trackTrain(trainNo: String, date: String): List<RunningStop> {
        val path = "/api/trackTrain/${trainNo.trim()}/${enc(date)}"
        val root = getJson(path)
        if (!root.optBoolean("success", false)) {
            throw IllegalStateException(root.optString("error", "Live status failed"))
        }
        val timeline = root.optJSONObject("data")?.optJSONArray("timeline") ?: JSONArray()
        return buildList {
            for (i in 0 until timeline.length()) {
                val p = timeline.getJSONObject(i)
                if (p.optString("type") == "intermediate") continue
                val arr = p.optJSONObject("arrival")
                val dep = p.optJSONObject("departure")
                add(
                    RunningStop(
                        station = p.optString("stationName"),
                        code = p.optString("stationCode"),
                        schArr = arr?.optString("scheduled").orEmpty(),
                        schDep = dep?.optString("scheduled").orEmpty(),
                        delayMin = 0,
                        status = p.optString("status")
                    )
                )
            }
        }
    }

    fun searchStations(query: String): List<Station> {
        if (query.length < 2) return emptyList()
        val root = getJson("/api/stations/search?name=${enc(query)}")
        if (!root.optBoolean("success", false)) return emptyList()
        val arr = root.optJSONObject("data")?.optJSONArray("stations") ?: JSONArray()
        return buildList {
            for (i in 0 until arr.length()) {
                val s = arr.getJSONObject(i)
                val name = s.optString("name")
                add(Station(s.optString("code"), name, name))
            }
        }
    }

    private fun getJson(pathAndQuery: String): JSONObject {
        val req = Request.Builder().url(BASE + pathAndQuery).get().build()
        http.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            return try {
                JSONObject(body)
            } catch (_: Exception) {
                JSONObject().put("success", false).put("error", "Invalid response (${resp.code})")
            }
        }
    }

    private fun enc(value: String) = java.net.URLEncoder.encode(value, "UTF-8")

    private fun runningDays(mask: String): String {
        if (mask.length != 7 || mask.any { it != '0' && it != '1' }) return mask
        val names = listOf("S", "M", "T", "W", "T", "F", "S")
        return names.mapIndexed { i, n -> if (mask[i] == '1') n else "-" }.joinToString("")
    }

    private class SigningInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()
            val url = original.url
            val path = url.encodedPath + if (url.encodedQuery.isNullOrEmpty()) "" else "?${url.encodedQuery}"
            val ts = System.currentTimeMillis().toString()
            val nonce = randomHex(32)
            val payloadHash = sha256Hex("")
            val canonical = listOf("GET", path, ts, nonce, payloadHash, API_KEY).joinToString("\n")
            val signature = hmacSha256Hex(SDK_SECRET, canonical)
            val signed: Request = original.newBuilder()
                .header("x-api-key", API_KEY)
                .header("Accept", "application/json")
                .header("x-irctc-sdk-ts", ts)
                .header("x-irctc-sdk-nonce", nonce)
                .header("x-irctc-sdk-payload-sha256", payloadHash)
                .header("x-irctc-sdk-signature", signature)
                .header("x-irctc-sdk-version", "1")
                .build()
            return chain.proceed(signed)
        }
    }

    private fun sha256Hex(text: String): String {
        val d = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return d.joinToString("") { "%02x".format(it) }
    }

    private fun hmacSha256Hex(secret: String, message: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        return mac.doFinal(message.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun randomHex(bytes: Int): String {
        val buf = ByteArray(bytes)
        SecureRandom().nextBytes(buf)
        return buf.joinToString("") { "%02x".format(it) }
    }
}
