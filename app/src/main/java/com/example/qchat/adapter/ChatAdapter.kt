package com.example.qchat.adapter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.qchat.databinding.ItemReceivedMessageBinding
import com.example.qchat.databinding.ItemReceivedPhotoBinding
import com.example.qchat.databinding.ItemSendMessageBinding
import com.example.qchat.databinding.ItemSendPhotoBinding
import com.example.qchat.model.ChatMessage
import com.example.qchat.utils.Constant
import com.example.qchat.utils.Constant.VIEW_TYPE_RECEIVED
import com.example.qchat.utils.Constant.VIEW_TYPE_RECEIVED_PHOTO
import com.example.qchat.utils.Constant.VIEW_TYPE_SEND
import com.example.qchat.utils.Constant.VIEW_TYPE_SEND_PHOTO
import java.util.*

class ChatAdapter(
    private val senderId: String,
    private val chatMessages: List<ChatMessage>
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var profileImage: Bitmap? = null

    private var chatMessagesList = mutableListOf<ChatMessage>()

    init {
        chatMessagesList.addAll(chatMessages)
    }


    class SendMessageViewHolder(val binding: ItemSendMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun setData(message: ChatMessage) {
            binding.apply {
                tvMessage.text = message.message
                tvDateTime.text = message.dateTime
            }
        }
    }

    class ReceivedMessageViewHolder(private val binding: ItemReceivedMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun setData(message: ChatMessage, profileImage: Bitmap?) {
            binding.apply {
                tvMessage.text = message.message
                tvDateTime.text = message.dateTime
                profileImage?.let {
                    ivProfile.setImageBitmap(profileImage)
                }
            }
        }

    }

    class SendPhotoViewHolder(val binding: ItemSendPhotoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun setData(message: ChatMessage) {
            try {
                Log.d("SendPhotoViewHolder", "Photo Base64 (first 100 chars): ${message.message.take(100)}") // Log first 100 characters
                val decodedBytes = Base64.decode(message.message, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                if (bitmap != null) {
                    binding.ivMessageImage.setImageBitmap(bitmap)
                    binding.tvDateTime.text = message.dateTime
                    Log.d("SendPhotoViewHolder", "Successfully decoded photo")
                } else {
                    Log.e("SendPhotoViewHolder", "Failed to decode image!")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("SendPhotoViewHolder", "Error decoding photo message: ${e.message}")
            }
        }
    }

    class ReceivedPhotoViewHolder(val binding: ItemReceivedPhotoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun setData(message: ChatMessage, profileImage: Bitmap?) {
            try {
                Log.d("ReceivedPhotoViewHolder", "Photo Base64 (first 100 chars): ${message.message.take(100)}")

                val decodedBytes = Base64.decode(message.message, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                if (bitmap != null) {
                    binding.ivMessageImage.setImageBitmap(bitmap)
                    binding.tvDateTime.text = message.dateTime
                    profileImage?.let { binding.ivProfile.setImageBitmap(it) }
                    Log.d("ReceivedPhotoViewHolder", "Successfully decoded received photo")
                } else {
                    Log.e("ReceivedPhotoViewHolder", "Failed to decode received image!")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("ReceivedPhotoViewHolder", "Error decoding received photo: ${e.message}")
            }
        }
    }

    fun addMessage(newMessage: List<ChatMessage>, rvChat: RecyclerView) {
        val uniqueMessages = newMessage.filterNot { newMsg ->
            chatMessagesList.any { it.date == newMsg.date && it.message == newMsg.message }
        }

        val initialSize = chatMessagesList.size
        chatMessagesList.addAll(uniqueMessages)
        chatMessagesList.sortBy { it.date }

        if (chatMessagesList.isNotEmpty()) {
            notifyItemRangeInserted(initialSize, uniqueMessages.size)
            rvChat.scrollToPosition(chatMessagesList.size - 1)
        }
    }


    fun getMessageSize() = chatMessagesList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SEND -> {
                SendMessageViewHolder(
                    ItemSendMessageBinding.inflate(LayoutInflater.from(parent.context),
                        parent,
                        false)
                )
            }
            VIEW_TYPE_RECEIVED -> {
                ReceivedMessageViewHolder(
                    ItemReceivedMessageBinding.inflate(LayoutInflater.from(parent.context),
                        parent,
                        false)
                )
            }
            VIEW_TYPE_SEND_PHOTO -> {
                SendPhotoViewHolder(
                    ItemSendPhotoBinding.inflate(LayoutInflater.from(parent.context),
                        parent,
                        false)
                )
            }
            VIEW_TYPE_RECEIVED_PHOTO -> {
                ReceivedPhotoViewHolder(
                    ItemReceivedPhotoBinding.inflate(LayoutInflater.from(parent.context),
                        parent,
                        false)
                )
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            VIEW_TYPE_SEND -> {
                val sendHolder = holder as SendMessageViewHolder
                sendHolder.setData(chatMessagesList[position])
            }
            VIEW_TYPE_RECEIVED -> {
                val receivedHolder = holder as ReceivedMessageViewHolder
                receivedHolder.setData(chatMessagesList[position], profileImage)
            }
            VIEW_TYPE_SEND_PHOTO -> {
                val sendPhotoHolder = holder as SendPhotoViewHolder
                sendPhotoHolder.setData(chatMessagesList[position])
            }
            VIEW_TYPE_RECEIVED_PHOTO -> {
                val receivedPhotoHolder = holder as ReceivedPhotoViewHolder
                receivedPhotoHolder.setData(chatMessagesList[position], profileImage)
            }
        }
    }

    fun setProfileImage(profileImage: Bitmap) {
        this.profileImage = profileImage
        notifyDataSetChanged()
    }

    override fun getItemCount() = chatMessagesList.size

    override fun getItemViewType(position: Int): Int {
        val message = chatMessagesList[position]
        Log.d("ChatAdapter", "MessageType: ${message.messageType}, SenderId: ${message.senderId}")

        return when {
            message.messageType == Constant.MESSAGE_TYPE_PHOTO -> {
                if (message.senderId == senderId) {
                    Constant.VIEW_TYPE_SEND_PHOTO
                } else {
                    Constant.VIEW_TYPE_RECEIVED_PHOTO
                }
            }
            message.senderId == senderId -> Constant.VIEW_TYPE_SEND
            else -> Constant.VIEW_TYPE_RECEIVED
        }
    }


}