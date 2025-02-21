package com.example.myapplication


import android.app.Application

class MiApp : Application() {
    companion object {
        lateinit var instance: MiApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}