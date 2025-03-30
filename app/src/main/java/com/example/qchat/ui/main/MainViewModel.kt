package com.example.qchat.ui.main

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.core.content.ContentProviderCompat.requireContext
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
import com.example.qchat.utils.ECDHUtils
import com.example.qchat.utils.clearAll
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.w3c.dom.DocumentType
import java.io.ByteArrayOutputStream
import java.util.*
import javax.inject.Inject
import kotlin.collections.HashMap
import android.util.Base64
import com.example.qchat.model.Story
import com.google.firebase.firestore.Query

@HiltViewModel
class MainViewModel @Inject constructor(
    private val pref: SharedPreferences,
    private val repository: MainRepository,
    private val fireStore: FirebaseFirestore
) : ViewModel() {

    init {
        viewModelScope.launch {
            pref.getString(Constant.KEY_USER_ID, null)?.let { userId ->
                // Only generate keys if they don't exist
                if (repository.getECDHPublicKey(userId) == null) {
                    val keyPair = ECDHUtils.generateKeyPair()
                    val publicKey = ECDHUtils.publicKeyToString(keyPair.public)
                    val privateKey = ECDHUtils.privateKeyToString(keyPair.private)

                    // Save with user-specific document
                    val updates = mapOf(
                        "ecdhPublicKey" to publicKey,
                        "ecdhPrivateKey" to privateKey
                    )
                    fireStore.collection(Constant.KEY_COLLECTION_USERS)
                        .document(userId)
                        .update(updates)
                        .await()
                }
                repository.updateToken(repository.getToken(), userId)
            }
        }
    }

    fun loadUserDetails() = pref.getString(Constant.KEY_IMAGE, null).toString()

    fun getName(): String = pref.getString(Constant.KEY_NAME, null).toString()

    fun getUserId(): String = pref.getString(Constant.KEY_USER_ID, "")!!

    fun sendPhotoToStories(encodedImage: String) {
        val senderId = pref.getString(Constant.KEY_USER_ID, null).orEmpty()
        val userName = pref.getString(Constant.KEY_NAME, null).orEmpty()
        val userProfilePicture = pref.getString(Constant.KEY_IMAGE, null).orEmpty()

        val storyMap = hashMapOf(
            "userId" to senderId,
            "userName" to userName,
            "userProfilePicture" to userProfilePicture,
            "photo" to encodedImage,
            "timestamp" to FieldValue.serverTimestamp()
        )

        FirebaseFirestore.getInstance().collection("stories")
            .add(storyMap)
            .addOnSuccessListener {
                Log.d("MainViewModel", "Story uploaded successfully")
            }
            .addOnFailureListener { e ->
                Log.e("MainViewModel", "Failed to upload story: ${e.message}")
            }
    }

    val storiesLiveData = MutableLiveData<List<Story>>()


    fun fetchStories() {
        FirebaseFirestore.getInstance().collection("stories")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("MainViewModel", "Listen failed.", e)
                    return@addSnapshotListener
                }

                val storyMap = mutableMapOf<String, Story>()
                snapshot?.documents?.forEach { document ->
                    try {
                        val story = document.toObject(Story::class.java)
                        story?.let {
                            val userId = it.userId ?: ""
                            // Compare existing story in map with the current one by timestamp
                            if (storyMap.containsKey(userId)) {
                                val existingStory = storyMap[userId]
                                if (existingStory == null || existingStory.timestamp?.compareTo(it.timestamp) ?: -1 < 0) {
                                    storyMap[userId] = it
                                }
                            } else {
                                storyMap[userId] = it
                            }
                        }
                    } catch (ex: Exception) {
                        Log.e("MainViewModel", "Error processing document ${document.id}", ex)
                    }
                }
                val latestStories = storyMap.values.toList()
                storiesLiveData.postValue(latestStories)
            }
    }


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
                        "document" -> "📄 Document"
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
