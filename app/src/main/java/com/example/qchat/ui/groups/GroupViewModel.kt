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

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val mainRepository: MainRepository,
    private val pref: SharedPreferences
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
                _isLoadingMessages.value = true // ✅ SHOW loader here first
                _groupMessages.value = emptyList() // optional clear before

                groupRepository.observeGroupMessages(groupId)
                    .catch { e ->
                        Log.e("GroupViewModel", "Error loading messages: ${e.message}")
                        _isLoadingMessages.value = false // ✅ HIDE on error
                        _error.emit(e.message ?: "Error loading messages")
                    }
                    .collect { messages ->
                        val aesKey = groupRepository.getGroupAesKey(groupId)
                        if (aesKey == null) {
                            _error.emit("AES Key not found for group $groupId")
                            _isLoadingMessages.value = false
                            return@collect
                        }

                        val decryptedMessages = messages.map { message ->
                            try {
                                val decryptedMessage = AesUtils.decryptGroupMessage(message.message, aesKey)
                                message.copy(message = decryptedMessage)
                            } catch (e: Exception) {
                                Log.e("GroupViewModel", "Decryption failed: ${e.message}")
                                message
                            }
                        }

                        val sortedMessages = decryptedMessages.sortedBy { it.timestamp }
                        _groupMessages.value = sortedMessages
                        _isLoadingMessages.value = false // ✅ HIDE loader after load
                    }
            } catch (e: Exception) {
                Log.e("GroupViewModel", "Exception in loadGroupMessages: ${e.message}")
                _isLoadingMessages.value = false // ✅ Always hide on catch
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
                .onFailure { e ->
                    _error.emit(e.message ?: "Error sending group message")
                }

            onMessageSent?.invoke(groupMessage)
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
} 