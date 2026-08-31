package com.kaushik.railway.data

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * RailRadar REST client (https://api.railradar.in/v1).
 * Auth: Authorization Bearer + X-API-Key.
 */
object RailKitClient {
    private const val BASE = "https://api.railradar.in/v1"
    private const val API_KEY = "rg_27bdae37f34b4dd29a38dda71882394d"

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .addInterceptor(AuthInterceptor())
        .build()

    fun searchTrains(from: String, to: String, date: String): List<Train> {
        val iso = isoDate(date)
        val path = "/trains/between/${from.uppercase()}/${to.uppercase()}?date=${enc(iso)}&byCity=true"
        val root = getJson(path)
        if (!root.optBoolean("success", false)) {
            throw IllegalStateException(errorMessage(root, "Train search failed"))
        }
        val trains = root.optJSONObject("data")?.optJSONArray("trains") ?: JSONArray()
        return buildList {
            for (i in 0 until trains.length()) {
                val row = trains.getJSONObject(i)
                val train = row.optJSONObject("train") ?: JSONObject()
                val fromSt = row.optJSONObject("from") ?: JSONObject()
                val toSt = row.optJSONObject("to") ?: JSONObject()
                val mins = row.optInt("duration", 0)
                add(
                    Train(
                        number = train.optString("number"),
                        name = train.optString("name"),
                        from = fromSt.optString("name").ifBlank { fromSt.optString("code") },
                        to = toSt.optString("name").ifBlank { toSt.optString("code") },
                        fromCode = fromSt.optString("code"),
                        toCode = toSt.optString("code"),
                        depart = fromSt.optString("departure"),
                        arrive = toSt.optString("arrival"),
                        duration = formatDuration(mins),
                        days = formatRunDays(train.optJSONArray("runDays")),
                        distance = row.opt("distance")?.toString().orEmpty()
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
        val iso = isoDate(date)
        val q = "journeyDate=${enc(iso)}&source=${from.uppercase()}&destination=${to.uppercase()}&classCode=${coach.uppercase()}&quotaCode=${quota.uppercase()}"
        val root = getJson("/trains/$trainNo/seats?$q")
        if (!root.optBoolean("success", false)) {
            throw IllegalStateException(errorMessage(root, "Availability failed"))
        }
        val data = root.optJSONObject("data") ?: JSONObject()
        val calendar = data.optJSONArray("calendar") ?: data.optJSONArray("avlDayList") ?: JSONArray()
        val days = buildList {
            for (i in 0 until calendar.length()) {
                val d = calendar.getJSONObject(i)
                val text = d.optString("status").ifBlank { d.optString("availablityStatus") }
                val day = d.optString("date").ifBlank { d.optString("availablityDate") }
                add(
                    VacancyDay(
                        date = day,
                        status = d.optString("statusCode").ifBlank { text },
                        text = text,
                        prediction = if (d.optBoolean("isAvailable", false)) "Available" else d.optString("statusCode"),
                        canBook = d.optBoolean("isAvailable", text.contains("AVAILABLE", true))
                    )
                )
            }
        }
        val fare = runCatching { fareLookup(trainNo, from, to, date, coach, quota) }.getOrDefault(0)
        val match = days.firstOrNull { it.date == iso } ?: days.firstOrNull()
        return AvailabilityResult(
            fare = fare,
            status = match?.text ?: "N/A",
            days = days
        )
    }

    fun fareLookup(trainNo: String, from: String, to: String, date: String, coach: String, quota: String): Int {
        val iso = isoDate(date)
        val q = "journeyDate=${enc(iso)}&source=${from.uppercase()}&destination=${to.uppercase()}&classCode=${coach.uppercase()}&quotaCode=${quota.uppercase()}"
        val root = getJson("/trains/$trainNo/fare?$q")
        if (!root.optBoolean("success", false)) return 0
        return root.optJSONObject("data")?.optInt("totalFare", 0) ?: 0
    }

    fun checkPnr(pnr: String): PnrResult {
        val digits = pnr.filter { it.isDigit() }
        val root = getJson("/pnr/$digits")
        if (!root.optBoolean("success", false)) {
            throw IllegalStateException(errorMessage(root, "PNR lookup failed"))
        }
        val data = root.optJSONObject("data") ?: JSONObject()
        val train = data.optJSONObject("train") ?: JSONObject()
        val src = train.optJSONObject("source") ?: JSONObject()
        val dest = train.optJSONObject("destination") ?: JSONObject()
        val journey = data.optJSONObject("journey") ?: JSONObject()
        val chart = data.optJSONObject("charting") ?: JSONObject()
        val pax = data.optJSONArray("passengers") ?: JSONArray()
        val names = buildList {
            for (i in 0 until pax.length()) {
                val p = pax.getJSONObject(i)
                add(
                    "Pax ${p.optInt("passengerNumber")}: ${p.optString("currentStatus")} " +
                        "${p.optString("coach")}/${p.opt("berthNumber")}/${p.optString("berthCode")}"
                )
            }
        }
        return PnrResult(
            pnr = data.optString("pnrNumber", digits),
            trainNo = train.optString("number"),
            trainName = train.optString("name"),
            fromName = "${src.optString("name")} (${src.optString("code")})",
            toName = "${dest.optString("name")} (${dest.optString("code")})",
            date = journey.optString("date"),
            travelClass = journey.optString("class"),
            quota = journey.optString("quota"),
            chart = chart.optString("status"),
            fare = journey.optString("bookingFare").toIntOrNull() ?: 0,
            passengers = names
        )
    }

    fun trackTrain(trainNo: String, date: String): List<RunningStop> {
        val iso = isoDate(date)
        val root = getJson("/trains/${trainNo.trim()}/live?haltsOnly=true&date=${enc(iso)}")
        if (!root.optBoolean("success", false)) {
            throw IllegalStateException(errorMessage(root, "Live status failed"))
        }
        val data = root.optJSONObject("data") ?: JSONObject()
        val route = data.optJSONArray("route") ?: JSONArray()
        return buildList {
            for (i in 0 until route.length()) {
                val p = route.getJSONObject(i)
                if (!p.optBoolean("isHalt", true)) continue
                add(
                    RunningStop(
                        station = p.optString("stationName"),
                        code = p.optString("stationCode"),
                        schArr = timePart(p.optString("scheduledArrival")),
                        schDep = timePart(p.optString("scheduledDeparture")),
                        delayMin = p.optInt("delayDeparture", data.optInt("delayMinutes", 0)),
                        status = p.optString("status")
                    )
                )
            }
        }
    }

    fun searchStations(query: String): List<Station> {
        if (query.length < 2) return emptyList()
        val root = getJson("/lookup/search/stations?q=${enc(query)}")
        if (!root.optBoolean("success", false)) return emptyList()
        val arr = when (val data = root.opt("data")) {
            is JSONArray -> data
            is JSONObject -> data.optJSONArray("stations") ?: JSONArray()
            else -> JSONArray()
        }
        return buildList {
            for (i in 0 until arr.length()) {
                val s = arr.getJSONObject(i)
                add(Station(s.optString("code"), s.optString("name"), s.optString("city")))
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

    private fun errorMessage(root: JSONObject, fallback: String): String {
        val err = root.opt("error")
        return when (err) {
            is JSONObject -> err.optString("message", fallback)
            is String -> err.ifBlank { fallback }
            else -> fallback
        }
    }

    private fun isoDate(date: String): String {
        val t = date.trim()
        if (t.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return t
        if (t.matches(Regex("\\d{2}-\\d{2}-\\d{4}"))) {
            val p = t.split("-")
            return "${p[2]}-${p[1]}-${p[0]}"
        }
        return try {
            val inn = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
            val out = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            out.format(inn.parse(t)!!)
        } catch (_: Exception) {
            t
        }
    }

    private fun formatDuration(mins: Int): String {
        if (mins <= 0) return ""
        val h = mins / 60
        val m = mins % 60
        return "${h}h ${m}m"
    }

    private fun formatRunDays(arr: JSONArray?): String {
        if (arr == null || arr.length() == 0) return ""
        return (0 until arr.length()).joinToString(",") { arr.optString(it).take(3).replaceFirstChar { c -> c.uppercase() } }
    }

    private fun timePart(iso: String): String {
        if (iso.isBlank() || iso == "null") return "--"
        val t = iso.substringAfter('T', iso)
        return t.take(5)
    }

    private fun enc(value: String) = java.net.URLEncoder.encode(value, "UTF-8")

    private class AuthInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val signed = chain.request().newBuilder()
                .header("Authorization", "Bearer $API_KEY")
                .header("X-API-Key", API_KEY)
                .header("Accept", "application/json")
                .build()
            return chain.proceed(signed)
        }
    }
}
