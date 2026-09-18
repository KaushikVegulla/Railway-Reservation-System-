package com.kaushik.railway

import android.app.Application
import com.razorpay.Checkout

class RailApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        runCatching { Checkout.preload(applicationContext) }
    }

    companion object {
        lateinit var instance: RailApp
            private set
    }
}
