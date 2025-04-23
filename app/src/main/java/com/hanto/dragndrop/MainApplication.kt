package com.hanto.dragndrop

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MainApplication : Application() {

    companion object {
        private lateinit var instance: MainApplication

        fun getInstance(): MainApplication {
            return instance
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("MainApplication", "Application started!")

        instance = this

    }
}