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
import com.example.qchat.utils.CryptoUtils
import com.example.qchat.utils.getReadableDate
import com.google.android.gms.tasks.OnCompleteListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import android.util.Base64
import com.example.qchat.utils.CryptoUtils.Companion.CRYPTO_PUBLICKEYBYTES
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


    fun generateKeyPair(): Pair<String, String> {
        val keyPair = CryptoUtils.generateDilithiumKeyPair()
        val publicKey = Base64.encodeToString(
            keyPair.copyOfRange(0, CryptoUtils.CRYPTO_PUBLICKEYBYTES),
            Base64.NO_WRAP
        )
        val secretKey = Base64.encodeToString(
            keyPair.copyOfRange(
                CryptoUtils.CRYPTO_PUBLICKEYBYTES,
                keyPair.size
            ), Base64.NO_WRAP
        )

        Log.d("ChatViewModel", "Generated Public Key: $publicKey")
        Log.d("ChatViewModel", "Generated Secret Key: $secretKey")

        return Pair(publicKey, secretKey)
    }

    fun signMessage(privateKey: ByteArray, message: ByteArray): ByteArray {
        if (privateKey.size != CryptoUtils.CRYPTO_SECRETKEYBYTES) {
            Log.e("CryptoUtils", "Private Key Size Mismatch! Expected: ${CryptoUtils.CRYPTO_SECRETKEYBYTES}, Got: ${privateKey.size}")
            return ByteArray(0)
        }

        return try {
            val signature = CryptoUtils.signMessage(privateKey, message) ?: ByteArray(0)
            Log.d("ChatViewModel", "Generated Signature: ${Base64.encodeToString(signature, Base64.NO_WRAP)}")
            signature
        } catch (e: Exception) {
            Log.e("CryptoUtils", "Error signing message: ${e.message}")
            ByteArray(0)
        }
    }

    fun verifyReceivedPhoto(message: ChatMessage): Boolean {
        return try {
            val publicKeyDecoded = Base64.decode(message.publicKey, Base64.NO_WRAP)
            val signatureDecoded = Base64.decode(message.signature, Base64.NO_WRAP)
            val imageBytes = message.message.toByteArray()

            CryptoUtils.verifySignature(publicKeyDecoded, imageBytes, signatureDecoded)
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error verifying photo signature: ${e.message}")
            false
        }
    }


    fun sendMessage(message: String, receiverUser: User) {
        viewModelScope.launch {
            val senderId = pref.getString(Constant.KEY_USER_ID, null).orEmpty()
            if (senderId.isEmpty()) return@launch

            // Generate Key Pair
            val keyPair = CryptoUtils.generateDilithiumKeyPair()
            if (keyPair == null || keyPair.size < CryptoUtils.CRYPTO_PUBLICKEYBYTES + CryptoUtils.CRYPTO_SECRETKEYBYTES) {
                Log.e("ChatViewModel", "Key pair generation failed or invalid size.")
                return@launch
            }

            val publicKey = keyPair.sliceArray(0 until CryptoUtils.CRYPTO_PUBLICKEYBYTES)
            val privateKey = keyPair.sliceArray(CryptoUtils.CRYPTO_PUBLICKEYBYTES until keyPair.size)

            if (privateKey.isEmpty() || publicKey.isEmpty() || message.isEmpty()) {
                Log.e("ChatViewModel", "Public/Private Key or Message is empty.")
                return@launch
            }

            Log.d("ChatViewModel", "Public Key: ${Base64.encodeToString(publicKey, Base64.NO_WRAP)}")
            Log.d("ChatViewModel", "Private Key: ${Base64.encodeToString(privateKey, Base64.NO_WRAP)}")

            //signature
            val signature = CryptoUtils.signMessage(privateKey, message.toByteArray())
            if (signature.isEmpty()) {
                Log.e("ChatViewModel", "Error: Signature generation failed")
                return@launch
            }

            val signatureBase64 = Base64.encodeToString(signature, Base64.NO_WRAP)
            val publicKeyBase64 = Base64.encodeToString(publicKey, Base64.NO_WRAP)

            // Log Signature
            Log.d("ChatViewModel", "Signature: $signatureBase64")

            val messageMap = hashMapOf(
                Constant.KEY_SENDER_ID to senderId,
                Constant.KEY_RECEIVER_ID to receiverUser.id,
                Constant.KEY_MESSAGE to message,
                Constant.KEY_SIGNATURE to signatureBase64,
                Constant.KEY_PUBLIC_KEY to publicKeyBase64,
                Constant.KEY_MESSAGE_TYPE to Constant.MESSAGE_TYPE_TEXT,
                Constant.KEY_TIMESTAMP to FieldValue.serverTimestamp()
            )

            repository.sendMessage(messageMap)
                .addOnSuccessListener {
                    Log.d("ChatViewModel", "Message sent to Firestore successfully")

                    viewModelScope.launch {
                        updateRecentConversation(senderId, receiverUser, message, Constant.MESSAGE_TYPE_TEXT)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ChatViewModel", "Failed to send message: ${e.message}")
                }
        }
    }

    private fun updateRecentConversation(
        senderId: String,
        receiverUser: User,
        lastMessage: String,
        messageType: String
    ) {
        viewModelScope.launch {
            val conversation = hashMapOf(
                Constant.KEY_SENDER_ID to senderId,
                Constant.KEY_RECEIVER_ID to receiverUser.id,
                Constant.KEY_LAST_MESSAGE to lastMessage,
                Constant.KEY_MESSAGE_TYPE to messageType,
                Constant.KEY_TIMESTAMP to FieldValue.serverTimestamp(),
                Constant.KEY_SENDER_NAME to pref.getString(Constant.KEY_NAME, null).orEmpty(),
                Constant.KEY_SENDER_IMAGE to pref.getString(Constant.KEY_IMAGE, null).orEmpty(),
                Constant.KEY_RECEIVER_NAME to receiverUser.name,
                Constant.KEY_RECEIVER_IMAGE to receiverUser.image.orEmpty()
            )

            try {
                repository.updateRecentConversation(conversation) { id ->
                    conversionId = id
                    Log.d("ChatViewModel", "Conversation updated with ID: $conversionId")
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error updating conversation: ${e.message}")
            }
        }
    }

    fun sendPhoto(encodedImage: String, receiverUser: User) {
        viewModelScope.launch {
            val senderId = pref.getString(Constant.KEY_USER_ID, null).orEmpty()
            if (senderId.isEmpty()) return@launch

            val keyPair = CryptoUtils.generateDilithiumKeyPair()
            if (keyPair == null || keyPair.size < CryptoUtils.CRYPTO_PUBLICKEYBYTES + CryptoUtils.CRYPTO_SECRETKEYBYTES) {
                Log.e("ChatViewModel", "Key pair generation failed or invalid size.")
                return@launch
            }

            val publicKey = keyPair.sliceArray(0 until CryptoUtils.CRYPTO_PUBLICKEYBYTES)
            val privateKey = keyPair.sliceArray(CryptoUtils.CRYPTO_PUBLICKEYBYTES until keyPair.size)

            if (privateKey.isEmpty() || publicKey.isEmpty() || encodedImage.isEmpty()) {
                Log.e("ChatViewModel", "Public/Private Key or Image is empty.")
                return@launch
            }

            val signature = CryptoUtils.signMessage(privateKey, encodedImage.toByteArray())
            if (signature.isEmpty()) {
                Log.e("ChatViewModel", "Error: Signature generation failed for image")
                return@launch
            }

            val signatureBase64 = Base64.encodeToString(signature, Base64.NO_WRAP)
            val publicKeyBase64 = Base64.encodeToString(publicKey, Base64.NO_WRAP)

            val messageMap = hashMapOf(
                Constant.KEY_SENDER_ID to senderId,
                Constant.KEY_RECEIVER_ID to receiverUser.id,
                Constant.KEY_MESSAGE to encodedImage,
                Constant.KEY_SIGNATURE to signatureBase64,
                Constant.KEY_PUBLIC_KEY to publicKeyBase64,
                Constant.KEY_MESSAGE_TYPE to Constant.MESSAGE_TYPE_PHOTO,
                Constant.KEY_TIMESTAMP to FieldValue.serverTimestamp()
            )

            repository.sendMessage(messageMap)
                .addOnSuccessListener {
                    viewModelScope.launch {
                        updateRecentConversation(senderId, receiverUser, encodedImage, Constant.MESSAGE_TYPE_PHOTO)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ChatViewModel", "Failed to send photo: ${e.message}")
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
                        newMessageList.add(chatMessage)
                    } else {
                        Log.e("ChatViewModel", "Missing required fields in document: ${document.id}")
                    }
                }
                chatObserver.observeChat(newMessageList)
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
            receiverUserId
        ) { task ->
            if (task.isSuccessful && task.result != null && task.result!!.documents.isNotEmpty()) {
                conversionId = task.result!!.documents[0].id
            }
        }

        repository.checkForConversionRemotely(
            receiverUserId,
            pref.getString(Constant.KEY_USER_ID, null).toString()
        ) { task ->
            if (task.isSuccessful && task.result != null && task.result!!.documents.isNotEmpty()) {
                conversionId = task.result!!.documents[0].id
            }
        }
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
            if (!response.isSuccessful) {
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
        repository.observeChat(pref.getString(Constant.KEY_USER_ID, null).orEmpty(), receiverId) { querySnapshot, error ->
            if (error != null) return@observeChat

            val newMessages = querySnapshot?.documents?.mapNotNull { document ->
                val senderId = document.getString(Constant.KEY_SENDER_ID) ?: return@mapNotNull null
                val receiverUserId = document.getString(Constant.KEY_RECEIVER_ID) ?: return@mapNotNull null

                // Ensure message is retrieved safely
                val message = document.get(Constant.KEY_MESSAGE)
                val messageString = when (message) {
                    is String -> message
                    else -> {
                        Log.e("ChatViewModel", "Invalid message type: ${message?.javaClass?.simpleName}")
                        return@mapNotNull null
                    }
                }

                val timestamp = document.getDate(Constant.KEY_TIMESTAMP) ?: return@mapNotNull null
                val messageType = document.getString(Constant.KEY_MESSAGE_TYPE) ?: Constant.MESSAGE_TYPE_TEXT

                ChatMessage(
                    senderId = senderId,
                    receiverId = receiverUserId,
                    message = messageString,
                    dateTime = timestamp.getReadableDate(),
                    date = timestamp,
                    messageType = messageType
                )
            }.orEmpty()

            chatObserver(newMessages)
        }
    }


    fun sendNotification(messageBody: MessageBody) = viewModelScope.launch {
        val response = repository.sendNotification(messageBody)
        if (response.isSuccessful) {
        }
    }
}