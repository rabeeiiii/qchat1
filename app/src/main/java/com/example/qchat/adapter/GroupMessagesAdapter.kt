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

class GroupMessagesAdapter : ListAdapter<GroupMessage, RecyclerView.ViewHolder>(GroupMessageDiffCallback()) {

    private var currentUserId: String? = null

    fun setCurrentUserId(userId: String) {
        currentUserId = userId
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.senderId == currentUserId) {
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
} 