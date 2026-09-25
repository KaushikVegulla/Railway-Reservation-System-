package com.kaushik.railway.nav

/**
 * Where a signed-in traveller goes next.
 * Profile, then MPIN, then home. Biometric unlock sits in front of those on a cold start.
 */
object AccountGate {
    fun destination(
        loggedIn: Boolean,
        profileComplete: Boolean,
        mpinSet: Boolean,
        mpinDeferred: Boolean,
        needsUnlock: Boolean
    ): String = when {
        !loggedIn -> Routes.Login
        needsUnlock -> Routes.Unlock
        !profileComplete -> Routes.Activate
        !mpinSet && !mpinDeferred -> Routes.Mpin
        else -> Routes.Home
    }
}
