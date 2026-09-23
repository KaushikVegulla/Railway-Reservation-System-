package com.kaushik.railway

import com.kaushik.railway.data.Passenger
import com.kaushik.railway.data.Train
import com.kaushik.railway.data.TrainClassAvail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {
    @Test
    fun passengerDefaults() {
        val p = Passenger()
        assertEquals("", p.name)
        assertEquals("Male", p.gender)
        assertEquals("No Preference", p.berth)
    }

    @Test
    fun trainHoldsRoute() {
        val t = Train(
            number = "12951",
            name = "Rajdhani",
            from = "NDLS",
            to = "MMCT",
            depart = "16:55",
            arrive = "08:35",
            duration = "15h 40m",
            days = "Daily"
        )
        assertEquals("12951", t.number)
        assertTrue(t.name.contains("Rajdhani"))
    }

    @Test
    fun classAvailFare() {
        val c = TrainClassAvail("3A", "AC 3 Tier", 1250, "AVAILABLE", 12)
        assertEquals(1250, c.fare)
        assertEquals("3A", c.code)
    }
}
