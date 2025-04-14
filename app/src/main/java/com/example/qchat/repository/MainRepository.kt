package com.example.qchat.repository

import android.net.Uri
import android.util.Base64
import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.JsonObject
import com.example.qchat.model.MessageBody
import com.example.qchat.network.Api
import com.example.qchat.utils.AesUtils
import com.example.qchat.utils.Constant
import com.example.qchat.utils.Resource
import com.example.qchat.utils.CryptoUtils
import com.example.qchat.utils.ECDHUtils
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Response
import java.util.*
import javax.crypto.SecretKey
import javax.inject.Inject
import kotlin.collections.HashMap
import com.example.qchat.model.User
import com.example.qchat.network.ApiService
import com.example.qchat.utils.PreferenceManager

class MainRepository @Inject constructor(
    private val fireStore: FirebaseFirestore,
    private val fireMessage: FirebaseMessaging,
    private val fcmApi:Api,
    private val remoteHeader:HashMap<String,String>,
    private val apiService: ApiService,
    private val preferenceManager: PreferenceManager
) {

    suspend fun updateToken(token: String, userId: String): Boolean {
        return try {
            val documentReference =
                fireStore.collection(Constant.KEY_COLLECTION_USERS).document(userId)
            documentReference.update(Constant.KEY_FCM_TOKEN, token).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getToken(): String {
        return fireMessage.token.await()
    }

    suspend fun userSignOut(userId: String, userData: HashMap<String, Any>): Boolean {
        return try {
            val documentReference = fireStore.collection(Constant.KEY_COLLECTION_USERS)
                .document(userId)
            documentReference.update(userData).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getAllUsers(): Resource<QuerySnapshot> {
        try {
            val await = fireStore.collection(Constant.KEY_COLLECTION_USERS)
                .get()
                .await()
            if (await.isEmpty) {
                return Resource.Empty("No User Available")
            }
            return Resource.Success(await)
        } catch (e: Exception) {
            return Resource.Error(e.message ?: "An Unknown Error Occurred")
        }
    }

    fun sendMessage(message: HashMap<String, Any>): Task<Void> {
        return FirebaseFirestore.getInstance()
            .collection(Constant.KEY_COLLECTION_CHAT)
            .add(message)
            .continueWith { task ->
                if (task.isSuccessful) {
                    null
                } else {
                    throw task.exception ?: Exception("Unknown Firestore error")
                }
            }
    }


    fun observeChat(senderId: String, receiverId: String, listener: EventListener<QuerySnapshot>) {
        fireStore.collection(Constant.KEY_COLLECTION_CHAT)
            .whereIn(Constant.KEY_SENDER_ID, listOf(senderId, receiverId))
            .whereIn(Constant.KEY_RECEIVER_ID, listOf(senderId, receiverId))
            .orderBy(Constant.KEY_TIMESTAMP, Query.Direction.ASCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e("Firestore", "Error observing chat: ${error.message}")
                }
                listener.onEvent(value, error)
            }
    }

    suspend fun checkForConversion(
        senderId: String,
        receiverId: String
    ): String? {
        return try {
            val querySnapshot = fireStore.collection(Constant.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constant.KEY_SENDER_ID, senderId)
                .whereEqualTo(Constant.KEY_RECEIVER_ID, receiverId)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                querySnapshot.documents[0].id
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("Firestore", "Error checking for conversion: ${e.message}")
            null
        }
    }


    suspend fun updateRecentConversation(
        senderId: String,
        receiverId: String,
        message: HashMap<String, Any>,
        onSuccessListener: (String) -> Unit
    ) {
        try {
            val querySnapshot = fireStore.collection(Constant.KEY_COLLECTION_CONVERSATIONS)
                .whereIn(Constant.KEY_SENDER_ID, listOf(senderId, receiverId))
                .whereIn(Constant.KEY_RECEIVER_ID, listOf(senderId, receiverId))
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                val documentId = querySnapshot.documents[0].id
                fireStore.collection(Constant.KEY_COLLECTION_CONVERSATIONS)
                    .document(documentId)
                    .update(message)
                    .addOnSuccessListener {
                        onSuccessListener(documentId)
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Error updating conversation: ${e.message}")
                    }
            } else {
                fireStore.collection(Constant.KEY_COLLECTION_CONVERSATIONS)
                    .add(message)
                    .addOnSuccessListener {
                        onSuccessListener(it.id)
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Error creating new conversation: ${e.message}")
                    }
            }
        } catch (e: Exception) {
            Log.e("Firestore", "Error handling conversation update: ${e.message}")
        }
    }


    fun observeRecentConversation(id:String, listener: EventListener<QuerySnapshot>) {
        fireStore.collection(Constant.KEY_COLLECTION_CONVERSATIONS)
            .whereEqualTo(Constant.KEY_SENDER_ID, id)
            .addSnapshotListener(listener)
        fireStore.collection(Constant.KEY_COLLECTION_CONVERSATIONS)
            .whereEqualTo(Constant.KEY_RECEIVER_ID, id)
            .addSnapshotListener(listener)
    }

    fun listenerAvailabilityOfReceiver(receiverId: String,listener: EventListener<DocumentSnapshot>){
        fireStore.collection(Constant.KEY_COLLECTION_USERS)
            .document(receiverId)
            .addSnapshotListener(listener)

    }

    suspend fun sendNotification(messageBody: MessageBody): Response<JsonObject> {
        return fcmApi.sendMessage(messageBody =  messageBody, header = remoteHeader)
    }

    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>): Boolean {
        return try {
            fireStore.collection(Constant.KEY_COLLECTION_USERS)
                .document(userId)
                .update(updates)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun generateAndSaveKeys(userId: String) {
        val keyPair = ECDHUtils.generateKeyPair()
        val publicKey = ECDHUtils.publicKeyToString(keyPair.public)
        val privateKey = ECDHUtils.privateKeyToString(keyPair.private)

        saveECDHKeys(userId, publicKey, privateKey)
    }

    suspend fun saveECDHKeys(userId: String, publicKey: String, privateKey: String): Boolean {
        return try {
            val updates = hashMapOf<String, Any>(
                "ecdhPublicKey" to publicKey,
                "ecdhPrivateKey" to privateKey
            )
            fireStore.collection("users")
                .document(userId)
                .update(updates)
                .await()

            Log.d("MainRepository", "ECDH keys saved for user: $userId")
            true
        } catch (e: Exception) {
            Log.e("MainRepository", "Error saving ECDH keys for user: $userId", e)
            false
        }
    }


    suspend fun getECDHPublicKey(userId: String): String? {
        return try {
            val document = fireStore.collection(Constant.KEY_COLLECTION_USERS)
                .document(userId)
                .get()
                .await()

            val publicKey = document.getString("ecdhPublicKey")
            if (publicKey == null) {
                Log.e("MainRepository", "No public key found for userId: $userId")
            } else {
                Log.d("MainRepository", "Retrieved public key for $userId: $publicKey")
            }
            publicKey
        } catch (e: Exception) {
            Log.e("MainRepository", "Error getting public key for $userId", e)
            null
        }
    }


    suspend fun getUserECDHPrivateKey(userId: String): String? {
        return try {
            val document = fireStore.collection(Constant.KEY_COLLECTION_USERS)
                .document(userId)
                .get()
                .await()
            val privateKey = document.getString("ecdhPrivateKey")
            Log.d("MainRepository", "Retrieved private key for $userId: $privateKey")
            privateKey
        } catch (e: Exception) {
            Log.e("MainRepository", "Error getting private key for $userId", e)
            null
        }
    }

    suspend fun getReceiverECDHKeys(receiverId: String): Pair<String, String>? {
        return try {
            val receiverDoc = fireStore.collection("users").document(receiverId).get().await()
            val publicKey = receiverDoc.getString("ecdhPublicKey")
            val privateKey = receiverDoc.getString("ecdhPrivateKey")

            if (publicKey != null && privateKey != null) {
                Log.d("MainRepository", "Successfully retrieved receiver keys for $receiverId.")
                Log.d("ECDH", "Receiver Public Key: $publicKey")
                Log.d("ECDH", "Receiver Private Key: $privateKey")
                Pair(publicKey, privateKey)
            } else {
                Log.e("MainRepository", "Receiver keys not found in Firestore for user: $receiverId")
                null
            }
        } catch (e: Exception) {
            Log.e("MainRepository", "Error retrieving receiver keys for $receiverId", e)
            null
        }
    }

    suspend fun getSenderECDHKeys(senderId: String): Pair<String, String>? {
        return try {
            val senderDoc = fireStore.collection("users").document(senderId).get().await()
            val publicKey = senderDoc.getString("ecdhPublicKey")
            val privateKey = senderDoc.getString("ecdhPrivateKey")

            if (publicKey != null && privateKey != null) {
                Log.d("MainRepository", "Successfully retrieved sender keys for $senderId.")
                Log.d("ECDH", "Sender Public Key: $publicKey")
                Log.d("ECDH", "Sender Private Key: $privateKey")
                Pair(publicKey, privateKey)
            } else {
                Log.e("MainRepository", "Sender keys not found in Firestore for user: $senderId")
                null
            }
        } catch (e: Exception) {
            Log.e("MainRepository", "Error retrieving sender keys for $senderId", e)
            null
        }
    }

    suspend fun getSharedSecret(senderId: String, receiverId: String): SecretKey? {
        val (receiverPublicKeyString, receiverPrivateKeyString) = getReceiverECDHKeys(receiverId) ?: return null
        val (senderPublicKeyString, senderPrivateKeyString) = getSenderECDHKeys(senderId) ?: return null

        val receiverPrivateKey = ECDHUtils.privateKeyFromString(receiverPrivateKeyString)
        val senderPublicKey = ECDHUtils.publicKeyFromString(senderPublicKeyString)

        Log.d("ECDH", "Receiver's Private Key (String): $receiverPrivateKeyString")
        Log.d("ECDH", "Sender's Public Key (String): $senderPublicKeyString")

        return try {
            val sharedSecret = ECDHUtils.generateSharedSecret(receiverPrivateKey, senderPublicKey)

            Log.d("ECDH", "Generated Shared Secret: $sharedSecret")

            sharedSecret
        } catch (e: Exception) {
            Log.e("MainRepository", "Error generating shared secret: ${e.message}")
            null
        }
    }

    suspend fun getUsers(): Result<List<User>> = try {
        val snapshot = fireStore.collection(Constant.KEY_COLLECTION_USERS).get().await()
        val users = snapshot.documents.mapNotNull { doc ->
            try {
                // Manually create User object instead of using toObject
                val user = User(
                    name = doc.getString(Constant.KEY_NAME) ?: "",
                    image = doc.getString(Constant.KEY_IMAGE),
                    email = doc.getString(Constant.KEY_EMAIL),
                    token = doc.getString(Constant.KEY_FCM_TOKEN),
                    id = doc.id
                )
                android.util.Log.d("MainRepository", "Loaded user: ${user.name}, id: ${user.id}")
                user
            } catch (e: Exception) {
                android.util.Log.e("MainRepository", "Error parsing user document: ${doc.id}", e)
                null
            }
        }
        android.util.Log.d("MainRepository", "Total users loaded: ${users.size}")
        Result.success(users)
    } catch (e: Exception) {
        android.util.Log.e("MainRepository", "Error loading users", e)
        Result.failure(e)
    }

    suspend fun getUsersByIds(userIds: List<String>): Result<List<User>> = try {
        val users = mutableListOf<User>()
        for (userId in userIds) {
            try {
                val doc = fireStore.collection(Constant.KEY_COLLECTION_USERS)
                    .document(userId)
                    .get()
                    .await()
                
                // Manually create User object
                val user = User(
                    name = doc.getString(Constant.KEY_NAME) ?: "",
                    image = doc.getString(Constant.KEY_IMAGE),
                    email = doc.getString(Constant.KEY_EMAIL),
                    token = doc.getString(Constant.KEY_FCM_TOKEN),
                    id = doc.id
                )
                users.add(user)
                android.util.Log.d("MainRepository", "Loaded user by ID: ${user.name}, id: ${user.id}")
            } catch (e: Exception) {
                android.util.Log.e("MainRepository", "Error loading user with ID: $userId", e)
            }
        }
        android.util.Log.d("MainRepository", "Total users loaded by IDs: ${users.size}")
        Result.success(users)
    } catch (e: Exception) {
        android.util.Log.e("MainRepository", "Error in getUsersByIds", e)
        Result.failure(e)
    }
}
