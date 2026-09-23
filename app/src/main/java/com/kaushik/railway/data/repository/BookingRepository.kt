package com.kaushik.railway.data.repository

import com.kaushik.railway.data.Booking
import com.kaushik.railway.data.Passenger
import com.kaushik.railway.data.Train
import com.kaushik.railway.data.TrainClassAvail
import com.kaushik.railway.data.db.AppDatabase
import com.kaushik.railway.data.db.BookingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class BookingRepository(private val db: AppDatabase) {

    private val dao = db.bookingDao()

    fun observeBookings(): Flow<List<Booking>> =
        dao.getAllBookings().map { list -> list.map { it.toDomain() } }

    suspend fun save(booking: Booking) {
        dao.insert(booking.toEntity())
    }

    suspend fun cancel(pnr: String) {
        dao.updateStatus(pnr, "CANCELLED")
    }

    suspend fun getByPnr(pnr: String): Booking? = dao.getByPnr(pnr)?.toDomain()

    private fun BookingEntity.toDomain(): Booking {
        val pax = mutableListOf<Passenger>()
        try {
            val arr = JSONArray(passengersJson)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                pax.add(
                    Passenger(
                        name = o.optString("name"),
                        age = o.optString("age"),
                        gender = o.optString("gender", "Male"),
                        berth = o.optString("berth", "No Preference"),
                        concession = o.optString("concession", "None")
                    )
                )
            }
        } catch (_: Exception) { /* ignore malformed */ }

        val train = Train(
            number = trainNumber,
            name = trainName,
            from = fromName,
            to = toName,
            fromCode = fromCode,
            toCode = toCode,
            depart = "",
            arrive = "",
            duration = "",
            days = ""
        )
        val cls = TrainClassAvail(
            code = travelClassCode,
            name = travelClassName,
            fare = fare,
            status = status,
            seats = 0
        )
        return Booking(
            pnr = pnr,
            train = train,
            travelClass = cls,
            date = date,
            quota = quota,
            passengers = pax,
            contact = contact,
            status = status,
            amount = amount,
            fromName = fromName,
            toName = toName,
            paymentId = paymentId,
            orderId = orderId
        )
    }

    private fun Booking.toEntity(): BookingEntity {
        val arr = JSONArray()
        passengers.forEach { p ->
            arr.put(
                JSONObject()
                    .put("name", p.name)
                    .put("age", p.age)
                    .put("gender", p.gender)
                    .put("berth", p.berth)
                    .put("concession", p.concession)
            )
        }
        return BookingEntity(
            pnr = pnr,
            trainNumber = train.number,
            trainName = train.name,
            fromCode = train.fromCode,
            toCode = train.toCode,
            fromName = fromName,
            toName = toName,
            travelClassCode = travelClass.code,
            travelClassName = travelClass.name,
            fare = travelClass.fare,
            date = date,
            quota = quota,
            passengersJson = arr.toString(),
            contact = contact,
            status = status,
            amount = amount,
            paymentId = paymentId,
            orderId = orderId
        )
    }
}
