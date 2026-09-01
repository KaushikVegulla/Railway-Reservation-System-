package com.kaushik.railway

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaushik.railway.data.AvailabilityResult
import com.kaushik.railway.data.Booking
import com.kaushik.railway.data.MockData
import com.kaushik.railway.data.Passenger
import com.kaushik.railway.data.PnrResult
import com.kaushik.railway.data.RailKitClient
import com.kaushik.railway.data.RunningStop
import com.kaushik.railway.data.Station
import com.kaushik.railway.data.Train
import com.kaushik.railway.data.TrainClassAvail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

class AppViewModel : ViewModel() {
    var loggedIn by mutableStateOf(false)
    var userId by mutableStateOf("demo_user")
    var userName by mutableStateOf("Kaushik Vegulla")

    var fromCode by mutableStateOf("NDLS")
    var toCode by mutableStateOf("MMCT")
    var journeyDate by mutableStateOf(defaultDate())
    var selectedClass by mutableStateOf("All Classes")
    var selectedQuota by mutableStateOf("GN - General")

    var selectedTrain by mutableStateOf<Train?>(null)
    var selectedTravelClass by mutableStateOf<TrainClassAvail?>(null)
    var vacancy by mutableStateOf<AvailabilityResult?>(null)
    var passengers = mutableStateListOf(Passenger())
    var mobile by mutableStateOf("9876543210")
    var email by mutableStateOf("kaushik@example.com")
    var insurance by mutableStateOf(true)
    var lastBooking by mutableStateOf<Booking?>(null)
    val bookings = mutableStateListOf<Booking>()

    val trains = mutableStateListOf<Train>()
    var searchLoading by mutableStateOf(false)
    var searchError by mutableStateOf<String?>(null)

    val classRows = mutableStateListOf<TrainClassAvail>()
    var availLoading by mutableStateOf(false)
    var availError by mutableStateOf<String?>(null)

    var pnrLoading by mutableStateOf(false)
    var pnrError by mutableStateOf<String?>(null)
    var pnrResult by mutableStateOf<PnrResult?>(null)

    var runningLoading by mutableStateOf(false)
    var runningError by mutableStateOf<String?>(null)
    val runningStops = mutableStateListOf<RunningStop>()
    var runningNote by mutableStateOf("")

    val stationSuggestions = mutableStateListOf<Station>()

    fun stationName(code: String) =
        MockData.stations.find { it.code == code }?.let { "${it.name} (${it.code})" }
            ?: stationSuggestions.find { it.code == code }?.let { "${it.name} (${it.code})" }
            ?: code

    fun swapStations() {
        val tmp = fromCode
        fromCode = toCode
        toCode = tmp
    }

    fun quotaCode() = selectedQuota.take(2)

    fun searchTrainsLive() {
        searchLoading = true
        searchError = null
        trains.clear()
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    RailKitClient.searchTrains(fromCode, toCode, toApiDate(journeyDate))
                }
                trains.addAll(result)
                if (result.isEmpty()) searchError = "No trains found for this route/date."
            } catch (e: Exception) {
                searchError = e.message ?: "Search failed"
            } finally {
                searchLoading = false
            }
        }
    }

    fun loadAvailability(train: Train) {
        selectedTrain = train
        availLoading = true
        availError = null
        classRows.clear()
        vacancy = null
        viewModelScope.launch {
            val classes = if (selectedClass == "All Classes") {
                listOf("1A", "2A", "3A", "3E", "SL", "CC", "EC", "2S")
            } else listOf(selectedClass)
            val quota = quotaCode()
            val date = toApiDate(journeyDate)
            val fetched = withContext(Dispatchers.IO) {
                classes.map { cls ->
                    async {
                        cls to runCatching {
                            val av = RailKitClient.getAvailability(
                                train.number, train.fromCode, train.toCode, date, cls, quota
                            )
                            TrainClassAvail(cls, className(cls), av.fare, av.status, 0)
                        }
                    }
                }.awaitAll()
            }
            classRows.addAll(fetched.mapNotNull { it.second.getOrNull() })
            if (classRows.isEmpty()) {
                availError = fetched.mapNotNull { it.second.exceptionOrNull()?.message }.firstOrNull()
                    ?: "No availability for ${train.fromCode} → ${train.toCode}."
            }
            availLoading = false
        }
    }

    fun loadVacancyChart(cls: TrainClassAvail) {
        selectedTravelClass = cls
        val train = selectedTrain ?: return
        viewModelScope.launch {
            try {
                vacancy = withContext(Dispatchers.IO) {
                    RailKitClient.getAvailability(
                        train.number, train.fromCode, train.toCode,
                        toApiDate(journeyDate), cls.code, quotaCode()
                    )
                }
            } catch (e: Exception) {
                availError = e.message
            }
        }
    }

    fun lookupPnr(pnr: String) {
        pnrLoading = true
        pnrError = null
        pnrResult = null
        viewModelScope.launch {
            try {
                pnrResult = withContext(Dispatchers.IO) { RailKitClient.checkPnr(pnr) }
            } catch (e: Exception) {
                pnrError = e.message ?: "PNR lookup failed"
            } finally {
                pnrLoading = false
            }
        }
    }

    fun loadRunning(trainNo: String) {
        runningLoading = true
        runningError = null
        runningStops.clear()
        viewModelScope.launch {
            try {
                val date = toApiDate(journeyDate)
                val stops = withContext(Dispatchers.IO) { RailKitClient.trackTrain(trainNo, date) }
                runningStops.addAll(stops)
                runningNote = if (stops.isEmpty()) "No live timeline for this train." else "Live from RailRadar"
            } catch (e: Exception) {
                runningError = e.message
            } finally {
                runningLoading = false
            }
        }
    }

    fun searchStations(query: String) {
        viewModelScope.launch {
            val found = withContext(Dispatchers.IO) { RailKitClient.searchStations(query) }
            stationSuggestions.clear()
            stationSuggestions.addAll(found)
        }
    }

    fun confirmBooking(): Booking {
        val train = selectedTrain!!
        val cls = selectedTravelClass!!
        val pnr = (1..10).map { Random.nextInt(0, 10) }.joinToString("")
        val ins = if (insurance) passengers.size * 15 else 0
        val amount = cls.fare * passengers.size + ins
        val booking = Booking(
            pnr = pnr,
            train = train,
            travelClass = cls,
            date = journeyDate,
            quota = quotaCode(),
            passengers = passengers.toList(),
            contact = mobile,
            status = cls.status,
            amount = amount,
            fromName = stationName(fromCode),
            toName = stationName(toCode)
        )
        lastBooking = booking
        bookings.add(0, booking)
        return booking
    }

    fun cancel(pnr: String) {
        val i = bookings.indexOfFirst { it.pnr == pnr }
        if (i >= 0) bookings[i] = bookings[i].copy(status = "CANCELLED")
        if (lastBooking?.pnr == pnr) lastBooking = lastBooking?.copy(status = "CANCELLED")
    }

    companion object {
        fun defaultDate(): String {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, 1)
            return SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(cal.time)
        }

        fun upcomingDates(): List<String> {
            val fmt = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
            val cal = Calendar.getInstance()
            return (0 until 14).map {
                if (it > 0) cal.add(Calendar.DAY_OF_YEAR, 1)
                fmt.format(cal.time)
            }
        }

        fun toApiDate(display: String): String {
            return try {
                val inFmt = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                val outFmt = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                outFmt.format(inFmt.parse(display)!!)
            } catch (_: Exception) {
                display
            }
        }

        fun className(code: String) = when (code) {
            "SL" -> "Sleeper"
            "3A" -> "AC 3 Tier"
            "2A" -> "AC 2 Tier"
            "1A" -> "AC First"
            "3E" -> "AC 3 Economy"
            "CC" -> "Chair Car"
            "2S" -> "Second Sitting"
            "EC" -> "Executive"
            else -> code
        }
    }
}
