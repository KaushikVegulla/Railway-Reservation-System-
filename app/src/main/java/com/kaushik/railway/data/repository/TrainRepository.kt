package com.kaushik.railway.data.repository

import com.kaushik.railway.data.AvailabilityResult
import com.kaushik.railway.data.PnrResult
import com.kaushik.railway.data.RailKitClient
import com.kaushik.railway.data.RunningStop
import com.kaushik.railway.data.Station
import com.kaushik.railway.data.Train
import com.kaushik.railway.data.TrainClassAvail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

class TrainRepository {

    suspend fun searchTrains(from: String, to: String, date: String): Result<List<Train>> =
        withContext(Dispatchers.IO) {
            runCatching { RailKitClient.searchTrains(from, to, date) }
        }

    suspend fun loadClassAvailability(
        train: Train,
        date: String,
        classes: List<String>,
        quota: String
    ): Result<List<TrainClassAvail>> = withContext(Dispatchers.IO) {
        runCatching {
            classes.map { cls ->
                async {
                    val av = RailKitClient.getAvailability(
                        train.number, train.fromCode, train.toCode, date, cls, quota
                    )
                    TrainClassAvail(cls, className(cls), av.fare, av.status, 0)
                }
            }.awaitAll()
        }
    }

    suspend fun getVacancy(
        trainNo: String, from: String, to: String, date: String, coach: String, quota: String
    ): Result<AvailabilityResult> = withContext(Dispatchers.IO) {
        runCatching { RailKitClient.getAvailability(trainNo, from, to, date, coach, quota) }
    }

    suspend fun checkPnr(pnr: String): Result<PnrResult> = withContext(Dispatchers.IO) {
        runCatching { RailKitClient.checkPnr(pnr) }
    }

    suspend fun trackTrain(trainNo: String, date: String): Result<List<RunningStop>> =
        withContext(Dispatchers.IO) {
            runCatching { RailKitClient.trackTrain(trainNo, date) }
        }

    suspend fun searchStations(query: String): List<Station> = withContext(Dispatchers.IO) {
        runCatching { RailKitClient.searchStations(query) }.getOrDefault(emptyList())
    }

    private fun className(code: String) = when (code) {
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
