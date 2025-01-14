package com.example.qchat

import android.app.Application
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ChatApplication:Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this) // Initialize Firebase
    }

}