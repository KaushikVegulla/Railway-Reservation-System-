package com.kaushik.railway.data

object MockData {
    val stations = listOf(
        Station("NDLS", "New Delhi", "Delhi"),
        Station("MMCT", "Mumbai Central", "Mumbai"),
        Station("MAS", "Chennai Central", "Chennai"),
        Station("HWH", "Howrah Jn", "Kolkata"),
        Station("SBC", "KSR Bengaluru", "Bengaluru"),
        Station("BCT", "Mumbai Central", "Mumbai"),
        Station("CSMT", "CSTM Mumbai", "Mumbai"),
        Station("HYB", "Hyderabad Decan", "Hyderabad"),
        Station("SC", "Secunderabad Jn", "Hyderabad"),
        Station("PUNE", "Pune Jn", "Pune"),
        Station("ADI", "Ahmedabad Jn", "Ahmedabad"),
        Station("JP", "Jaipur Jn", "Jaipur"),
        Station("LKO", "Lucknow NR", "Lucknow"),
        Station("PNBE", "Patna Jn", "Patna"),
        Station("BZA", "Vijayawada Jn", "Vijayawada"),
        Station("ERS", "Ernakulam Jn", "Kochi"),
        Station("CNB", "Kanpur Central", "Kanpur"),
        Station("BBS", "Bhubaneswar", "Bhubaneswar"),
        Station("GWL", "Gwalior", "Gwalior"),
        Station("AGC", "Agra Cantt", "Agra")
    )

    val quotas = listOf("GN - General", "TQ - Tatkal", "LD - Ladies", "SS - Senior Citizen", "PT - Premium Tatkal")
    val classes = listOf("SL", "3A", "2A", "1A", "CC", "EC", "2S")
    val berths = listOf("No Preference", "Lower", "Middle", "Upper", "Side Lower", "Side Upper", "Window")

    fun trains(from: String, to: String): List<Train> {
        val f = from.ifBlank { "NDLS" }
        val t = to.ifBlank { "MMCT" }
        return listOf(
            Train(
                "12952", "Mumbai Rajdhani", f, t, "16:55", "08:35", "15h 40m", "Daily",
                listOf(
                    TrainClassAvail("1A", "AC First", 4520, "AVAILABLE 12", 12),
                    TrainClassAvail("2A", "AC 2 Tier", 2680, "AVAILABLE 28", 28),
                    TrainClassAvail("3A", "AC 3 Tier", 1860, "RAC 4", 0)
                )
            ),
            Train(
                "12954", "August Kranti RJ", f, t, "17:40", "10:55", "17h 15m", "Daily",
                listOf(
                    TrainClassAvail("2A", "AC 2 Tier", 2410, "WL 14", 0),
                    TrainClassAvail("3A", "AC 3 Tier", 1685, "AVAILABLE 9", 9),
                    TrainClassAvail("SL", "Sleeper", 625, "AVAILABLE 64", 64)
                )
            ),
            Train(
                "22210", "NDLS MMCT Duronto", f, t, "23:25", "16:40", "17h 15m", "Tue, Sat",
                listOf(
                    TrainClassAvail("1A", "AC First", 4210, "AVAILABLE 4", 4),
                    TrainClassAvail("2A", "AC 2 Tier", 2520, "AVAILABLE 18", 18),
                    TrainClassAvail("3A", "AC 3 Tier", 1740, "AVAILABLE 41", 41)
                )
            ),
            Train(
                "12926", "Paschim SF Exp", f, t, "11:25", "05:50", "18h 25m", "Daily",
                listOf(
                    TrainClassAvail("2A", "AC 2 Tier", 1985, "AVAILABLE 7", 7),
                    TrainClassAvail("3A", "AC 3 Tier", 1380, "GNWL 22", 0),
                    TrainClassAvail("SL", "Sleeper", 520, "AVAILABLE 112", 112),
                    TrainClassAvail("2S", "Second Sitting", 310, "AVAILABLE 180", 180)
                )
            ),
            Train(
                "12432", "Trivandrum Raj", f, t, "10:55", "06:10", "19h 15m", "Tue, Thu, Fri",
                listOf(
                    TrainClassAvail("1A", "AC First", 5120, "AVAILABLE 2", 2),
                    TrainClassAvail("2A", "AC 2 Tier", 3010, "RAC 1", 0),
                    TrainClassAvail("3A", "AC 3 Tier", 2095, "AVAILABLE 16", 16)
                )
            )
        )
    }

    fun runningStops(trainNo: String): List<RunningStop> = listOf(
        RunningStop("New Delhi", "NDLS", "--", "16:55", 0, "Departed"),
        RunningStop("Mathura Jn", "MTJ", "18:48", "18:50", 8, "Departed"),
        RunningStop("Kota Jn", "KOTA", "22:05", "22:15", 12, "Departed"),
        RunningStop("Ratlam Jn", "RTM", "01:20", "01:25", 18, "Arriving"),
        RunningStop("Vadodara Jn", "BRC", "04:18", "04:28", 0, "Yet to start"),
        RunningStop("Surat", "ST", "06:10", "06:15", 0, "Yet to start"),
        RunningStop("Mumbai Central", "MMCT", "08:35", "--", 0, "Yet to start")
    )

    fun pnrLookup(pnr: String): Booking? {
        if (pnr.length < 10) return null
        val train = trains("NDLS", "MMCT").first()
        return Booking(
            pnr = pnr.take(10),
            train = train,
            travelClass = train.classes[1],
            date = "12 Sep 2026",
            quota = "GN",
            passengers = listOf(
                Passenger("RAHUL SHARMA", "32", "Male", "Lower", "None"),
                Passenger("ANITA SHARMA", "29", "Female", "Upper", "None")
            ),
            contact = "9876543210",
            status = "CNF / B2 / 21,22",
            amount = 5360,
            fromName = "New Delhi (NDLS)",
            toName = "Mumbai Central (MMCT)"
        )
    }
}
