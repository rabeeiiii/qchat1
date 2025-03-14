package com.example.qchat.utils

object Constant {

    const val KEY_LAST_ENCRYPTED_AES_KEY = "last encrypted_aes_key"
    const val KEY_LAST_ENCRYPTED_MESSAGE = "last encrypted_message"
    const val KEY_COLLECTION_USERS = "users"
    const val KEY_NAME = "name"
    const val KEY_EMAIL = "email"
    const val KEY_PASSWORD = "password"
    const val KEY_PREFERENCE_NAME = "chatAppPreference"
    const val KEY_IS_SIGNED_IN = "isSignedIn"
    const val KEY_USER_ID = "userId"
    const val KEY_IMAGE = "image"
    const val KEY_FCM_TOKEN = "fcmToken"
    const val KEY_USER = "user"
    const val VIEW_TYPE_SEND = 1
    const val VIEW_TYPE_RECEIVED = 2
    const val VIEW_TYPE_SEND_PHOTO = 3 // For sending photo messages
    const val VIEW_TYPE_RECEIVED_PHOTO = 4 // For receiving photo messages
    const val VIEW_TYPE_SEND_LOCATION = 5
    const val VIEW_TYPE_RECEIVED_LOCATION = 6
    const val VIEW_TYPE_SEND_VIDEO = 7
    const val VIEW_TYPE_RECEIVED_VIDEO = 8

    const val KEY_COLLECTION_CHAT = "chat"
    const val KEY_SENDER_ID = "senderId"
    const val KEY_RECEIVER_ID = "receiverId"
    const val KEY_MESSAGE = "message"
    const val KEY_MESSAGE_TYPE = "messageType"
    const val MESSAGE_TYPE_TEXT = "text" // Explicitly define text type
    const val MESSAGE_TYPE_PHOTO = "photo" // Explicitly define photo type
    const val MESSAGE_TYPE_LOCATION = "location"
    const val KEY_TIMESTAMP = "timestamp"
    const val KEY_COLLECTION_CONVERSATIONS = "conversations"
    const val KEY_SENDER_NAME = "senderName"
    const val KEY_RECEIVER_NAME = "receiverName"
    const val KEY_SENDER_IMAGE = "senderImage"
    const val KEY_RECEIVER_IMAGE = "receiverImage"
    const val KEY_LAST_MESSAGE = "lastMessage"
    const val KEY_AVAILABILITY = "availability"
    const val FCM_BASE_URL = "https://fcm.googleapis.com/fcm/"
    const val REMOTE_MSG_AUTHORIZATION = "Authorization"
    const val REMOTE_MSG_CONTENT_TYPE = "Content-Type"
    const val REMOTE_MSG_DATA = "data"
    const val REMOTE_MSG_REGISTRATION_IDS = "registration_ids"
    const val ACTION_SHOW_CHAT_FRAGMENT = "ACTION_SHOW_CHAT_FRAGMENT"
    const val NOTIFICATION_CHANNEL_ID = "message_channel"
    const val NOTIFICATION_CHANNEL_NAME = "message_channel"
    const val KEY_SECRET_KEY = "secret_key"
    const val MESSAGE_TYPE_VIDEO = "video"
    const val KEY_VIDEO_URL = "videoUrl"
    const val KEY_THUMBNAIL_URL = "thumbnailUrl"

    // 🔹 WebRTC Call Constants
    const val KEY_COLLECTION_CALLS = "calls"
    const val KEY_CALL_ID = "callId"
    const val KEY_CALLER_ID = "callerId"
    const val KEY_CALL_TYPE = "callType"
    const val CALL_TYPE_AUDIO = "audio"
    const val CALL_TYPE_VIDEO = "video"

    const val KEY_CALLER_IMAGE = "CallerImage"
    const val KEY_CALLER_NAME = "CallerName"
    const val KEY_REMOTE_USER = "key_remote_user"


    // 🔹 Call Status Constants
    const val KEY_CALL_STATUS = "callStatus"
    const val CALL_STATUS_RINGING = "ringing"
    const val CALL_STATUS_ONGOING = "ongoing"
    const val CALL_STATUS_ENDED = "ended"

    // 🔹 Call Signaling Data
    const val KEY_SIGNAL_TYPE = "signalType"
    const val SIGNAL_OFFER = "offer"
    const val SIGNAL_ANSWER = "answer"
    const val SIGNAL_CANDIDATE = "candidate"


    const val KEY_SIGNATURE = "signature"
    const val KEY_PUBLIC_KEY = "publicKey"

    const val KEY_ENCRYPTED_MESSAGE = "encrypted_message"
    const val KEY_ENCRYPTED_AES_KEY = "encrypted_aes_key"


}
