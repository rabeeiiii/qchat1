package com.example.qchat.ui.groups

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qchat.model.Group
import com.example.qchat.model.GroupMessage
import com.example.qchat.model.User
import com.example.qchat.repository.GroupRepository
import com.example.qchat.repository.MainRepository
import com.example.qchat.utils.Constant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import android.util.Log
import com.example.qchat.utils.AesUtils
import com.example.qchat.network.NotificationService
import com.google.firebase.firestore.FieldValue
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val mainRepository: MainRepository,
    private val pref: SharedPreferences,
    private val notificationService: NotificationService
) : ViewModel() {

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _currentGroup = MutableStateFlow<Group?>(null)
    val currentGroup: StateFlow<Group?> = _currentGroup.asStateFlow()

    private val _groupMessages = MutableStateFlow<List<GroupMessage>>(emptyList())
    val groupMessages: StateFlow<List<GroupMessage>> = _groupMessages.asStateFlow()

    private val _groupMembers = MutableStateFlow<List<User>>(emptyList())
    val groupMembers: StateFlow<List<User>> = _groupMembers.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    private val _isLoadingMessages = MutableStateFlow(false)
    val isLoadingMessages: StateFlow<Boolean> = _isLoadingMessages.asStateFlow()

    private val processedMessageIds = mutableSetOf<String>()

    init {
        loadUserGroups()
    }

    private fun loadUserGroups() {
        val userId = pref.getString(Constant.KEY_USER_ID, null) ?: return

        viewModelScope.launch {
            groupRepository.observeUserGroups(userId)
                .catch { e -> _error.emit(e.message ?: "Error loading groups") }
                .collect { groups ->
                    val decryptedGroups = groups.map { group ->
                        val key = groupRepository.getGroupAesKey(group.id)
                        val decryptedLastMessage = try {
                            if (!group.lastMessage.isNullOrEmpty() && key != null) {
                                AesUtils.decryptGroupMessage(group.lastMessage, key)
                            } else {
                                group.lastMessage
                            }
                        } catch (e: Exception) {
                            Log.e("GroupViewModel", "Failed to decrypt group ${group.id}: ${e.message}")
                            group.lastMessage // Fallback to encrypted if failed
                        }

                        group.copy(lastMessage = decryptedLastMessage)
                    }

                    _groups.value = decryptedGroups
                }
        }
    }



    fun clearGroupMessages() {
        _groupMessages.value = emptyList()
    }

    fun loadGroupMessages(groupId: String) {
        viewModelScope.launch {
            try {
                _isLoadingMessages.value = true
                _groupMessages.value = emptyList()

                groupRepository.observeGroupMessages(groupId)
                    .catch { e ->
                        _isLoadingMessages.value = false
                        _error.emit(e.message ?: "Error loading messages")
                    }
                    .collect { messages ->
                        val aesKey = groupRepository.getGroupAesKey(groupId)
                        if (aesKey == null) {
                            _error.emit("AES Key not found for group $groupId")
                            _isLoadingMessages.value = false
                            return@collect
                        }

                        val parsedMessages = messages.map { message ->
                            try {
                                val decrypted = AesUtils.decryptGroupMessage(message.message, aesKey)

                                when (message.messageType) {
                                    Constant.MESSAGE_TYPE_TEXT -> {
                                        val parts = decrypted.split("||")
                                        if (parts.size == 2) {
                                            message.copy(message = parts[0])
                                        } else message.copy(message = decrypted)
                                    }

                                    Constant.MESSAGE_TYPE_DOCUMENT -> {
                                        val parts = decrypted.split("||")
                                        if (parts.size >= 3) {
                                            message.copy(
                                                message = parts[1], // URL
                                                documentName = parts[2]
                                            )
                                        } else message.copy(message = decrypted)
                                    }

                                    Constant.MESSAGE_TYPE_VIDEO -> {
                                        val parts = decrypted.split("||")
                                        if (parts.size >= 4) {
                                            message.copy(
                                                message = parts[1], // Video URL
                                                thumbnailUrl = parts[2],
                                                videoDuration = parts[3]
                                            )
                                        } else message.copy(message = decrypted)
                                    }

                                    Constant.MESSAGE_TYPE_AUDIO -> {
                                        val parts = decrypted.split("||")
                                        if (parts.size >= 2) {
                                            message.copy(message = parts[1])
                                        } else message.copy(message = decrypted)
                                    }

                                    else -> message.copy(message = decrypted)
                                }
                            } catch (e: Exception) {
                                Log.e("GroupViewModel", "Decryption failed: ${e.message}")
                                message
                            }
                        }

                        _groupMessages.value = parsedMessages.sortedBy { it.timestamp }
                        _isLoadingMessages.value = false
                    }
            } catch (e: Exception) {
                _isLoadingMessages.value = false
                _error.emit(e.message ?: "Error loading messages")
            }
        }
    }


    fun loadUsers() {
        viewModelScope.launch {
            try {
                mainRepository.getUsers()
                    .onSuccess { users -> 
                        val currentUserId = pref.getString(Constant.KEY_USER_ID, null)
                        _groupMembers.value = users.filter { it.id != currentUserId }
                    }
                    .onFailure { e -> 
                        _error.emit(e.message ?: "Error loading users")
                        _groupMembers.value = emptyList()
                    }
            } catch (e: Exception) {
                _error.emit(e.message ?: "Error loading users")
                _groupMembers.value = emptyList()
            }
        }
    }

    fun loadGroupMembers(groupId: String) {
        viewModelScope.launch {
            groupRepository.getGroup(groupId)
                .onSuccess { group ->
                    mainRepository.getUsersByIds(group.members)
                        .onSuccess { users -> _groupMembers.value = users }
                        .onFailure { e -> _error.emit(e.message ?: "Error loading group members") }
                }
                .onFailure { e -> _error.emit(e.message ?: "Error loading group") }
        }
    }

    fun createGroup(name: String, description: String, members: List<String>, imageBase64: String?)
    {        viewModelScope.launch {
            val userId = pref.getString(Constant.KEY_USER_ID, null) ?: return@launch
            val userName = pref.getString(Constant.KEY_NAME, null) ?: return@launch
            val aesKey = AesUtils.generateAESKey()

            val aesKeyBase64 = AesUtils.keyToBase64(aesKey)
            val group = Group(
                name = name,
                description = description,
                createdBy = userId,
                members = members + userId,
                admins = listOf(userId),
                aesKey = aesKeyBase64,
                image = imageBase64
            )
            groupRepository.createGroup(group)
                .onSuccess { groupId ->
                    Log.d("GroupViewModel", "Group created with ID: $groupId")
                    groupRepository.updateGroupWithId(groupId, group)
                }
                .onFailure { e ->
                    _error.emit(e.message ?: "Error creating group")
                }
        }
    }

    fun sendMessage(message: String, groupId: String, onMessageSent: ((GroupMessage) -> Unit)? = null) {
        viewModelScope.launch {
            val userId = pref.getString(Constant.KEY_USER_ID, null) ?: return@launch
            val userName = pref.getString(Constant.KEY_NAME, null) ?: return@launch
            val aesKey = groupRepository.getGroupAesKey(groupId) ?: run {
                Log.e("GroupViewModel", "Failed to get AES key for group $groupId")
                return@launch
            }
            val encryptedMessage = AesUtils.encryptGroupMessage(message, aesKey)

            val groupMessage = GroupMessage(
                groupId = groupId,
                senderId = userId,
                senderName = userName,
                message = encryptedMessage,
                timestamp = Date()
            )
            groupRepository.sendGroupMessage(groupMessage, AesUtils.keyToBase64(aesKey))
                .onSuccess { messageId ->
                    Log.d("GroupViewModel", "Message sent successfully, preparing to send notifications")
                    // Send notifications to all group members except the sender
                    sendNotificationsToGroupMembers(groupId, message, userName)
                    onMessageSent?.invoke(groupMessage)
                }
                .onFailure { e ->
                    _error.emit(e.message ?: "Error sending group message")
                }
        }
    }

    private suspend fun sendNotificationsToGroupMembers(groupId: String, message: String, senderName: String) {
        try {
            // Get all group members
            val group = groupRepository.getGroup(groupId).getOrNull() ?: return
            val currentUserId = pref.getString(Constant.KEY_USER_ID, null) ?: return

            // Send notification to each member except the sender
            group.members.forEach { memberId ->
                if (memberId != currentUserId) {
                    withContext(Dispatchers.IO) {
                        val memberToken = mainRepository.getUserFcmToken(memberId)
                        if (memberToken != null) {
                            notificationService.sendNotification(
                                fcmToken = memberToken,
                                title = "New message in ${group.name}",
                                body = "$senderName: $message",
                                data = mapOf(
                                    "type" to "group_message",
                                    "groupId" to groupId,
                                    "senderId" to currentUserId,
                                    "senderName" to senderName
                                )
                            ).onFailure { e ->
                                Log.e("GroupViewModel", "Failed to send notification to $memberId: ${e.message}")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GroupViewModel", "Error sending group notifications: ${e.message}")
        }
    }

    fun leaveGroup(groupId: String, userId: String) {
        viewModelScope.launch {
            groupRepository.removeMember(groupId, userId)
                .onFailure { e ->
                    _error.emit(e.message ?: "Error leaving group")
                }
        }
    }

    fun addMember(groupId: String, userId: String) {
        viewModelScope.launch {
            groupRepository.addMember(groupId, userId)
                .onFailure { e ->
                    _error.emit(e.message ?: "Error adding member")
                }
        }
    }

    fun removeMember(groupId: String, userId: String) {
        viewModelScope.launch {
            groupRepository.removeMember(groupId, userId)
                .onFailure { e ->
                    _error.emit(e.message ?: "Error removing member")
                }
        }
    }

    fun addAdmin(groupId: String, userId: String) {
        viewModelScope.launch {
            groupRepository.addAdmin(groupId, userId)
                .onFailure { e ->
                    _error.emit(e.message ?: "Error adding admin")
                }
        }
    }

    fun removeAdmin(groupId: String, userId: String) {
        viewModelScope.launch {
            groupRepository.removeAdmin(groupId, userId)
                .onFailure { e ->
                    _error.emit(e.message ?: "Error removing admin")
                }
        }
    }

    fun updateGroup(groupId: String, updates: Map<String, Any>) {
        viewModelScope.launch {
            groupRepository.updateGroup(groupId, updates)
                .onFailure { e ->
                    _error.emit(e.message ?: "Error updating group")
                }
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            groupRepository.deleteGroup(groupId)
                .onFailure { e ->
                    _error.emit(e.message ?: "Error deleting group")
                }
        }
    }

    fun loadGroup(groupId: String) {
        viewModelScope.launch {
            groupRepository.getGroup(groupId)
                .onSuccess { group -> _currentGroup.value = group }
                .onFailure { e ->
                    _error.emit(e.message ?: "Error loading group")
                }
        }
    }

    fun getUserName(userId: String, callback: (String) -> Unit) {
        viewModelScope.launch {
            try {
                mainRepository.getUserById(userId)
                    .onSuccess { user -> 
                        callback(user.name)
                    }
                    .onFailure { 
                        callback("Unknown User")
                    }
            } catch (e: Exception) {
                Log.e("GroupViewModel", "Error fetching user name: ${e.message}")
                callback("Unknown User")
            }
        }
    }

    fun sendGroupPhoto(encodedImage: String, groupId: String) {
        viewModelScope.launch {
            val userId = pref.getString(Constant.KEY_USER_ID, null) ?: return@launch
            val userName = pref.getString(Constant.KEY_NAME, null) ?: return@launch
            val aesKey = groupRepository.getGroupAesKey(groupId) ?: return@launch

            val encrypted = AesUtils.encryptGroupMessage(encodedImage, aesKey)

            val groupMessage = GroupMessage(
                groupId = groupId,
                senderId = userId,
                senderName = userName,
                message = encrypted,
                messageType = Constant.MESSAGE_TYPE_PHOTO,
                timestamp = Date()
            )

            groupRepository.sendGroupMessage(groupMessage, AesUtils.keyToBase64(aesKey))
        }
    }

    fun sendGroupVideo(videoBytes: ByteArray, thumbnailBytes: ByteArray, groupId: String, duration: String) {
        viewModelScope.launch {
            val userId = pref.getString(Constant.KEY_USER_ID, null) ?: return@launch
            val userName = pref.getString(Constant.KEY_NAME, null) ?: return@launch
            val aesKey = groupRepository.getGroupAesKey(groupId) ?: return@launch

            val timestamp = System.currentTimeMillis()
            val videoPath = "group_videos/video_$timestamp.mp4"
            val thumbPath = "group_thumbs/thumb_$timestamp.jpg"

            val storage = FirebaseStorage.getInstance().reference
            val videoRef = storage.child(videoPath)
            val thumbRef = storage.child(thumbPath)

            videoRef.putBytes(videoBytes).addOnSuccessListener {
                videoRef.downloadUrl.addOnSuccessListener { videoUri ->
                    thumbRef.putBytes(thumbnailBytes).addOnSuccessListener {
                        thumbRef.downloadUrl.addOnSuccessListener { thumbUri ->
                            val message = "VIDEO||$videoUri||$thumbUri||$duration"
                            val encrypted = AesUtils.encryptGroupMessage(message, aesKey)

                            val groupMessage = GroupMessage(
                                groupId = groupId,
                                senderId = userId,
                                senderName = userName,
                                message = encrypted,
                                messageType = Constant.MESSAGE_TYPE_VIDEO,
                                thumbnailUrl = thumbUri.toString(),
                                videoDuration = duration,
                                timestamp = Date()
                            )
                            viewModelScope.launch {
                                groupRepository.sendGroupMessage(
                                    groupMessage,
                                    AesUtils.keyToBase64(aesKey)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun sendGroupDocument(fileBytes: ByteArray, fileName: String, groupId: String) {
        viewModelScope.launch {
            val userId = pref.getString(Constant.KEY_USER_ID, null) ?: return@launch
            val userName = pref.getString(Constant.KEY_NAME, null) ?: return@launch
            val aesKey = groupRepository.getGroupAesKey(groupId) ?: return@launch

            val docPath = "group_documents/${System.currentTimeMillis()}_$fileName"
            val docRef = FirebaseStorage.getInstance().reference.child(docPath)

            docRef.putBytes(fileBytes).addOnSuccessListener {
                docRef.downloadUrl.addOnSuccessListener { uri ->
                    val message = "DOCUMENT||${uri}||$fileName"
                    val encrypted = AesUtils.encryptGroupMessage(message, aesKey)

                    val groupMessage = GroupMessage(
                        groupId = groupId,
                        senderId = userId,
                        senderName = userName,
                        message = encrypted,
                        messageType = Constant.MESSAGE_TYPE_DOCUMENT,
                        documentName = fileName,
                        timestamp = Date()
                    )
                    viewModelScope.launch {
                        groupRepository.sendGroupMessage(
                            groupMessage,
                            AesUtils.keyToBase64(aesKey)
                        )
                    }
                }
            }
        }
    }

    fun sendGroupLocation(latitude: Double, longitude: Double, groupId: String) {
        viewModelScope.launch {
            val userId = pref.getString(Constant.KEY_USER_ID, null) ?: return@launch
            val userName = pref.getString(Constant.KEY_NAME, null) ?: return@launch
            val aesKey = groupRepository.getGroupAesKey(groupId) ?: return@launch

            val locationString = "$latitude,$longitude"
            val encryptedLocation = AesUtils.encryptGroupMessage(locationString, aesKey)

            val groupMessage = GroupMessage(
                groupId = groupId,
                senderId = userId,
                senderName = userName,
                message = encryptedLocation,
                messageType = Constant.MESSAGE_TYPE_LOCATION,
                timestamp = Date()
            )

            groupRepository.sendGroupMessage(groupMessage, AesUtils.keyToBase64(aesKey))
                .onFailure { e -> Log.e("GroupViewModel", "Failed to send location: ${e.message}") }
        }
    }


    fun sendGroupAudio(audioBytes: ByteArray, groupId: String , durationInMillis: Long) {
        viewModelScope.launch {
            val userId = pref.getString(Constant.KEY_USER_ID, null) ?: return@launch
            val userName = pref.getString(Constant.KEY_NAME, null) ?: return@launch
            val aesKey = groupRepository.getGroupAesKey(groupId) ?: return@launch

            val audioName = "group_audio/audio_${System.currentTimeMillis()}.3gp"
            val audioRef = FirebaseStorage.getInstance().reference.child(audioName)

            audioRef.putBytes(audioBytes).addOnSuccessListener {
                audioRef.downloadUrl.addOnSuccessListener { uri ->
                    val message = "AUDIO||$uri"
                    val encrypted = AesUtils.encryptGroupMessage(message, aesKey)

                    val groupMessage = GroupMessage(
                        groupId = groupId,
                        senderId = userId,
                        senderName = userName,
                        message = encrypted,
                        messageType = Constant.MESSAGE_TYPE_AUDIO,
                        audioDurationInMillis = durationInMillis,
                        timestamp = Date()
                    )

                    viewModelScope.launch {
                        groupRepository.sendGroupMessage(groupMessage, AesUtils.keyToBase64(aesKey))
                    }
                }
            }
        }
    }




} 