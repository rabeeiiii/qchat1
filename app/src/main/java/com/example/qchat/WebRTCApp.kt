package com.example.qchat

import android.app.Application

import android.util.Log

class WebRTCApp : Application() {

    override fun onCreate() {
        super.onCreate()

        Log.d("WebRTCApp", "Logging initialized")
    }
}