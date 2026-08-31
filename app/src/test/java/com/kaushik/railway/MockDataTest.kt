package com.kaushik.railway

import com.kaushik.railway.data.MockData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockDataTest {
    @Test
    fun stationsArePresent() {
        assertTrue(MockData.stations.size >= 10)
        assertTrue(MockData.stations.any { it.code == "NDLS" })
    }

    @Test
    fun trainsHaveClasses() {
        val trains = MockData.trains("NDLS", "MMCT")
        assertTrue(trains.isNotEmpty())
        assertTrue(trains.all { it.classes.isNotEmpty() })
    }

    @Test
    fun pnrLookupWorksForTenDigits() {
        val booking = MockData.pnrLookup("4521987630")
        assertNotNull(booking)
        assertEquals("4521987630", booking!!.pnr)
    }
}
