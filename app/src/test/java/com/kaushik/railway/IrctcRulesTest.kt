package com.kaushik.railway

import com.kaushik.railway.data.IrctcRules
import com.kaushik.railway.data.ProfileDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class IrctcRulesTest {
    @Test
    fun userIdRules() {
        assertEquals("User ID must start with a letter", IrctcRules.validateUserId("1abc"))
        assertNull(IrctcRules.validateUserId("kaushik_01"))
    }

    @Test
    fun passwordRules() {
        assertNull(IrctcRules.validatePassword("RailOne1!"))
        assertTrue(IrctcRules.validatePassword("railone1!")!!.contains("uppercase"))
        assertTrue(IrctcRules.validatePassword("short") != null)
    }

    @Test
    fun masksHideTheRest() {
        val email = "kajal.kumari@example.com"
        val masked = IrctcRules.maskEmail(email)
        assertFalse(masked.contains("kajal.kumari"))
        assertTrue(masked.contains("@"))
        val mobile = "9876543210"
        val hidden = IrctcRules.maskMobile(mobile)
        assertEquals("******3210", hidden)
        assertFalse(hidden.contains("987654"))
    }

    @Test
    fun identityKeepsLastFourOnly() {
        val raw = "234567890123"
        val check = IrctcRules.checkIdentity("AADHAAR CARD / VID", raw, consent = true)
        assertTrue(check.ok)
        assertEquals("AADHAAR", check.kind)
        assertEquals("0123", check.last4)
        assertFalse(check.toString().contains(raw))
        assertFalse(IrctcRules.checkIdentity("AADHAAR CARD / VID", "123456789012", true).ok)
        val vid = "1234567890123456"
        val vidCheck = IrctcRules.checkIdentity("AADHAAR CARD / VID", vid, true)
        assertTrue(vidCheck.ok)
        assertEquals("VID", vidCheck.kind)
        assertEquals("3456", vidCheck.last4)
        assertFalse(vidCheck.toString().contains(vid))
        assertFalse(IrctcRules.checkIdentity("PAN CARD", "ABCDE1234F", true).ok)
        assertFalse(IrctcRules.checkIdentity("AADHAAR CARD / VID", raw, consent = false).ok)
    }

    @Test
    fun tatkalQuotas() {
        assertTrue(IrctcRules.isTatkalQuota("TQ - Tatkal"))
        assertTrue(IrctcRules.isTatkalQuota("PT - Premium Tatkal"))
        assertFalse(IrctcRules.isTatkalQuota("GN - General"))
    }

    @Test
    fun captchaAndMpin() {
        assertTrue(IrctcRules.captchaMatches("AB23C", " ab23c "))
        assertEquals("PIN and confirm PIN do not match", IrctcRules.validateMpin("1234", "1235"))
        assertNull(IrctcRules.validateMpin("1234", "1234"))
    }

    @Test
    fun dobRequiresAdult() {
        val today = Calendar.getInstance()
        today.set(2026, Calendar.SEPTEMBER, 25, 0, 0, 0)
        assertNull(IrctcRules.validateDob("25-09-2000", today))
        assertTrue(IrctcRules.validateDob("26-09-2008", today)!!.contains("18"))
        assertTrue(IrctcRules.validateDob("31-02-2000", today)!!.contains("DD-MM-YYYY"))
    }

    @Test
    fun profileNeedsTerms() {
        val draft = ProfileDraft(
            gender = "Female",
            dob = "01-01-1990",
            occupation = "Private",
            maritalStatus = "Unmarried",
            nationality = "India",
            addressLine = "12 Station Road",
            city = "Pune",
            state = "Maharashtra",
            country = "India",
            pinCode = "411001",
            acceptedTerms = false
        )
        assertTrue(IrctcRules.validateProfile(draft)!!.contains("terms"))
        assertNull(IrctcRules.validateProfile(draft.copy(acceptedTerms = true)))
    }

    @Test
    fun hashIsNotThePassword() {
        val hash = IrctcRules.secretHash("Kajal01", "RailOne1!")
        assertNotEquals("RailOne1!", hash)
        assertEquals(hash, IrctcRules.secretHash("kajal01", "RailOne1!"))
        assertEquals(64, hash.length)
    }
}
