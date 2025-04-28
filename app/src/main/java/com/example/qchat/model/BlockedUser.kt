package com.example.qchat.model

data class BlockedUser(
    val blockedUserId: String = "",
    val blockedUserName: String = "",
    val blockedAt: Long = 0
) 