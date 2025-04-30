package com.example.qchat.adapter

import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.qchat.databinding.ItemGroupMessageReceivedBinding
import com.example.qchat.databinding.ItemGroupMessageSentBinding
import com.example.qchat.model.GroupMessage
import com.example.qchat.utils.Constant
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import android.util.Log

class GroupMessagesAdapter @Inject constructor(
    private val preferences: SharedPreferences
) : ListAdapter<GroupMessage, RecyclerView.ViewHolder>(GroupMessageDiffCallback()) {

    private val currentUserId: String? 
        get() = preferences.getString(Constant.KEY_USER_ID, null)

    private val messagesList = mutableListOf<GroupMessage>()
    private val messageIds = mutableSetOf<String>()
    private val tempMessageSignatures = mutableSetOf<String>()
    
    init {
        // Log the current user ID on adapter initialization
        Log.d("GroupMessagesAdapter", "Adapter initialized with user ID: $currentUserId")
    }
    
    private fun createMessageSignature(message: GroupMessage): String {
        // Create a unique signature using sender, content and approximate timestamp (rounded to seconds)
        val timestampSeconds = message.timestamp.time / 1000
        return "${message.senderId}|${message.message}|$timestampSeconds"
    }
    
    fun addMessage(message: GroupMessage, recyclerView: RecyclerView? = null) {
        // Generate a signature for this message
        val signature = createMessageSignature(message)
        
        // Check if message already exists using the ID
        if (message.id.isNotEmpty() && messageIds.contains(message.id)) {
            Log.d("GroupMessagesAdapter", "Skipping duplicate message by ID: ${message.id}")
            return
        }
        
        // Check for duplicate temporary messages using signature
        if (tempMessageSignatures.contains(signature)) {
            Log.d("GroupMessagesAdapter", "Skipping duplicate temp message by signature: $signature")
            return
        }
        
        val newList = ArrayList(messagesList)
        newList.add(message)
        
        // Sort by timestamp
        newList.sortBy { it.timestamp }
        
        // Track this message
        if (message.id.isNotEmpty()) {
            messageIds.add(message.id)
        } else {
            // For temporary messages, track by signature
            tempMessageSignatures.add(signature)
        }
        
        Log.d("GroupMessagesAdapter", "Adding message: id=${message.id}, signature=$signature")
        
        messagesList.clear()
        messagesList.addAll(newList)
        
        // Use a temporary copy for DiffUtil
        super.submitList(ArrayList(newList))
        
        recyclerView?.let {
            it.post {
                it.scrollToPosition(newList.size - 1)
            }
        }
    }
    
    fun addMessages(messages: List<GroupMessage>, recyclerView: RecyclerView? = null) {
        // Log incoming messages
        Log.d("GroupMessagesAdapter", "Received ${messages.size} total messages")
        
        // Filter out messages we've already processed by ID or by signature
        val newMessages = messages.filter { message -> 
            val signature = createMessageSignature(message)
            val isNew = message.id.isNotEmpty() && !messageIds.contains(message.id) && !tempMessageSignatures.contains(signature)
            
            if (!isNew) {
                Log.d("GroupMessagesAdapter", "Filtering out duplicate: id=${message.id}, signature=$signature")
            }
            
            isNew
        }
        
        if (newMessages.isEmpty()) {
            Log.d("GroupMessagesAdapter", "No new messages to add after filtering")
            return
        }
        
        Log.d("GroupMessagesAdapter", "Adding ${newMessages.size} new messages after filtering")
        
        // Add all new messages to the existing list
        val updatedList = ArrayList(messagesList)
        
        // Process each new message
        newMessages.forEach { message -> 
            val signature = createMessageSignature(message)
            
            // Remove any temporary messages with the same signature
            // This helps replace temporary messages with their real versions from Firebase
            val tempMessageIndex = updatedList.indexOfFirst { 
                createMessageSignature(it) == signature && it.id.startsWith("temp_") 
            }
            
            if (tempMessageIndex >= 0) {
                Log.d("GroupMessagesAdapter", "Replacing temp message with real message: $signature")
                updatedList.removeAt(tempMessageIndex)
            }
            
            // Add the new message
            updatedList.add(message)
            
            // Track this message
            messageIds.add(message.id)
            tempMessageSignatures.add(signature)
        }
        
        // Sort by timestamp
        updatedList.sortBy { it.timestamp }
        
        Log.d("GroupMessagesAdapter", "Updated list size: ${updatedList.size}")
        
        messagesList.clear()
        messagesList.addAll(updatedList)
        
        // Use a temporary copy for DiffUtil
        super.submitList(ArrayList(updatedList))
        
        recyclerView?.let {
            it.post {
                it.scrollToPosition(updatedList.size - 1)
            }
        }
    }

    override fun submitList(list: List<GroupMessage>?) {
        if (list == null) {
            clearMessages()
            super.submitList(null)
            return
        }

        // Explicitly clear before adding new if it's a completely different group
        if (list.any() && messagesList.any() && list[0].groupId != messagesList[0].groupId) {
            clearMessages()
        }

        addMessages(list)
    }


    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        val currentId = currentUserId
        
        // Log the message sender and current user for debugging
        Log.d("GroupMessagesAdapter", "GetItemViewType for message ${message.id}: " +
                "sender=${message.senderId}, currentUser=$currentId, " +
                "isMine=${message.senderId == currentId}")
        
        return if (message.senderId == currentId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val binding = ItemGroupMessageSentBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                SentMessageViewHolder(binding)
            }
            else -> {
                val binding = ItemGroupMessageReceivedBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ReceivedMessageViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
        }
    }

    inner class SentMessageViewHolder(
        private val binding: ItemGroupMessageSentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: GroupMessage) {
            binding.apply {
                textViewMessage.text = message.message
                textViewTime.text = formatTime(message.timestamp)
                textViewSenderName.text = message.senderName
            }
        }
    }

    inner class ReceivedMessageViewHolder(
        private val binding: ItemGroupMessageReceivedBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: GroupMessage) {
            binding.apply {
                textViewMessage.text = message.message
                textViewTime.text = formatTime(message.timestamp)
                textViewSenderName.text = message.senderName
            }
        }
    }

    private fun formatTime(date: Date): String {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return timeFormat.format(date)
    }

    private class GroupMessageDiffCallback : DiffUtil.ItemCallback<GroupMessage>() {
        override fun areItemsTheSame(oldItem: GroupMessage, newItem: GroupMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: GroupMessage, newItem: GroupMessage): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    // Call this method when the fragment is destroyed to prevent duplicates when returning
    fun clearMessages() {
        messagesList.clear()
        messageIds.clear()
        tempMessageSignatures.clear()
        super.submitList(emptyList()) // Use super to wipe the diff util state
        Log.d("GroupMessagesAdapter", "Messages cleared")
    }

} 