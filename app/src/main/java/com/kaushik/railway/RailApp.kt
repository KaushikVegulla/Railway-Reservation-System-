package com.kaushik.railway

import android.app.Application

class RailApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: RailApp
            private set
    }
}
