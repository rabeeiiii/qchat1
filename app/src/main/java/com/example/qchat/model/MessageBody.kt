package com.example.qchat.model

import com.google.gson.annotations.SerializedName

data class MessageBody(
    @SerializedName("message")
    val message: Message
)

data class Message(
    @SerializedName("token")
    val token: String,
    @SerializedName("notification")
    val notification: Notification,
    @SerializedName("data")
    val data: Map<String, String>? = null
)

data class Notification(
    @SerializedName("title")
    val title: String,
    @SerializedName("body")
    val body: String
)