package com.kaushik.railway.data

data class Station(val code: String, val name: String, val city: String)

data class TrainClassAvail(
    val code: String,
    val name: String,
    val fare: Int,
    val status: String,
    val seats: Int
)

data class Train(
    val number: String,
    val name: String,
    val from: String,
    val to: String,
    val depart: String,
    val arrive: String,
    val duration: String,
    val days: String,
    val fromCode: String = from,
    val toCode: String = to,
    val distance: String = "",
    val classes: List<TrainClassAvail> = emptyList()
)

data class VacancyDay(
    val date: String,
    val status: String,
    val text: String,
    val prediction: String,
    val canBook: Boolean
)

data class AvailabilityResult(
    val fare: Int,
    val status: String,
    val days: List<VacancyDay>
)

data class PnrResult(
    val pnr: String,
    val trainNo: String,
    val trainName: String,
    val fromName: String,
    val toName: String,
    val date: String,
    val travelClass: String,
    val quota: String,
    val chart: String,
    val fare: Int,
    val passengers: List<String>
)

data class Passenger(
    val name: String = "",
    val age: String = "",
    val gender: String = "Male",
    val berth: String = "No Preference",
    val concession: String = "None"
)

data class Booking(
    val pnr: String,
    val train: Train,
    val travelClass: TrainClassAvail,
    val date: String,
    val quota: String,
    val passengers: List<Passenger>,
    val contact: String,
    val status: String,
    val amount: Int,
    val fromName: String,
    val toName: String,
    val paymentId: String = "",
    val orderId: String = ""
)

data class RunningStop(
    val station: String,
    val code: String,
    val schArr: String,
    val schDep: String,
    val delayMin: Int,
    val status: String
)
