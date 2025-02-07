package com.example.qchat.ui.main

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QuerySnapshot
import com.example.qchat.model.ChatMessage
import com.example.qchat.repository.MainRepository
import com.example.qchat.utils.AesUtils
import com.example.qchat.utils.Constant
import com.example.qchat.utils.clearAll
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.w3c.dom.DocumentType
import java.util.*
import javax.inject.Inject
import kotlin.collections.HashMap

@HiltViewModel
class MainViewModel @Inject constructor(
    private val pref: SharedPreferences,
    private val repository: MainRepository,
) : ViewModel() {

    init {
        viewModelScope.launch {
            pref.getString(Constant.KEY_USER_ID, null)
                ?.let { repository.updateToken(repository.getToken(), it) }
        }
    }

    fun loadUserDetails() = pref.getString(Constant.KEY_IMAGE, null).toString()

    fun getName(): String = pref.getString(Constant.KEY_NAME, null).toString()

    fun signOut(): LiveData<Boolean> {
        val signOut = MutableLiveData(false)
        viewModelScope.launch {
            val userData = HashMap<String, Any>()
            userData[Constant.KEY_FCM_TOKEN] = FieldValue.delete()
            val isSignOut =
                repository.userSignOut(pref.getString(Constant.KEY_USER_ID, null)!!, userData)
            if (isSignOut) {
                pref.clearAll()
            }
            signOut.postValue(isSignOut)
        }
        return signOut
    }

    fun recentMessageEventListener(
        list: List<ChatMessage>,
        onUpdateRecentConversation: (List<ChatMessage>) -> Unit
    ) = EventListener<QuerySnapshot> { value, error ->
        if (value != null) {
            val updatedConversionList = mutableListOf<ChatMessage>()
            updatedConversionList.addAll(list)

            value.documentChanges.forEach { documentChange ->
                if (documentChange.type == DocumentChange.Type.ADDED) {
                    val senderId = documentChange.document.getString(Constant.KEY_SENDER_ID).orEmpty()
                    val receiverId = documentChange.document.getString(Constant.KEY_RECEIVER_ID).orEmpty()
                    var conversionImage = ""
                    var conversionName = ""
                    var conversionId = ""

                    if (pref.getString(Constant.KEY_USER_ID, null) == senderId) {
                        conversionImage = documentChange.document.getString(Constant.KEY_RECEIVER_IMAGE).orEmpty()
                        conversionName = documentChange.document.getString(Constant.KEY_RECEIVER_NAME).orEmpty()
                        conversionId = documentChange.document.getString(Constant.KEY_RECEIVER_ID).orEmpty()
                    } else {
                        conversionImage = documentChange.document.getString(Constant.KEY_SENDER_IMAGE).orEmpty()
                        conversionName = documentChange.document.getString(Constant.KEY_SENDER_NAME).orEmpty()
                        conversionId = documentChange.document.getString(Constant.KEY_SENDER_ID).orEmpty()
                    }

                    val encryptedMessage = documentChange.document.getString(Constant.KEY_LAST_MESSAGE).orEmpty()
                    val encryptedAesKey = documentChange.document.getString(Constant.KEY_ENCRYPTED_AES_KEY).orEmpty()

                    // 🔹 Decrypt message
                    val decryptedMessage = try {
                        val aesKey = AesUtils.base64ToKey(encryptedAesKey)
                        AesUtils.decryptMessage(encryptedMessage, aesKey)
                    } catch (e: Exception) {
                        "Decryption Failed"
                    }

                    val parts = decryptedMessage.split("||")
                    if (parts.size != 2) {
                        Log.e("ChatViewModel", "Invalid decrypted message format. Expected 'message||signature'")
                    }

                    val messageType = documentChange.document.getString(Constant.KEY_MESSAGE_TYPE).orEmpty()

                    val originalMessage = when (messageType) {
                        "photo" -> "📷 Photo"
                        "location" -> "📍 Location"
                        else -> parts[0].trim()
                    }

                    val date = documentChange.document.getDate(Constant.KEY_TIMESTAMP) ?: Date()

                    updatedConversionList.add(
                        ChatMessage(
                            senderId = senderId,
                            receiverId = receiverId,
                            message = originalMessage,
                            dateTime = date.toString(),
                            date = date,
                            conversionId = conversionId,
                            conversionName = conversionName,
                            conversionImage = conversionImage
                        )
                    )
                } else if (documentChange.type == DocumentChange.Type.MODIFIED) {
                    updatedConversionList.find {
                        it.senderId == documentChange.document.getString(Constant.KEY_SENDER_ID) &&
                                it.receiverId == documentChange.document.getString(Constant.KEY_RECEIVER_ID)
                    }?.apply {
                        message = documentChange.document.getString(Constant.KEY_LAST_MESSAGE).orEmpty()
                        date = documentChange.document.getDate(Constant.KEY_TIMESTAMP) ?: date
                    }
                }
            }

            updatedConversionList.sortByDescending { it.date }
            onUpdateRecentConversation(updatedConversionList)
        }
    }.apply {
        repository.observeRecentConversation(
            pref.getString(Constant.KEY_USER_ID, null).orEmpty(),
            this
        )
    }

}