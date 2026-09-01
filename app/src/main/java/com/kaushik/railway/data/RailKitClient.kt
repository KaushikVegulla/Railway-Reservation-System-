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
 * RailRadar REST client — https://api.railradar.in/v1
 * Auth: Authorization: Bearer <key>
 */
object RailKitClient {
    private const val BASE = "https://api.railradar.in/v1"
    private const val API_KEY = "rg_27bdae37f34b4dd29a38dda71882394d"

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(AuthInterceptor())
        .build()

    fun searchTrains(from: String, to: String, date: String): List<Train> {
        val iso = isoDate(date)
        val src = from.uppercase()
        val dst = to.uppercase()
        val root = getJson("/trains/between/$src/$dst?date=${enc(iso)}")
        if (!root.optBoolean("success", false)) {
            throw IllegalStateException(errorMessage(root, "Train search failed"))
        }
        val data = root.optJSONObject("data") ?: JSONObject()
        val trains = data.optJSONArray("trains") ?: JSONArray()
        return buildList {
            for (i in 0 until trains.length()) {
                val row = trains.getJSONObject(i)
                val train = row.optJSONObject("train") ?: JSONObject()
                val fromSt = row.optJSONObject("from") ?: JSONObject()
                val toSt = row.optJSONObject("to") ?: JSONObject()
                val mins = number(row, "duration").toInt()
                add(
                    Train(
                        number = padTrain(train.optString("number")),
                        name = train.optString("name"),
                        from = fromSt.optString("name").ifBlank { fromSt.optString("code") },
                        to = toSt.optString("name").ifBlank { toSt.optString("code") },
                        fromCode = fromSt.optString("code").ifBlank { src },
                        toCode = toSt.optString("code").ifBlank { dst },
                        depart = fromSt.optString("departure"),
                        arrive = toSt.optString("arrival"),
                        duration = formatDuration(mins),
                        days = formatRunDays(train.optJSONArray("runDays")),
                        distance = number(row, "distance").let { if (it <= 0) "" else "${it.toInt()} km" }
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
        val no = padTrain(trainNo)
        val q =
            "journeyDate=${enc(iso)}&source=${from.uppercase()}&destination=${to.uppercase()}" +
                "&classCode=${coach.uppercase()}&quotaCode=${quota.uppercase()}"
        val root = getJson("/trains/$no/seats?$q")
        if (!root.optBoolean("success", false)) {
            throw IllegalStateException(errorMessage(root, "Availability failed"))
        }
        val data = root.optJSONObject("data") ?: JSONObject()
        val calendar = data.optJSONArray("calendar") ?: data.optJSONArray("avlDayList") ?: JSONArray()
        val days = buildList {
            for (i in 0 until calendar.length()) {
                val d = calendar.getJSONObject(i)
                val text = firstString(d, "status", "availablityStatus", "availabilityStatus")
                val day = firstString(d, "date", "rawDate", "availablityDate")
                add(
                    VacancyDay(
                        date = day,
                        status = d.optString("statusCode").ifBlank { text },
                        text = text,
                        prediction = d.optString("statusCode").ifBlank {
                            if (d.optBoolean("isAvailable", false)) "Available" else ""
                        },
                        canBook = d.optBoolean("isAvailable", text.contains("AVAILABLE", true))
                    )
                )
            }
        }
        val fare = fareLookup(no, from, to, iso, coach, quota)
        val match = days.firstOrNull { it.date == iso || it.date.startsWith(iso) } ?: days.firstOrNull()
        return AvailabilityResult(
            fare = fare,
            status = match?.text ?: "N/A",
            days = days
        )
    }

    fun fareLookup(trainNo: String, from: String, to: String, date: String, coach: String, quota: String): Int {
        val iso = isoDate(date)
        val no = padTrain(trainNo)
        val q =
            "journeyDate=${enc(iso)}&source=${from.uppercase()}&destination=${to.uppercase()}" +
                "&classCode=${coach.uppercase()}&quotaCode=${quota.uppercase()}"
        val root = getJson("/trains/$no/fare?$q")
        if (!root.optBoolean("success", false)) return 0
        val data = root.optJSONObject("data") ?: return 0
        val top = data.optInt("totalFare", 0)
        if (top > 0) return top
        return data.optJSONObject("breakdown")?.optInt("totalFare", 0) ?: 0
    }

    fun checkPnr(pnr: String): PnrResult {
        val digits = pnr.filter { it.isDigit() }
        if (digits.length != 10) throw IllegalStateException("PNR must be 10 digits")
        val root = getJson("/pnr/$digits")
        if (!root.optBoolean("success", false)) {
            throw IllegalStateException(errorMessage(root, "PNR lookup failed"))
        }
        val data = root.optJSONObject("data") ?: JSONObject()
        val train = data.optJSONObject("train") ?: JSONObject()
        val src = nestedStation(train, "source", "from")
        val dest = nestedStation(train, "destination", "to")
        val journey = data.optJSONObject("journey") ?: JSONObject()
        val chart = data.optJSONObject("charting") ?: data.optJSONObject("chart") ?: JSONObject()
        val pax = data.optJSONArray("passengers") ?: JSONArray()
        val names = buildList {
            for (i in 0 until pax.length()) {
                val p = pax.getJSONObject(i)
                val n = p.optInt("passengerNumber", i + 1)
                val status = firstString(p, "currentStatus", "bookingStatus")
                val coach = p.optString("coach")
                val berth = p.opt("berthNumber")?.toString().orEmpty()
                val code = p.optString("berthCode")
                add("Passenger $n: $status $coach/$berth/$code".trim())
            }
        }
        val fareRaw = journey.opt("bookingFare") ?: journey.opt("fare")
        val fare = when (fareRaw) {
            is Number -> fareRaw.toInt()
            is String -> fareRaw.filter { it.isDigit() }.toIntOrNull() ?: 0
            else -> 0
        }
        return PnrResult(
            pnr = firstString(data, "pnrNumber", "pnr").ifBlank { digits },
            trainNo = train.optString("number"),
            trainName = train.optString("name"),
            fromName = "${src.first} (${src.second})",
            toName = "${dest.first} (${dest.second})",
            date = journey.optString("date"),
            travelClass = journey.optString("class"),
            quota = journey.optString("quota"),
            chart = firstString(chart, "status", "chartStatus"),
            fare = fare,
            passengers = names
        )
    }

    fun trackTrain(trainNo: String, date: String): List<RunningStop> {
        val no = padTrain(trainNo)
        // Omit date so RailRadar auto-detects the current run (docs default).
        val root = getJson("/trains/$no/live?haltsOnly=true")
        if (!root.optBoolean("success", false)) {
            throw IllegalStateException(errorMessage(root, "Live status failed"))
        }
        val data = root.optJSONObject("data") ?: JSONObject()
        val delay = data.optInt("delayMinutes", 0)
        val route = data.optJSONArray("route") ?: JSONArray()
        return buildList {
            for (i in 0 until route.length()) {
                val p = route.getJSONObject(i)
                if (!p.optBoolean("isHalt", true)) continue
                val st = p.optJSONObject("station")
                add(
                    RunningStop(
                        station = p.optString("stationName").ifBlank { st?.optString("name").orEmpty() },
                        code = p.optString("stationCode").ifBlank { st?.optString("code").orEmpty() },
                        schArr = timePart(firstString(p, "scheduledArrival", "arrival")),
                        schDep = timePart(firstString(p, "scheduledDeparture", "departure")),
                        delayMin = if (p.has("delayDeparture") && !p.isNull("delayDeparture")) {
                            p.optInt("delayDeparture")
                        } else delay,
                        status = p.optString("status").ifBlank { data.optString("status") }
                    )
                )
            }
        }
    }

    fun searchStations(query: String): List<Station> {
        if (query.length < 2) return emptyList()
        val root = getJson("/lookup/search/stations?q=${enc(query)}&limit=20")
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
            val parsed = try {
                JSONObject(body)
            } catch (_: Exception) {
                JSONObject().put("success", false).put("error", "Invalid response (${resp.code}): ${body.take(120)}")
            }
            if (!parsed.has("success") && resp.isSuccessful) parsed.put("success", true)
            if (!resp.isSuccessful && parsed.optBoolean("success", true)) {
                parsed.put("success", false)
                if (!parsed.has("error")) parsed.put("error", "HTTP ${resp.code}")
            }
            return parsed
        }
    }

    private fun errorMessage(root: JSONObject, fallback: String): String {
        val err = root.opt("error")
        return when (err) {
            is JSONObject -> err.optString("message").ifBlank { err.optString("code", fallback) }
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

    private fun padTrain(n: String) = n.filter { it.isDigit() }.padStart(5, '0').takeLast(5)

    private fun number(o: JSONObject, key: String): Double {
        if (!o.has(key) || o.isNull(key)) return 0.0
        return when (val v = o.opt(key)) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }

    private fun firstString(o: JSONObject, vararg keys: String): String {
        for (k in keys) {
            val v = o.optString(k)
            if (v.isNotBlank() && v != "null") return v
        }
        return ""
    }

    private fun nestedStation(parent: JSONObject, vararg keys: String): Pair<String, String> {
        for (k in keys) {
            val st = parent.optJSONObject(k) ?: continue
            val name = st.optString("name")
            val code = st.optString("code")
            if (name.isNotBlank() || code.isNotBlank()) return name to code
        }
        return "" to ""
    }

    private fun formatDuration(mins: Int): String {
        if (mins <= 0) return ""
        return "${mins / 60}h ${mins % 60}m"
    }

    private fun formatRunDays(arr: JSONArray?): String {
        if (arr == null || arr.length() == 0) return ""
        return (0 until arr.length()).joinToString(",") {
            arr.optString(it).take(3).replaceFirstChar { c -> c.uppercase() }
        }
    }

    private fun timePart(iso: String): String {
        if (iso.isBlank() || iso == "null") return "--"
        val t = iso.substringAfter('T', iso)
        return if (t.length >= 5) t.take(5) else t
    }

    private fun enc(value: String) = java.net.URLEncoder.encode(value, "UTF-8")

    private class AuthInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val signed = chain.request().newBuilder()
                .header("Authorization", "Bearer $API_KEY")
                .header("Accept", "application/json")
                .build()
            return chain.proceed(signed)
        }
    }
}
