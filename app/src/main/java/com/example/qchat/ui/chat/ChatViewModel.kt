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
import javax.crypto.SecretKey
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
            keyPair.copyOfRange(CryptoUtils.CRYPTO_PUBLICKEYBYTES, keyPair.size),
            Base64.NO_WRAP
        )

        Log.d("ChatViewModel", "Generated Public Key: $publicKey")
        return Pair(publicKey, secretKey)
    }

    private fun signMessage(privateKey: ByteArray, message: ByteArray): String {
        return try {
            val signature = CryptoUtils.signMessage(privateKey, message)
            val signatureBase64 = Base64.encodeToString(signature, Base64.NO_WRAP)
            Log.d("ChatViewModel", "Generated Signature: $signatureBase64")
            signatureBase64
        } catch (e: Exception) {
            Log.e("CryptoUtils", "Error signing message: ${e.message}")
            ""
        }
    }

    private fun verifyMessage(publicKey: ByteArray, message: String, signatureBase64: String): Boolean {
        return try {
            val signature = Base64.decode(signatureBase64, Base64.NO_WRAP)
            CryptoUtils.verifySignature(publicKey, message.toByteArray(Charsets.UTF_8), signature)
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Signature verification failed: ${e.message}")
            false
        }
    }


    private fun encryptMessage(message: String, secretKey: SecretKey): Pair<String, String> {
        val encryptedMessage = AesUtils.encryptMessage(message, secretKey)
        val keyBase64 = AesUtils.keyToBase64(secretKey)
        return Pair(encryptedMessage, keyBase64)
    }

    private fun decryptMessage(encryptedMessage: String, aesKeyBase64: String): String {
        val secretKey = AesUtils.base64ToKey(aesKeyBase64)
        return AesUtils.decryptMessage(encryptedMessage, secretKey)
    }

    fun sendMessage(message: String, receiverUser: User) {
        viewModelScope.launch {
            val senderId = pref.getString(Constant.KEY_USER_ID, null).orEmpty()
            if (senderId.isEmpty()) return@launch

            val keyPair = CryptoUtils.generateDilithiumKeyPair()
            val publicKey = keyPair.sliceArray(0 until CryptoUtils.CRYPTO_PUBLICKEYBYTES)
            val privateKey = keyPair.sliceArray(CryptoUtils.CRYPTO_PUBLICKEYBYTES until keyPair.size)

            val signatureBase64 = signMessage(privateKey, message.toByteArray())
            if (signatureBase64.isEmpty()) return@launch

            val signedMessage = "$message||$signatureBase64"

            val aesKey = AesUtils.generateAESKey()
            val (encryptedMessage, aesKeyBase64) = encryptMessage(signedMessage, aesKey)

            val messageMap = hashMapOf(
                Constant.KEY_SENDER_ID to senderId,
                Constant.KEY_RECEIVER_ID to receiverUser.id,
                Constant.KEY_ENCRYPTED_MESSAGE to encryptedMessage,
                Constant.KEY_ENCRYPTED_AES_KEY to aesKeyBase64,
                Constant.KEY_SIGNATURE to signatureBase64,
                Constant.KEY_PUBLIC_KEY to Base64.encodeToString(publicKey, Base64.NO_WRAP),
                Constant.KEY_MESSAGE_TYPE to Constant.MESSAGE_TYPE_TEXT,
                Constant.KEY_TIMESTAMP to FieldValue.serverTimestamp()
            )

            repository.sendMessage(messageMap)
                .addOnSuccessListener {
                    Log.d("ChatViewModel", "Message sent successfully")

                    updateRecentConversation(
                        senderId = senderId,
                        receiverUser = receiverUser,
                        lastMessage = message,
                        messageType = Constant.MESSAGE_TYPE_TEXT
                    )
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
                repository.updateRecentConversation(
                    senderId,
                    receiverUser.id,
                    conversation
                ) { id ->
                    conversionId = id
                    Log.d("ChatViewModel", "Conversation updated/created with ID: $conversionId")
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error updating/creating conversation: ${e.message}")
            }
        }
    }

    fun sendPhoto(encodedImage: String, receiverUser: User) {
        viewModelScope.launch {
            val senderId = pref.getString(Constant.KEY_USER_ID, null).orEmpty()
            if (senderId.isEmpty()) return@launch

            val aesKey = AesUtils.generateAESKey()
            val (encryptedImage, aesKeyBase64) = encryptMessage(encodedImage, aesKey)

            val messageMap = hashMapOf(
                Constant.KEY_SENDER_ID to senderId,
                Constant.KEY_RECEIVER_ID to receiverUser.id,
                Constant.KEY_ENCRYPTED_MESSAGE to encryptedImage,
                Constant.KEY_ENCRYPTED_AES_KEY to aesKeyBase64,
                Constant.KEY_MESSAGE_TYPE to Constant.MESSAGE_TYPE_PHOTO,
                Constant.KEY_TIMESTAMP to FieldValue.serverTimestamp()
            )

            repository.sendMessage(messageMap)
                .addOnSuccessListener {
                    Log.d("ChatViewModel", "Photo sent successfully")

                    updateRecentConversation(
                        senderId = senderId,
                        receiverUser = receiverUser,
                        lastMessage = "[Photo Sent]",
                        messageType = Constant.MESSAGE_TYPE_PHOTO
                    )
                }
                .addOnFailureListener { e ->
                    Log.e("ChatViewModel", "Failed to send photo: ${e.message}")
                }
        }
    }
    fun sendLocation(latitude: Double, longitude: Double, receiverUser: User) {
        viewModelScope.launch {
            val senderId = pref.getString(Constant.KEY_USER_ID, null).orEmpty()
            if (senderId.isEmpty()) return@launch

            // We'll just encrypt location with AES (no signature)
            val locationString = "$latitude,$longitude"
            val aesKey = AesUtils.generateAESKey()
            val (encryptedLocation, aesKeyBase64) = encryptMessage(locationString, aesKey)

            val messageMap = hashMapOf(
                Constant.KEY_SENDER_ID to senderId,
                Constant.KEY_RECEIVER_ID to receiverUser.id,
                Constant.KEY_ENCRYPTED_MESSAGE to encryptedLocation,
                Constant.KEY_ENCRYPTED_AES_KEY to aesKeyBase64,
                Constant.KEY_MESSAGE_TYPE to Constant.MESSAGE_TYPE_LOCATION,
                Constant.KEY_TIMESTAMP to FieldValue.serverTimestamp()
            )

            repository.sendMessage(messageMap)
                .addOnSuccessListener {
                    Log.d("ChatViewModel", "Location sent successfully")

                    // Update conversation with "[Location]"
                    updateRecentConversation(
                        senderId = senderId,
                        receiverUser = receiverUser,
                        lastMessage = "[Location]",
                        messageType = Constant.MESSAGE_TYPE_LOCATION
                    )
                }
                .addOnFailureListener { e ->
                    Log.e("ChatViewModel", "Failed to send location: ${e.message}")
                }
        }
    }



    fun eventListener(receiverId: String, chatObserver: (List<ChatMessage>) -> Unit) {
        repository.observeChat(pref.getString(Constant.KEY_USER_ID, null).orEmpty(), receiverId) { querySnapshot, error ->
            if (error != null) return@observeChat

            val newMessages = querySnapshot?.documents?.mapNotNull { document ->
                val senderId = document.getString(Constant.KEY_SENDER_ID) ?: return@mapNotNull null
                val encryptedMessage = document.getString(Constant.KEY_ENCRYPTED_MESSAGE) ?: return@mapNotNull null
                val encryptedAesKey = document.getString(Constant.KEY_ENCRYPTED_AES_KEY) ?: return@mapNotNull null
                val senderPublicKeyBase64 = document.getString(Constant.KEY_PUBLIC_KEY) ?: return@mapNotNull null

                val decryptedMessage = decryptMessage(encryptedMessage, encryptedAesKey)
                val parts = decryptedMessage.split("||")
                if (parts.size != 2) return@mapNotNull null

                val originalMessage = parts[0]
                val signature = parts[1]

                val senderPublicKey = Base64.decode(senderPublicKeyBase64, Base64.NO_WRAP)
                val isVerified = verifyMessage(senderPublicKey, originalMessage, signature)

                if (!isVerified) {
                    Log.e("ChatViewModel", "Signature verification failed for message: $originalMessage")
                    return@mapNotNull null
                }

                ChatMessage(
                    senderId = senderId,
                    receiverId = receiverId,
                    message = originalMessage,
                    dateTime = document.getDate(Constant.KEY_TIMESTAMP)?.getReadableDate() ?: "",
                    date = document.getDate(Constant.KEY_TIMESTAMP) ?: Date(),
                    messageType = Constant.MESSAGE_TYPE_TEXT
                )
            }.orEmpty()

            chatObserver(newMessages)
        }
    }

    fun checkForConversation(receiverUserId: String) = viewModelScope.launch {
        val senderId = pref.getString(Constant.KEY_USER_ID, null).orEmpty()
        if (senderId.isEmpty()) {
            Log.e("ChatViewModel", "Sender ID is missing.")
            return@launch
        }

        try {
            conversionId = repository.checkForConversion(senderId, receiverUserId)
                ?: repository.checkForConversion(receiverUserId, senderId)
                        ?: ""

            if (conversionId.isNotEmpty()) {
                Log.d("ChatViewModel", "Existing conversation found with ID: $conversionId")
            } else {
                Log.d("ChatViewModel", "No existing conversation found.")
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error checking for conversation: ${e.message}")
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

            try {
                repository.updateRecentConversation(
                    senderId = senderId,
                    receiverId = receiverUser.id,
                    message = conversation
                ) { id ->
                    conversionId = id
                    Log.d("ChatViewModel", "Conversation created/updated with ID: $conversionId")
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error creating/updating conversation: ${e.message}")
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
        val currentUserId = pref.getString(Constant.KEY_USER_ID, null).orEmpty()

        repository.observeChat(currentUserId, receiverId) { querySnapshot, error ->
            if (error != null) {
                Log.e("ChatViewModel", "Error observing chat: ${error.message}")
                return@observeChat
            }

            if (querySnapshot == null || querySnapshot.isEmpty) return@observeChat

            val newMessages = querySnapshot.documents.mapNotNull { document ->
                val messageId = document.id

                // Skip if we already processed this message (avoid duplicates)
                if (processedMessageIds.contains(messageId)) return@mapNotNull null
                processedMessageIds.add(messageId)

                // Fetch required fields
                val senderId = document.getString(Constant.KEY_SENDER_ID) ?: return@mapNotNull null
                val receiverUserId = document.getString(Constant.KEY_RECEIVER_ID) ?: return@mapNotNull null
                val encryptedMessage = document.getString(Constant.KEY_ENCRYPTED_MESSAGE) ?: return@mapNotNull null
                val encryptedAesKey = document.getString(Constant.KEY_ENCRYPTED_AES_KEY) ?: return@mapNotNull null
                val senderPublicKeyBase64 = document.getString(Constant.KEY_PUBLIC_KEY) ?: ""
                val messageType = document.getString(Constant.KEY_MESSAGE_TYPE) ?: Constant.MESSAGE_TYPE_TEXT

                Log.d("ChatViewModel", "Processing message from sender: $senderId, Type: $messageType")

                // Decrypt with AES
                val aesKey = try {
                    AesUtils.base64ToKey(encryptedAesKey)
                } catch (e: IllegalArgumentException) {
                    Log.e("ChatViewModel", "Invalid AES Key Base64 format: ${e.message}")
                    return@mapNotNull null
                }

                val decryptedMessage = AesUtils.decryptMessage(encryptedMessage, aesKey)
                if (decryptedMessage.isEmpty()) {
                    Log.e("ChatViewModel", "AES Decryption Failed for message from sender: $senderId")
                    return@mapNotNull null
                }

                Log.d("ChatViewModel", "Decrypted Message (first 100 chars): ${decryptedMessage.take(100)}")

                // 1) For PHOTO or LOCATION messages -> skip signature checks
                if (messageType == Constant.MESSAGE_TYPE_PHOTO ||
                    messageType == Constant.MESSAGE_TYPE_LOCATION
                ) {
                    return@mapNotNull ChatMessage(
                        senderId = senderId,
                        receiverId = receiverUserId,
                        message = decryptedMessage,
                        dateTime = document.getDate(Constant.KEY_TIMESTAMP)?.getReadableDate() ?: "",
                        date = document.getDate(Constant.KEY_TIMESTAMP) ?: Date(),
                        messageType = messageType
                    )
                }

                // 2) For TEXT messages -> do signature verification
                val parts = decryptedMessage.split("||")
                if (parts.size != 2) {
                    Log.e("ChatViewModel", "Invalid decrypted message format. Expected 'message||signature'")
                    return@mapNotNull null
                }

                val originalMessage = parts[0].trim()
                val signatureBase64 = parts[1].trim()

                // Decode signature and public key
                val signatureDecoded = try {
                    Base64.decode(signatureBase64, Base64.NO_WRAP)
                } catch (e: IllegalArgumentException) {
                    Log.e("ChatViewModel", "Invalid Signature Base64 format: ${e.message}")
                    return@mapNotNull null
                }

                val senderPublicKey = try {
                    Base64.decode(senderPublicKeyBase64, Base64.NO_WRAP)
                } catch (e: IllegalArgumentException) {
                    Log.e("ChatViewModel", "Invalid Public Key Base64 format: ${e.message}")
                    return@mapNotNull null
                }

                // Verify the signature
                val isVerified = try {
                    CryptoUtils.verifySignature(
                        senderPublicKey,
                        originalMessage.toByteArray(Charsets.UTF_8),
                        signatureDecoded
                    )
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Signature verification failed (exception): ${e.message}")
                    false
                }

                if (!isVerified) {
                    Log.e("ChatViewModel", "Signature verification failed! Message might be tampered.")
                    return@mapNotNull null
                }

                Log.d("ChatViewModel", "Signature verified successfully!")

                // Build the ChatMessage for verified text
                ChatMessage(
                    senderId = senderId,
                    receiverId = receiverUserId,
                    message = originalMessage,
                    dateTime = document.getDate(Constant.KEY_TIMESTAMP)?.getReadableDate() ?: "",
                    date = document.getDate(Constant.KEY_TIMESTAMP) ?: Date(),
                    messageType = messageType
                )
            }

            if (newMessages.isNotEmpty()) {
                // If you still want to remove any duplicates by text, you can do:
                // chatObserver(newMessages.distinctBy { it.message })
                // Otherwise, just pass them through:
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