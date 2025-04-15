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
    
    // Set to track processed message IDs to avoid duplicates
    private val processedMessageIds = mutableSetOf<String>()

    init {
        loadUserGroups()
    }

    private fun loadUserGroups() {
        val userId = pref.getString(Constant.KEY_USER_ID, null) ?: return
        viewModelScope.launch {
            groupRepository.observeUserGroups(userId)
                .catch { e -> _error.emit(e.message ?: "Error loading groups") }
                .collect { groups -> _groups.value = groups }
        }
    }

    fun loadGroupMessages(groupId: String) {
        viewModelScope.launch {
            try {
                groupRepository.observeGroupMessages(groupId)
                    .catch { e -> 
                        Log.e("GroupViewModel", "Error loading messages: ${e.message}")
                        _error.emit(e.message ?: "Error loading messages") 
                    }
                    .collect { messages -> 
                        // Simply emit the messages and let the Fragment handle deduplication
                        _groupMessages.value = messages
                        Log.d("GroupViewModel", "Loaded ${messages.size} messages for group $groupId")
                    }
            } catch (e: Exception) {
                Log.e("GroupViewModel", "Exception in loadGroupMessages: ${e.message}")
                _error.emit(e.message ?: "Error loading messages")
            }
        }
    }

    fun loadUsers() {
        viewModelScope.launch {
            try {
                mainRepository.getUsers()
                    .onSuccess { users -> 
                        // Filter out current user
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

    fun createGroup(name: String, description: String, members: List<String>) {
        viewModelScope.launch {
            val userId = pref.getString(Constant.KEY_USER_ID, null) ?: return@launch
            val userName = pref.getString(Constant.KEY_NAME, null) ?: return@launch

            val group = Group(
                name = name,
                description = description,
                createdBy = userId,
                members = members + userId,
                admins = listOf(userId)
            )

            groupRepository.createGroup(group)
                .onSuccess { groupId ->
                    // Group created successfully
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

            val groupMessage = GroupMessage(
                groupId = groupId,
                senderId = userId,
                senderName = userName,
                message = message,
                timestamp = Date()
            )

            // Call onMessageSent immediately with the newly created message
            // This allows the UI to update before waiting for Firebase
            onMessageSent?.invoke(groupMessage)

            groupRepository.sendGroupMessage(groupMessage)
                .onFailure { e ->
                    _error.emit(e.message ?: "Error sending message")
                }
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