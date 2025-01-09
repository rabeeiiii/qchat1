package com.example.qchat.model

data class ChatMessage(
    val text: String,
    val timestamp: String,
    val isSent: Boolean
)