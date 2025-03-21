package com.example.qchat.adapter

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.qchat.R
import com.example.qchat.databinding.ItemReceivedLocationBinding
import com.example.qchat.databinding.ItemReceivedMessageBinding
import com.example.qchat.databinding.ItemReceivedPhotoBinding
import com.example.qchat.databinding.ItemSendLocationBinding
import com.example.qchat.databinding.ItemSendMessageBinding
import com.example.qchat.databinding.ItemSendPhotoBinding
import com.example.qchat.model.ChatMessage
import com.example.qchat.utils.Constant
import com.example.qchat.utils.Constant.VIEW_TYPE_RECEIVED
import com.example.qchat.utils.Constant.VIEW_TYPE_RECEIVED_PHOTO
import com.example.qchat.utils.Constant.VIEW_TYPE_SEND
import com.example.qchat.utils.Constant.VIEW_TYPE_SEND_PHOTO
import com.example.qchat.utils.Constant.VIEW_TYPE_SEND_LOCATION
import com.example.qchat.utils.Constant.VIEW_TYPE_RECEIVED_LOCATION
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

    class SendLocationViewHolder(val binding: ItemSendLocationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun setData(message: ChatMessage) {
            val locationUrl = getStaticMapUrl(message.message)
            Glide.with(binding.root.context)
                .load(locationUrl)
                .placeholder(R.drawable.placeholder_map) // Use a placeholder while loading
                .into(binding.ivMapPreview)

            binding.tvDateTime.text = message.dateTime

            // Open Google Maps when clicking on the preview
            binding.ivMapPreview.setOnClickListener {
                openLocationInMap(message.message)
            }
        }

        private fun openLocationInMap(location: String) {
            val locationParts = location.split(",")
            if (locationParts.size == 2) {
                val uri = Uri.parse("geo:${locationParts[0]},${locationParts[1]}?q=${locationParts[0]},${locationParts[1]}")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.setPackage("com.google.android.apps.maps")
                binding.root.context.startActivity(intent)
            }
        }

        private fun getStaticMapUrl(location: String): String {
            val locationParts = location.split(",")
            return "https://static-maps.yandex.ru/1.x/?lang=en_US&ll=${locationParts[1]},${locationParts[0]}&z=15&l=map&size=400,400"
        }

    }

    class ReceivedLocationViewHolder(val binding: ItemReceivedLocationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun setData(message: ChatMessage, profileImage: Bitmap?) {
            val locationUrl = getStaticMapUrl(message.message)
            Glide.with(binding.root.context)
                .load(locationUrl)
                .placeholder(R.drawable.placeholder_map) // Use a placeholder while loading
                .into(binding.ivMapPreview)

            binding.tvDateTime.text = message.dateTime
            profileImage?.let {
                binding.ivProfile.setImageBitmap(profileImage)
            }

            // Open Google Maps when clicking on the preview
            binding.ivMapPreview.setOnClickListener {
                openLocationInMap(message.message)
            }
        }

        private fun openLocationInMap(location: String) {
            val locationParts = location.split(",")
            if (locationParts.size == 2) {
                val uri = Uri.parse("geo:${locationParts[0]},${locationParts[1]}?q=${locationParts[0]},${locationParts[1]}")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.setPackage("com.google.android.apps.maps")
                binding.root.context.startActivity(intent)
            }
        }

        private fun getStaticMapUrl(location: String): String {
            val locationParts = location.split(",")
            return "https://static-maps.yandex.ru/1.x/?lang=en_US&ll=${locationParts[1]},${locationParts[0]}&z=15&l=map&size=400,400"
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
                    ItemSendMessageBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            VIEW_TYPE_RECEIVED -> {
                ReceivedMessageViewHolder(
                    ItemReceivedMessageBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            VIEW_TYPE_SEND_PHOTO -> {
                SendPhotoViewHolder(
                    ItemSendPhotoBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            VIEW_TYPE_RECEIVED_PHOTO -> {
                ReceivedPhotoViewHolder(
                    ItemReceivedPhotoBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            VIEW_TYPE_SEND_LOCATION -> {
                // Notice we are inflating the item_send_location.xml layout
                SendLocationViewHolder(
                    ItemSendLocationBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            VIEW_TYPE_RECEIVED_LOCATION -> {
                // Notice we are inflating the item_received_location.xml layout
                ReceivedLocationViewHolder(
                    ItemReceivedLocationBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
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
            VIEW_TYPE_SEND_LOCATION -> {
                val sendLocationHolder = holder as SendLocationViewHolder
                sendLocationHolder.setData(chatMessagesList[position])
            }
            VIEW_TYPE_RECEIVED_LOCATION -> {
                val receivedLocationHolder = holder as ReceivedLocationViewHolder
                receivedLocationHolder.setData(chatMessagesList[position], profileImage)
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
                    VIEW_TYPE_SEND_PHOTO
                } else {
                    VIEW_TYPE_RECEIVED_PHOTO
                }
            }
            // Check if it's a location message
            message.messageType == Constant.MESSAGE_TYPE_LOCATION -> {
                if (message.senderId == senderId) {
                    VIEW_TYPE_SEND_LOCATION
                } else {
                    VIEW_TYPE_RECEIVED_LOCATION
                }
            }
            // Otherwise text
            message.senderId == senderId -> VIEW_TYPE_SEND
            else -> VIEW_TYPE_RECEIVED
        }
    }



}