package com.example.qchat.model

import com.example.qchat.utils.Constant
import java.util.*

data class ChatMessage(
    val id: String = "",
    val senderId:String,
    val receiverId:String,
    val thumbnailUrl: String? = null,
    val videoDuration: String? = null,
    val documentName: String? = null,
    var message:String,
    val dateTime:String,
    var date:Date,
    val conversionId:String? = null,
    val conversionName:String? = null,
    val conversionImage:String? = null,
    var unreadCount: Int = 0,
    val messageType: String = Constant.MESSAGE_TYPE_TEXT,
    val signature: String = "",
    val publicKey: String = "",
    val audioDurationInMillis: Long? = null,

    )