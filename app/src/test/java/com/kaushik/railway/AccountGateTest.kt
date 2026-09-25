package com.kaushik.railway

import com.kaushik.railway.data.IrctcRules
import com.kaushik.railway.nav.AccountGate
import com.kaushik.railway.nav.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccountGateTest {
    @Test
    fun signedOutStaysOnLogin() {
        assertEquals(
            Routes.Login,
            AccountGate.destination(false, false, false, false, false)
        )
    }

    @Test
    fun coldStartWithBiometricUnlocksFirst() {
        assertEquals(
            Routes.Unlock,
            AccountGate.destination(true, true, true, false, true)
        )
    }

    @Test
    fun incompleteProfileThenMpinThenHome() {
        assertEquals(Routes.Activate, AccountGate.destination(true, false, false, false, false))
        assertEquals(Routes.Mpin, AccountGate.destination(true, true, false, false, false))
        assertEquals(Routes.Home, AccountGate.destination(true, true, false, true, false))
        assertEquals(Routes.Home, AccountGate.destination(true, true, true, false, false))
    }

    @Test
    fun indianMobileBecomesE164() {
        assertEquals("+919876543210", IrctcRules.toIndianE164("9876543210"))
        assertEquals("+919876543210", IrctcRules.toIndianE164("+91 98765 43210"))
        assertNull(IrctcRules.toIndianE164("12345"))
    }
}
