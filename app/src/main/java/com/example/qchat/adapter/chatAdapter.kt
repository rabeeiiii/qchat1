package com.example.qchat

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.qchat.model.ChatMessage

class ChatAdapter(
    private val context: Context,
    private val chatList: List<ChatMessage>
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.textView_message)
        val timestampText: TextView = itemView.findViewById(R.id.textView_timestamp)
        val chatItemLayout: View = itemView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_bubble, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = chatList[position]

        holder.messageText.text = message.text
        holder.timestampText.text = message.timestamp

        val params = holder.chatItemLayout.layoutParams as ViewGroup.MarginLayoutParams
        val constraintLayout = holder.chatItemLayout as androidx.constraintlayout.widget.ConstraintLayout
        val messageTextLayoutParams = holder.messageText.layoutParams as ConstraintLayout.LayoutParams
        val timestampTextLayoutParams = holder.timestampText.layoutParams as ConstraintLayout.LayoutParams

        if (message.isSent) {
            // right
            messageTextLayoutParams.startToStart = ConstraintLayout.LayoutParams.UNSET
            messageTextLayoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            timestampTextLayoutParams.startToStart = ConstraintLayout.LayoutParams.UNSET
            timestampTextLayoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            holder.messageText.setBackgroundResource(R.drawable.bg_chat_bubble_sent)
        } else {
            // left
            messageTextLayoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            messageTextLayoutParams.endToEnd = ConstraintLayout.LayoutParams.UNSET
            timestampTextLayoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            timestampTextLayoutParams.endToEnd = ConstraintLayout.LayoutParams.UNSET
            holder.messageText.setBackgroundResource(R.drawable.bg_chat_bubble_received)
        }

        holder.messageText.layoutParams = messageTextLayoutParams
        holder.timestampText.layoutParams = timestampTextLayoutParams
    }


    override fun getItemCount(): Int = chatList.size
}
