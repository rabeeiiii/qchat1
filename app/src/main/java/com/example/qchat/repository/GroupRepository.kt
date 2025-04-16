package com.example.qchat.repository

import android.util.Log
import com.example.qchat.model.Group
import com.example.qchat.model.GroupMessage
import com.example.qchat.utils.AesUtils
import com.example.qchat.utils.Constant
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.encoding.Base64

@Singleton
class GroupRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val groupsCollection = firestore.collection(Constant.KEY_COLLECTION_GROUPS)
    private val groupMessagesCollection = firestore.collection(Constant.KEY_COLLECTION_GROUP_MESSAGES)

    suspend fun getGroupAesKey(groupId: String): SecretKey? {
        try {
            val document = groupsCollection.document(groupId).get().await()
            val aesKeyBase64 = document.getString("aesKey")

            if (aesKeyBase64 != null) {
                return AesUtils.base64ToKey(aesKeyBase64)
            } else {
                Log.e("GroupRepository", "AES Key not found for group: $groupId")
            }
        } catch (e: Exception) {
            Log.e("GroupRepository", "Failed to get AES key for group: $groupId", e)
        }
        return null
    }


    suspend fun createGroup(group: Group): Result<String> = try {
        val docRef = groupsCollection.add(group).await()
        Result.success(docRef.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateGroup(groupId: String, updates: Map<String, Any>): Result<Unit> = try {
        groupsCollection.document(groupId).update(updates).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateGroupWithId(groupId: String, group: Group) {
        try {
            groupsCollection.document(groupId).update("id", groupId).await()
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error updating group ID: ${e.message}")
        }
    }

    suspend fun deleteGroup(groupId: String): Result<Unit> = try {
        groupsCollection.document(groupId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun addMember(groupId: String, userId: String): Result<Unit> = try {
        groupsCollection.document(groupId)
            .update(Constant.KEY_GROUP_MEMBERS, FieldValue.arrayUnion(userId))
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun removeMember(groupId: String, userId: String): Result<Unit> = try {
        groupsCollection.document(groupId)
            .update(Constant.KEY_GROUP_MEMBERS, FieldValue.arrayRemove(userId))
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun addAdmin(groupId: String, userId: String): Result<Unit> = try {
        groupsCollection.document(groupId)
            .update(Constant.KEY_GROUP_ADMINS, FieldValue.arrayUnion(userId))
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun removeAdmin(groupId: String, userId: String): Result<Unit> = try {
        groupsCollection.document(groupId)
            .update(Constant.KEY_GROUP_ADMINS, FieldValue.arrayRemove(userId))
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun sendGroupMessage(message: GroupMessage, aesKeyBase64: String): Result<String> = try {
        val aesKey = AesUtils.base64ToKey(aesKeyBase64)
        val encryptedMessage = AesUtils.encryptGroupMessage(message.message, aesKey)
        val encryptedGroupMessage = message.copy(message = encryptedMessage)
        val docRef = groupMessagesCollection.add(encryptedGroupMessage).await()
        val updates = hashMapOf(
            Constant.KEY_GROUP_LAST_MESSAGE to encryptedMessage,
            Constant.KEY_GROUP_LAST_MESSAGE_TIME to message.timestamp.time,
            Constant.KEY_GROUP_LAST_MESSAGE_SENDER to message.senderId
        )
        groupsCollection.document(message.groupId).update(updates as Map<String, Any>).await()

        Result.success(docRef.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun observeGroupMessages(groupId: String): Flow<List<GroupMessage>> = callbackFlow {
        try {
            val subscription = groupMessagesCollection
                .whereEqualTo(Constant.KEY_GROUP_ID, groupId)
                .orderBy("timestamp")
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        Log.e("GroupRepository", "Error observing group messages: ${error.message}")
                        return@addSnapshotListener
                    }

                    snapshot?.let {
                        val messages = it.documents.mapNotNull { doc ->
                            doc.toObject(GroupMessage::class.java)?.copy(id = doc.id)
                        }

                        if (messages.isEmpty()) {
                            trySend(emptyList())
                            return@addSnapshotListener
                        }

                        runBlocking {
                            val aesKey = getGroupAesKey(groupId)
                            if (aesKey == null) {
                                Log.e("GroupRepository", "Failed to get AES key for group: $groupId")
                                return@runBlocking
                            }

                            val decryptedMessages = messages.map { message ->
                                try {
                                    val decryptedMessage = AesUtils.decryptGroupMessage(message.message, aesKey)
                                    message.copy(message = decryptedMessage)
                                } catch (e: Exception) {
                                    Log.e("GroupRepository", "Decryption failed: ${e.message}")
                                    message
                                }
                            }

                            val sortedMessages = decryptedMessages.sortedBy { it.timestamp }
                            trySend(sortedMessages)
                        }
                    }
                }

            awaitClose { subscription.remove() }
        } catch (e: Exception) {
            Log.e("GroupRepository", "Exception in observeGroupMessages: ${e.message}")
            trySend(emptyList())
            close(e)
        }
    }

    fun observeUserGroups(userId: String): Flow<List<Group>> = callbackFlow {
        val subscription = groupsCollection
            .whereArrayContains(Constant.KEY_GROUP_MEMBERS, userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("GroupRepository", "Error observing user groups: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val groups = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Group::class.java)?.copy(id = doc.id)
                    }
                    trySend(groups)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun getGroup(groupId: String): Result<Group> = try {
        val doc = groupsCollection.document(groupId).get().await()
        val group = doc.toObject(Group::class.java)?.copy(id = doc.id)
            ?: throw Exception("Group not found")
        Result.success(group)
    } catch (e: Exception) {
        Result.failure(e)
    }
} 