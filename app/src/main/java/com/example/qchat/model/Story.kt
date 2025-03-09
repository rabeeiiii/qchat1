package com.example.qchat.model

import com.google.firebase.Timestamp
import java.io.Serializable

data class Story(
    val userId: String = "",
    val userName: String = "",
    val userProfilePicture: String = "",
    val photo: String = "",
    val timestamp: Timestamp = Timestamp.now()  // Firebase Timestamp
)
