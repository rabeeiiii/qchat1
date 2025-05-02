package com.example.qchat.model

import com.example.qchat.utils.Constant
import java.util.*

data class GroupMessage(
    val id: String = "",
    val groupId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val messageType: String = Constant.MESSAGE_TYPE_TEXT,
    val timestamp: Date = Date(),
    val attachments: List<String> = listOf(),
    val mentions: List<String> = listOf(),
    val replyTo: String? = null,
    val edited: Boolean = false,
    val editedAt: Date? = null,
    val thumbnailUrl: String? = null,
    val videoDuration: String? = null,
    val documentName: String? = null,
) 