package com.example.qchat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.qchat.databinding.ItemUserListBinding
import com.example.qchat.databinding.ItemUserListRecentConversionBinding
import com.example.qchat.model.ChatMessage
import com.example.qchat.model.User
import com.example.qchat.utils.decodeToBitmap
import java.text.SimpleDateFormat
import java.util.Locale

class RecentConversationsAdapter :
    RecyclerView.Adapter<RecentConversationsAdapter.ConversationViewHolder>() {

    private var recentConversationList = mutableListOf<ChatMessage>()
    var onClickConversation: ((User) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ConversationViewHolder(ItemUserListRecentConversionBinding.inflate(LayoutInflater.from(parent.context),parent,false))

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.setData(recentConversationList[position])
    }

    override fun getItemCount() = recentConversationList.size

    fun getRecentList() = recentConversationList

    fun updateRecentConversion(conversation: List<ChatMessage>) {
        recentConversationList.clear()
        recentConversationList.addAll(conversation)
        notifyDataSetChanged()
    }

    inner class ConversationViewHolder(private val binding: ItemUserListRecentConversionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun setData(chatMessage: ChatMessage) {
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            with(binding) {
                tvName.text = chatMessage.conversionName
                tvRecentMessage.text = chatMessage.message
                tvTimestamp.text = timeFormat.format(chatMessage.date)
                ivProfile.setImageBitmap(chatMessage.conversionImage?.decodeToBitmap())
                root.setOnClickListener {
                    onClickConversation?.let { it1 ->
                        it1(User(
                            id = chatMessage.conversionId.toString(),
                            name = chatMessage.conversionName.toString(),
                            image = chatMessage.conversionImage.toString()
                        ))
                    }
                }
            }

        }

    }

}