package com.example.qchat

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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

        // Handle clicks for each chat item
        holder.chatItemLayout.setOnClickListener {
            val intent = Intent(context, chatActivity::class.java)
            intent.putExtra("CHAT_NAME", "Moaz Mohamed")
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = chatList.size
}
