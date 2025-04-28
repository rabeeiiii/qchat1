package com.example.qchat.adapter

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.qchat.R
import com.example.qchat.model.ChatMessage

class BlockedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val tvBlockedMessage: TextView = itemView.findViewById(R.id.tvBlockedMessage)

    fun bind(message: ChatMessage) {
        tvBlockedMessage.text = message.message
    }
} 