package com.example.qchat.ui.chat

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.*
import com.example.qchat.model.ChatMessage
import com.example.qchat.model.Data
import com.example.qchat.model.MessageBody
import com.example.qchat.model.User
import com.example.qchat.repository.MainRepository
import com.example.qchat.utils.Constant
import com.example.qchat.utils.getReadableDate
import com.google.android.gms.tasks.OnCompleteListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import okhttp3.EventListener
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: MainRepository,
    private val pref: SharedPreferences,
) : ViewModel() {

    var conversionId = ""
    private var isReceiverAvailable = false

    fun sendMessage(message: String, receiverUser: User, messageType: String = Constant.MESSAGE_TYPE_TEXT) {
        viewModelScope.launch {
            val messageMap = HashMap<String, Any>()
            messageMap[Constant.KEY_SENDER_ID] = pref.getString(Constant.KEY_USER_ID, null).toString()
            messageMap[Constant.KEY_RECEIVER_ID] = receiverUser.id
            messageMap[Constant.KEY_MESSAGE] = message
            messageMap[Constant.KEY_MESSAGE_TYPE] = messageType
            messageMap[Constant.KEY_TIMESTAMP] = Date()

            repository.sendMessage(messageMap)

            if (conversionId.isNotEmpty()) {
                repository.updateConversation(message, conversionId)
            } else {
                val conversation = HashMap<String, Any>().apply {
                    put(Constant.KEY_SENDER_ID, pref.getString(Constant.KEY_USER_ID, null).toString())
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
                            userId = pref.getString(Constant.KEY_USER_ID, null).toString(),
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
        }
    }

    fun sendPhoto(encodedImage: String, receiverUser: User) {
        sendMessage(encodedImage, receiverUser, messageType = Constant.MESSAGE_TYPE_PHOTO)
    }
    fun eventListener(receiverId: String, chatObserver: ChatFragment.ChatObserver) {
        val newMessageList = mutableListOf<ChatMessage>()
        val eventListener =
            com.google.firebase.firestore.EventListener<QuerySnapshot> { value, error ->
                if (error != null) {
                    Log.e("ChatViewModel", "Firestore error: ${error.message}")
                    return@EventListener
                }
                if (value != null) {
                    for (documentChange in value.documentChanges) {
                        if (documentChange.type == DocumentChange.Type.ADDED) {
                            val chatMessage = ChatMessage(
                                senderId = documentChange.document[Constant.KEY_SENDER_ID].toString(),
                                receiverId = documentChange.document[Constant.KEY_RECEIVER_ID].toString(),
                                message = documentChange.document[Constant.KEY_MESSAGE].toString(),
                                dateTime = documentChange.document.getDate(Constant.KEY_TIMESTAMP)!!
                                    .getReadableDate(),
                                date = documentChange.document.getDate(Constant.KEY_TIMESTAMP)!!,
                                messageType = documentChange.document[Constant.KEY_MESSAGE_TYPE]?.toString()
                                    ?: Constant.MESSAGE_TYPE_TEXT
                            )
                            Log.d("ChatViewModel", "MessageType: ${chatMessage.messageType}, Message: ${chatMessage.message}")

                            newMessageList.add(chatMessage)
                        }
                    }
                    chatObserver.observeChat(newMessageList)
                    newMessageList.clear()
                }
            }

        repository.observeChat(
            pref.getString(Constant.KEY_USER_ID, null).toString(),
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

    fun sendNotification(messageBody: MessageBody) = viewModelScope.launch {
        val response = repository.sendNotification(messageBody)
        if (response.isSuccessful) {
        }
    }
}
