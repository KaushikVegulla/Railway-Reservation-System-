package com.kaushik.railway

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.kaushik.railway.data.Booking
import com.kaushik.railway.data.MockData
import com.kaushik.railway.data.Passenger
import com.kaushik.railway.data.Train
import com.kaushik.railway.data.TrainClassAvail
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
    var passengers = mutableStateListOf(Passenger())
    var mobile by mutableStateOf("9876543210")
    var email by mutableStateOf("kaushik@example.com")
    var insurance by mutableStateOf(true)
    var lastBooking by mutableStateOf<Booking?>(null)
    val bookings = mutableStateListOf<Booking>()

    fun stationName(code: String) =
        MockData.stations.find { it.code == code }?.let { "${it.name} (${it.code})" } ?: code

    fun swapStations() {
        val tmp = fromCode
        fromCode = toCode
        toCode = tmp
    }

    fun searchTrains(): List<Train> = MockData.trains(fromCode, toCode)

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
            quota = selectedQuota.take(2),
            passengers = passengers.toList(),
            contact = mobile,
            status = if (cls.status.contains("AVAILABLE")) "CNF" else cls.status.substringBefore(" "),
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
    }
}
