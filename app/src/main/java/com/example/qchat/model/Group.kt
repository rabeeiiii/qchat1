package com.example.qchat.model

import java.io.Serializable

data class Group(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val image: String? = null,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val members: List<String> = listOf(),
    val admins: List<String> = listOf(),
    val lastMessage: String? = null,
    val lastMessageTime: Long = 0,
    val lastMessageSender: String? = null
) : Serializable 