package com.example.qchat.ui.chat

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import com.example.qchat.model.ChatMessage
import com.example.qchat.model.Data
import com.example.qchat.model.MessageBody
import com.example.qchat.model.User
import com.example.qchat.repository.MainRepository
import com.example.qchat.utils.AesUtils
import com.example.qchat.utils.Constant
import com.example.qchat.utils.getReadableDate
import com.google.android.gms.tasks.OnCompleteListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: MainRepository,
    private val pref: SharedPreferences,

) : ViewModel() {

    var conversionId = ""
    private var isReceiverAvailable = false
    private val processedMessageIds = mutableSetOf<String>()

    fun sendMessage(message: String, receiverUser: User, messageType: String = Constant.MESSAGE_TYPE_TEXT) {
        viewModelScope.launch {
            val senderId = pref.getString(Constant.KEY_USER_ID, null).orEmpty()
            if (senderId.isEmpty()) {
                Log.e("ChatViewModel", "Sender ID is missing.")
                return@launch
            }

            val messageMap = hashMapOf(
                Constant.KEY_SENDER_ID to senderId,
                Constant.KEY_RECEIVER_ID to receiverUser.id,
                Constant.KEY_MESSAGE to message,
                Constant.KEY_MESSAGE_TYPE to messageType,
                Constant.KEY_TIMESTAMP to FieldValue.serverTimestamp()
            )

            val result = repository.sendMessage(messageMap)

            if (result) {
                Log.d("ChatViewModel", "Message sent successfully.")

                if (conversionId.isNotEmpty()) {
                    repository.updateConversation(message, conversionId)
                } else {
                    val conversation = HashMap<String, Any>().apply {
                        put(Constant.KEY_SENDER_ID, senderId)
                        put(Constant.KEY_SENDER_NAME, pref.getString(Constant.KEY_NAME, null).toString())
                        put(Constant.KEY_SENDER_IMAGE, pref.getString(Constant.KEY_IMAGE, null).toString())
                        put(Constant.KEY_RECEIVER_ID, receiverUser.id)
                        put(Constant.KEY_RECEIVER_NAME, receiverUser.name)
                        put(Constant.KEY_RECEIVER_IMAGE, receiverUser.image!!)
                        put(Constant.KEY_LAST_MESSAGE, message)
                        put(Constant.KEY_TIMESTAMP, Date())
                    }
                    repository.updateRecentConversation(conversation) {
                        conversionId = it
                    }
                }

                if (!isReceiverAvailable) {
                    try {
                        val messageBody = MessageBody(
                            data = Data(
                                userId = senderId,
                                name = pref.getString(Constant.KEY_NAME, null).toString(),
                                fcmToken = pref.getString(Constant.KEY_FCM_TOKEN, null).toString(),
                                message = message
                            ),
                            regIs = listOf(receiverUser.token!!)
                        )
                        sendNotification(messageBody)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                Log.e("ChatViewModel", "Failed to send message.")
            }
        }
    }

    fun sendPhoto(encodedImage: String, receiverUser: User) {
        viewModelScope.launch {
            val messageMap = HashMap<String, Any>()
            messageMap[Constant.KEY_SENDER_ID] = pref.getString(Constant.KEY_USER_ID, null).toString()
            messageMap[Constant.KEY_RECEIVER_ID] = receiverUser.id
            messageMap[Constant.KEY_MESSAGE] = encodedImage
            messageMap[Constant.KEY_MESSAGE_TYPE] = Constant.MESSAGE_TYPE_PHOTO
            messageMap[Constant.KEY_TIMESTAMP] = Date()

            repository.sendMessage(messageMap)

            if (conversionId.isNotEmpty()) {
                repository.updateConversation(encodedImage, conversionId)
            } else {
                val conversation = HashMap<String, Any>().apply {
                    put(Constant.KEY_SENDER_ID, pref.getString(Constant.KEY_USER_ID, null).toString())
                    put(Constant.KEY_SENDER_NAME, pref.getString(Constant.KEY_NAME, null).toString())
                    put(Constant.KEY_SENDER_IMAGE, pref.getString(Constant.KEY_IMAGE, null).toString())
                    put(Constant.KEY_RECEIVER_ID, receiverUser.id)
                    put(Constant.KEY_RECEIVER_NAME, receiverUser.name)
                    put(Constant.KEY_RECEIVER_IMAGE, receiverUser.image!!)
                    put(Constant.KEY_LAST_MESSAGE, encodedImage)
                    put(Constant.KEY_TIMESTAMP, Date())
                }
                repository.updateRecentConversation(conversation) {
                    conversionId = it
                }
            }

            if (!isReceiverAvailable) {
                try {
                    val messageBody = MessageBody(
                        data = Data(
                            userId = pref.getString(Constant.KEY_USER_ID, null).toString(),
                            name = pref.getString(Constant.KEY_NAME, null).toString(),
                            fcmToken = pref.getString(Constant.KEY_FCM_TOKEN, null).toString(),
                            message = encodedImage
                        ),
                        regIs = listOf(receiverUser.token!!)
                    )
                    sendNotification(messageBody)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun eventListener(receiverId: String, chatObserver: ChatFragment.ChatObserver) {
        val newMessageList = mutableListOf<ChatMessage>()
        val eventListener = EventListener<QuerySnapshot> { value, error ->
            if (error != null) {
                Log.e("ChatViewModel", "Firestore error: ${error.message}")
                return@EventListener
            }

            if (value != null) {
                Log.d("ChatViewModel", "Firestore document count: ${value.documents.size}")
                for (document in value.documents) {
                    val senderId = document.getString(Constant.KEY_SENDER_ID)
                    val receiverUserId = document.getString(Constant.KEY_RECEIVER_ID)
                    val message = document.getString(Constant.KEY_MESSAGE)
                    val timestamp = document.getDate(Constant.KEY_TIMESTAMP)
                    val messageType = document.getString(Constant.KEY_MESSAGE_TYPE) ?: Constant.MESSAGE_TYPE_TEXT

                    if (senderId != null && receiverUserId != null && message != null && timestamp != null) {
                        val chatMessage = ChatMessage(
                            senderId = senderId,
                            receiverId = receiverUserId,
                            message = message,
                            dateTime = timestamp.getReadableDate(),
                            date = timestamp,
                            messageType = messageType
                        )
                        Log.d("ChatViewModel", "Message added: $chatMessage")
                        newMessageList.add(chatMessage)
                    } else {
                        Log.e("ChatViewModel", "Missing required fields in document: ${document.id}")
                    }
                }
                chatObserver.observeChat(newMessageList)
            } else {
                Log.e("ChatViewModel", "No documents found in Firestore.")
            }
        }

        repository.observeChat(
            pref.getString(Constant.KEY_USER_ID, null).orEmpty(),
            receiverId,
            eventListener
        )
    }

    fun checkForConversation(receiverUserId: String) = viewModelScope.launch {
        repository.checkForConversionRemotely(
            pref.getString(Constant.KEY_USER_ID, null).toString(),
            receiverUserId,
            conversionOnCompleteListener()
        )
        repository.checkForConversionRemotely(
            receiverUserId,
            pref.getString(Constant.KEY_USER_ID, null).toString(),
            conversionOnCompleteListener()
        )
    }
    private fun createConversation(message: String, receiverUser: User) {
        viewModelScope.launch {
            val senderId = pref.getString(Constant.KEY_USER_ID, null).orEmpty()
            if (senderId.isEmpty()) {
                Log.e("ChatViewModel", "Sender ID is missing.")
                return@launch
            }

            val conversation = hashMapOf(
                Constant.KEY_SENDER_ID to senderId,
                Constant.KEY_RECEIVER_ID to receiverUser.id,
                Constant.KEY_LAST_MESSAGE to message,
                Constant.KEY_TIMESTAMP to FieldValue.serverTimestamp(),
                Constant.KEY_SENDER_NAME to pref.getString(Constant.KEY_NAME, null).orEmpty(),
                Constant.KEY_SENDER_IMAGE to pref.getString(Constant.KEY_IMAGE, null).orEmpty(),
                Constant.KEY_RECEIVER_NAME to receiverUser.name,
                Constant.KEY_RECEIVER_IMAGE to receiverUser.image.orEmpty()
            )

            repository.updateRecentConversation(conversation) { id ->
                conversionId = id
                Log.d("ChatViewModel", "Conversation created with ID: $conversionId")
            }
        }
    }

    private fun sendNotificationToReceiver(message: String, receiverUser: User) {
        val messageBody = MessageBody(
            data = Data(
                userId = pref.getString(Constant.KEY_USER_ID, null).orEmpty(),
                name = pref.getString(Constant.KEY_NAME, null).orEmpty(),
                fcmToken = receiverUser.token.orEmpty(),
                message = message
            ),
            regIs = listOf(receiverUser.token.orEmpty())
        )

        viewModelScope.launch {
            val response = repository.sendNotification(messageBody)
            if (response.isSuccessful) {
                Log.d("ChatViewModel", "Notification sent successfully.")
            } else {
                Log.e("ChatViewModel", "Failed to send notification.")
            }
        }
    }


    private fun conversionOnCompleteListener() = OnCompleteListener<QuerySnapshot> { task ->
        if (task.isSuccessful && task.result != null && task.result?.documents?.size!! > 0) {
            val documentSnapshot = task.result?.documents?.get(0)
            conversionId = documentSnapshot?.id.toString()
        }
    }

    fun listenerAvailabilityOfReceiver(receiverId: String, availability: (Boolean, String, String) -> Unit) {
        repository.listenerAvailabilityOfReceiver(receiverId) { value, error ->
            var fcm = ""
            var profileImage = ""
            if (error != null) return@listenerAvailabilityOfReceiver
            if (value != null) {
                if (value.getLong(Constant.KEY_AVAILABILITY) != null) {
                    isReceiverAvailable = value.getLong(Constant.KEY_AVAILABILITY)?.toInt() == 1
                }
                fcm = value.getString(Constant.KEY_FCM_TOKEN).toString()
                profileImage = value.getString(Constant.KEY_IMAGE).toString()
            }
            availability(isReceiverAvailable, fcm, profileImage)
        }
    }
    fun observeChat(receiverId: String, chatObserver: (List<ChatMessage>) -> Unit) {
        val senderId = pref.getString(Constant.KEY_USER_ID, null).orEmpty()

        if (senderId.isEmpty()) {
            Log.e("ChatViewModel", "Sender ID is missing.")
            return
        }

        repository.observeChat(senderId, receiverId) { querySnapshot, error ->
            if (error != null) {
                Log.e("ChatViewModel", "Error observing chat: ${error.message}")
                return@observeChat
            }

            val newMessages = querySnapshot?.documents?.mapNotNull { document ->
                val messageId = document.id
                if (!processedMessageIds.contains(messageId)) {
                    processedMessageIds.add(messageId)

                    try {
                        val encryptedMessage = document.getString(Constant.KEY_MESSAGE)
                        val secretKeyBase64 = document.getString(Constant.KEY_SECRET_KEY)

                        if (!encryptedMessage.isNullOrEmpty() && !secretKeyBase64.isNullOrEmpty()) {
                            val secretKey = AesUtils.base64ToKey(secretKeyBase64)
                            val decryptedMessage = AesUtils.decryptMessage(encryptedMessage, secretKey)

                            Log.d("ChatViewModel", "Decryption successful: $decryptedMessage")

                            ChatMessage(
                                senderId = document.getString(Constant.KEY_SENDER_ID) ?: "",
                                receiverId = document.getString(Constant.KEY_RECEIVER_ID) ?: "",
                                message = decryptedMessage,
                                dateTime = document.getDate(Constant.KEY_TIMESTAMP)?.getReadableDate() ?: "",
                                date = document.getDate(Constant.KEY_TIMESTAMP) ?: Date(),
                                messageType = document.getString(Constant.KEY_MESSAGE_TYPE) ?: Constant.MESSAGE_TYPE_TEXT
                            )
                        } else null
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Failed to decrypt message: ${e.message}")
                        null
                    }
                } else null
            }.orEmpty()

            if (newMessages.isNotEmpty()) {
                Log.d("ChatViewModel", "New chat messages received: ${newMessages.size}")
                chatObserver(newMessages)
            }
        }
    }

    fun sendNotification(messageBody: MessageBody) = viewModelScope.launch {
        val response = repository.sendNotification(messageBody)
        if (response.isSuccessful) {
        }
    }
}
