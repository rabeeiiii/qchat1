package com.example.qchat.adapter

import android.content.Context
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
import com.example.qchat.databinding.ItemReceivedDocumentBinding
import com.example.qchat.databinding.ItemReceivedLocationBinding
import com.example.qchat.databinding.ItemReceivedMessageBinding
import com.example.qchat.databinding.ItemReceivedPhotoBinding
import com.example.qchat.databinding.ItemSendDocumentBinding
import com.example.qchat.databinding.ItemSendLocationBinding
import com.example.qchat.databinding.ItemSendMessageBinding
import com.example.qchat.databinding.ItemSendPhotoBinding
import com.example.qchat.databinding.ItemSendVideoBinding
import com.example.qchat.databinding.ItemReceivedVideoBinding
import com.example.qchat.model.ChatMessage
import com.example.qchat.utils.Constant
import com.example.qchat.utils.Constant.VIEW_TYPE_RECEIVED
import com.example.qchat.utils.Constant.VIEW_TYPE_RECEIVED_DOCUMENT
import com.example.qchat.utils.Constant.VIEW_TYPE_RECEIVED_PHOTO
import com.example.qchat.utils.Constant.VIEW_TYPE_SEND
import com.example.qchat.utils.Constant.VIEW_TYPE_SEND_PHOTO
import com.example.qchat.utils.Constant.VIEW_TYPE_SEND_LOCATION
import com.example.qchat.utils.Constant.VIEW_TYPE_RECEIVED_LOCATION
import com.example.qchat.utils.Constant.VIEW_TYPE_SEND_DOCUMENT
import com.example.qchat.utils.Constant.VIEW_TYPE_SEND_VIDEO
import com.example.qchat.utils.Constant.VIEW_TYPE_RECEIVED_VIDEO
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
                .placeholder(R.drawable.placeholder_map)
                .into(binding.ivMapPreview)

            binding.tvDateTime.text = message.dateTime
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
                .placeholder(R.drawable.placeholder_map)
                .into(binding.ivMapPreview)

            binding.tvDateTime.text = message.dateTime
            profileImage?.let {
                binding.ivProfile.setImageBitmap(profileImage)
            }
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

    class SendVideoViewHolder(val binding: ItemSendVideoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun setData(message: ChatMessage) {
            binding.apply {
                Glide.with(itemView.context)
                    .load(message.thumbnailUrl)
                    .into(binding.ivThumbnail)

                binding.tvDuration.text = message.videoDuration

                binding.root.setOnClickListener {
                    openVideo(message.message)
                }
            }
        }

        private fun openVideo(videoUrl: String) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(videoUrl), "video/*")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            binding.root.context.startActivity(intent)
        }
    }

    class ReceivedVideoViewHolder(val binding: ItemReceivedVideoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun setData(message: ChatMessage, profileImage: Bitmap?) {
            binding.apply {
                profileImage?.let {
                    ivProfile.setImageBitmap(it)
                }

                Glide.with(itemView.context)
                    .load(message.thumbnailUrl)
                    .into(binding.ivThumbnail)

                binding.tvDuration.text = message.videoDuration

                binding.root.setOnClickListener {
                    openVideo(message.message)
                }
            }
        }

        private fun openVideo(videoUrl: String) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(videoUrl), "video/*")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            binding.root.context.startActivity(intent)
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
                SendLocationViewHolder(
                    ItemSendLocationBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            VIEW_TYPE_RECEIVED_LOCATION -> {
                ReceivedLocationViewHolder(
                    ItemReceivedLocationBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            VIEW_TYPE_SEND_DOCUMENT -> {
                SendDocumentViewHolder(ItemSendDocumentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            VIEW_TYPE_RECEIVED_DOCUMENT -> {
                ReceivedDocumentViewHolder(ItemReceivedDocumentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            VIEW_TYPE_SEND_VIDEO -> {
                SendVideoViewHolder(
                    ItemSendVideoBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            VIEW_TYPE_RECEIVED_VIDEO -> {
                ReceivedVideoViewHolder(
                    ItemReceivedVideoBinding.inflate(
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
            VIEW_TYPE_SEND_DOCUMENT -> {
                val sendDocumentHolder = holder as SendDocumentViewHolder
                sendDocumentHolder.setData(chatMessagesList[position])
            }
            VIEW_TYPE_RECEIVED_DOCUMENT -> {
                val receivedDocumentHolder = holder as ReceivedDocumentViewHolder
                receivedDocumentHolder.setData(chatMessagesList[position], profileImage, holder.itemView.context)
            }
            VIEW_TYPE_SEND_VIDEO -> {
                val sendVideoHolder = holder as SendVideoViewHolder
                sendVideoHolder.setData(chatMessagesList[position])
            }
            VIEW_TYPE_RECEIVED_VIDEO -> {
                val receivedVideoHolder = holder as ReceivedVideoViewHolder
                receivedVideoHolder.setData(chatMessagesList[position], profileImage)
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

        Log.d("ChatAdapter", "Determining view type for message - senderId: ${message.senderId}, adapter senderId: $senderId, messageType: ${message.messageType}")

        return when {
            message.messageType == Constant.MESSAGE_TYPE_PHOTO -> {
                if (message.senderId == senderId) {
                    VIEW_TYPE_SEND_PHOTO
                } else {
                    VIEW_TYPE_RECEIVED_PHOTO
                }
            }
            message.messageType == Constant.MESSAGE_TYPE_DOCUMENT -> {
                if (message.senderId == senderId) VIEW_TYPE_SEND_DOCUMENT else VIEW_TYPE_RECEIVED_DOCUMENT
            }
            message.messageType == Constant.MESSAGE_TYPE_LOCATION -> {
                if (message.senderId == senderId) VIEW_TYPE_SEND_LOCATION else VIEW_TYPE_RECEIVED_LOCATION
            }
            message.messageType == Constant.MESSAGE_TYPE_VIDEO -> {
                if (message.senderId == senderId) {
                    Log.d("ChatAdapter", "Video message is SENT")
                    VIEW_TYPE_SEND_VIDEO
                } else {
                    Log.d("ChatAdapter", "Video message is RECEIVED")
                    VIEW_TYPE_RECEIVED_VIDEO
                }
            }
            message.senderId == senderId -> VIEW_TYPE_SEND
            else -> VIEW_TYPE_RECEIVED
        }
    }
    class SendDocumentViewHolder(val binding: ItemSendDocumentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun setData(message: ChatMessage) {
            binding.apply {
                binding.tvDocumentName.text = message.documentName ?: "Unnamed "
                tvDateTime.text = message.dateTime
                Glide.with(itemView.context)
                    .load(message.message)

                binding.root.setOnClickListener {
                    openDocument(message.message)
                }
            }
        }

        private fun openDocument(url: String) {
            // Open the document URL (maybe using a PDF viewer)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            binding.root.context.startActivity(intent)
        }
    }

    class ReceivedDocumentViewHolder(val binding: ItemReceivedDocumentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun setData(message: ChatMessage, profileImage: Bitmap?, context: Context) {
            binding.apply {
                profileImage?.let {
                    ivProfile.setImageBitmap(it)
                }
                tvDocumentName.text = message.documentName ?: "Document"
                tvDateTime.text = message.dateTime
                root.setOnClickListener {
                    message.message.takeIf { it.isNotEmpty() }?.let { url ->
                        openDocument(url, context)
                    }
                }
            }
        }

        private fun openDocument(url: String, context: Context) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(url)
                    flags = Intent.FLAG_ACTIVITY_NO_HISTORY
                    setDataAndType(Uri.parse(url), "application/pdf")
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("ReceivedDocument", "Error opening document", e)
            }
        }
    }
}