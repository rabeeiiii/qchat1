package com.example.qchat.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.qchat.R
import com.example.qchat.databinding.ItemReceivedAudioBinding
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
import com.example.qchat.databinding.ItemSendAudioBinding
import com.example.qchat.model.ChatMessage
import com.example.qchat.ui.chat.PdfRendererActivity
import com.example.qchat.ui.chat.PhotoViewerActivity
import com.example.qchat.ui.chat.VideoPlayerActivity
import com.example.qchat.utils.Constant
import com.example.qchat.utils.Constant.VIEW_TYPE_BLOCKED
import com.example.qchat.utils.Constant.VIEW_TYPE_RECEIVED
import com.example.qchat.utils.Constant.VIEW_TYPE_RECEIVED_AUDIO
import com.example.qchat.utils.Constant.VIEW_TYPE_RECEIVED_DOCUMENT
import com.example.qchat.utils.Constant.VIEW_TYPE_RECEIVED_PHOTO
import com.example.qchat.utils.Constant.VIEW_TYPE_SEND
import com.example.qchat.utils.Constant.VIEW_TYPE_SEND_PHOTO
import com.example.qchat.utils.Constant.VIEW_TYPE_SEND_LOCATION
import com.example.qchat.utils.Constant.VIEW_TYPE_RECEIVED_LOCATION
import com.example.qchat.utils.Constant.VIEW_TYPE_SEND_DOCUMENT
import com.example.qchat.utils.Constant.VIEW_TYPE_SEND_VIDEO
import com.example.qchat.utils.Constant.VIEW_TYPE_RECEIVED_VIDEO
import com.example.qchat.utils.Constant.VIEW_TYPE_SEND_AUDIO
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
                Log.d(
                    "SendPhotoViewHolder",
                    "Photo Base64 (first 100 chars): ${message.message.take(100)}"
                ) // Log first 100 characters
                val decodedBytes = Base64.decode(message.message, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                if (bitmap != null) {
                    binding.ivMessageImage.setImageBitmap(bitmap)
                    binding.tvDateTime.text = message.dateTime
                    binding.ivMessageImage.setOnClickListener {
                        openPhoto(message.message)
                    }
                    Log.d("SendPhotoViewHolder", "Successfully decoded photo")
                } else {
                    Log.e("SendPhotoViewHolder", "Failed to decode image!")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("SendPhotoViewHolder", "Error decoding photo message: ${e.message}")
            }
        }
        private fun openPhoto(photoBase64: String) {
            val intent = Intent(itemView.context, PhotoViewerActivity::class.java).apply {
                putExtra("photoBase64", photoBase64)
            }
            itemView.context.startActivity(intent)
        }
    }

    class ReceivedPhotoViewHolder(val binding: ItemReceivedPhotoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun setData(message: ChatMessage, profileImage: Bitmap?) {
            try {
                Log.d(
                    "ReceivedPhotoViewHolder",
                    "Photo Base64 (first 100 chars): ${message.message.take(100)}"
                )

                val decodedBytes = Base64.decode(message.message, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                if (bitmap != null) {
                    binding.ivMessageImage.setImageBitmap(bitmap)
                    binding.tvDateTime.text = message.dateTime
                    profileImage?.let { binding.ivProfile.setImageBitmap(it) }
                    binding.ivMessageImage.setOnClickListener {
                        openPhoto(message.message)
                    }
                    Log.d("ReceivedPhotoViewHolder", "Successfully decoded received photo")
                } else {
                    Log.e("ReceivedPhotoViewHolder", "Failed to decode received image!")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("ReceivedPhotoViewHolder", "Error decoding received photo: ${e.message}")
            }
        }
        private fun openPhoto(photoBase64: String) {
            val intent = Intent(itemView.context, PhotoViewerActivity::class.java).apply {
                putExtra("photoBase64", photoBase64)
            }
            itemView.context.startActivity(intent)
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
                val uri =
                    Uri.parse("geo:${locationParts[0]},${locationParts[1]}?q=${locationParts[0]},${locationParts[1]}")
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
                val uri =
                    Uri.parse("geo:${locationParts[0]},${locationParts[1]}?q=${locationParts[0]},${locationParts[1]}")
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

    class SendVideoViewHolder(val binding: ItemSendVideoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(message: ChatMessage) {
            binding.apply {
                Glide.with(itemView.context)
                    .load(message.thumbnailUrl)
                    .into(binding.ivThumbnail)

                binding.tvDuration.text = message.videoDuration
                binding.tvDateTime.text = message.dateTime


                binding.root.setOnClickListener {
                    openVideo(message.message)
                }
            }
        }

        private fun openVideo(videoUrl: String) {
            val intent = Intent(itemView.context, VideoPlayerActivity::class.java).apply {
                putExtra("videoUrl", videoUrl)
            }
            itemView.context.startActivity(intent)
        }

    }

    class ReceivedVideoViewHolder(val binding: ItemReceivedVideoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(message: ChatMessage, profileImage: Bitmap?) {
            binding.apply {
                profileImage?.let {
                    ivProfile.setImageBitmap(it)
                }

                Glide.with(itemView.context)
                    .load(message.thumbnailUrl)
                    .into(binding.ivThumbnail)

                binding.tvDuration.text = message.videoDuration
                binding.tvDateTime.text = message.dateTime

                binding.root.setOnClickListener {
                    openVideo(message.message)
                }
            }
        }

        private fun openVideo(videoUrl: String) {
            val intent = Intent(itemView.context, VideoPlayerActivity::class.java).apply {
                putExtra("videoUrl", videoUrl)
            }
            itemView.context.startActivity(intent)
        }

    }

    class SendAudioViewHolder(val binding: ItemSendAudioBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var mediaPlayer: MediaPlayer? = null
        private var isPlaying = false
        private var audioDuration = 0
        private var updateProgressRunnable: Runnable? = null

        fun setData(message: ChatMessage) {
            binding.tvDateTime.text = message.dateTime

            binding.btnPlayAudio.setOnClickListener {
                if (!isPlaying) {
                    startPlaying(message.message)
                } else {
                    stopPlaying()
                }
            }
        }

        private fun startPlaying(audioUrl: String) {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(audioUrl)
                    prepare()
                    start()
                    audioDuration = duration
                    setOnCompletionListener {
                        stopPlaying()
                    }
                }

                isPlaying = true
                binding.btnPlayAudio.setImageResource(R.drawable.ic_pause)

                startUpdatingProgress()
            } catch (e: Exception) {
                Log.e("SendAudioViewHolder", "Playback failed: ${e.message}")
            }
        }

        private fun stopPlaying() {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null

            isPlaying = false
            binding.btnPlayAudio.setImageResource(R.drawable.ic_play)

            stopUpdatingProgress()

            // Reset dot position and timer if needed
            binding.progressDot.translationX = 0f
            binding.tvAudioDuration.text = "00:00"
        }

        private fun startUpdatingProgress() {
            updateProgressRunnable = object : Runnable {
                override fun run() {
                    mediaPlayer?.let { player ->
                        val currentPosition = player.currentPosition
                        val progress = (currentPosition.toFloat() / audioDuration.toFloat())
                        val totalWidth = binding.audioWaveform.width.toFloat()

                        binding.progressDot.translationX = totalWidth * progress

                        val minutes = currentPosition / 1000 / 60
                        val seconds = (currentPosition / 1000) % 60
                        binding.tvAudioDuration.text = String.format("%02d:%02d", minutes, seconds)

                        binding.root.postDelayed(this, 500)
                    }
                }
            }
            binding.root.post(updateProgressRunnable!!)
        }

        private fun stopUpdatingProgress() {
            updateProgressRunnable?.let { binding.root.removeCallbacks(it) }
        }
    }


    class ReceivedAudioViewHolder(val binding: ItemReceivedAudioBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var mediaPlayer: MediaPlayer? = null
        private var isPlaying = false
        private var audioDuration = 0
        private var updateProgressRunnable: Runnable? = null

        fun setData(message: ChatMessage, profileImage: Bitmap?) {
            binding.tvDateTime.text = message.dateTime
            profileImage?.let { binding.ivProfile.setImageBitmap(it) }

            binding.btnPlayAudio.setOnClickListener {
                if (!isPlaying) {
                    startPlaying(message.message)
                } else {
                    stopPlaying()
                }
            }
        }

        private fun startPlaying(audioUrl: String) {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(audioUrl)
                    prepare()
                    start()
                    audioDuration = duration
                    setOnCompletionListener {
                        stopPlaying()
                    }
                }

                isPlaying = true
                binding.btnPlayAudio.setImageResource(R.drawable.ic_pause)

                startUpdatingProgress()
            } catch (e: Exception) {
                Log.e("ReceivedAudioViewHolder", "Playback failed: ${e.message}")
            }
        }

        private fun stopPlaying() {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null

            isPlaying = false
            binding.btnPlayAudio.setImageResource(R.drawable.ic_play)

            stopUpdatingProgress()

            binding.progressDot.translationX = 0f
            binding.tvAudioDuration.text = "00:00"
        }

        private fun startUpdatingProgress() {
            updateProgressRunnable = object : Runnable {
                override fun run() {
                    mediaPlayer?.let { player ->
                        val currentPosition = player.currentPosition
                        val progress = (currentPosition.toFloat() / audioDuration.toFloat())
                        val totalWidth = binding.audioWaveform.width.toFloat()

                        binding.progressDot.translationX = totalWidth * progress

                        val minutes = currentPosition / 1000 / 60
                        val seconds = (currentPosition / 1000) % 60
                        binding.tvAudioDuration.text = String.format("%02d:%02d", minutes, seconds)

                        binding.root.postDelayed(this, 500)
                    }
                }
            }
            binding.root.post(updateProgressRunnable!!)
        }

        private fun stopUpdatingProgress() {
            updateProgressRunnable?.let { binding.root.removeCallbacks(it) }
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
            VIEW_TYPE_BLOCKED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_blocked_message, parent, false)
                BlockedMessageViewHolder(view)
            }
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
                SendDocumentViewHolder(
                    ItemSendDocumentBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            VIEW_TYPE_RECEIVED_DOCUMENT -> {
                ReceivedDocumentViewHolder(
                    ItemReceivedDocumentBinding.inflate(
                        LayoutInflater.from(
                            parent.context
                        ), parent, false
                    )
                )
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

            VIEW_TYPE_SEND_AUDIO -> {
                SendAudioViewHolder(
                    ItemSendAudioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                )
            }

            VIEW_TYPE_RECEIVED_AUDIO -> {
                ReceivedAudioViewHolder(
                    ItemReceivedAudioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                )
            }


            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is BlockedMessageViewHolder -> holder.bind(chatMessagesList[position])
            is SendMessageViewHolder -> holder.setData(chatMessagesList[position])
            is ReceivedMessageViewHolder -> holder.setData(chatMessagesList[position], profileImage)
            is SendPhotoViewHolder -> holder.setData(chatMessagesList[position])
            is ReceivedPhotoViewHolder -> holder.setData(chatMessagesList[position], profileImage)
            is SendLocationViewHolder -> holder.setData(chatMessagesList[position])
            is ReceivedLocationViewHolder -> holder.setData(chatMessagesList[position], profileImage)
            is SendDocumentViewHolder -> holder.setData(chatMessagesList[position])
            is ReceivedDocumentViewHolder -> holder.setData(chatMessagesList[position], profileImage, holder.itemView.context)
            is SendVideoViewHolder -> holder.setData(chatMessagesList[position])
            is ReceivedVideoViewHolder -> holder.setData(chatMessagesList[position], profileImage)
            is SendAudioViewHolder -> holder.setData(chatMessagesList[position])
            is ReceivedAudioViewHolder -> holder.setData(chatMessagesList[position], profileImage)

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
            message.messageType == "blocked" -> VIEW_TYPE_BLOCKED
            message.senderId == senderId -> {
                when (message.messageType) {
                    Constant.MESSAGE_TYPE_PHOTO -> VIEW_TYPE_SEND_PHOTO
                    Constant.MESSAGE_TYPE_LOCATION -> VIEW_TYPE_SEND_LOCATION
                    Constant.MESSAGE_TYPE_DOCUMENT -> VIEW_TYPE_SEND_DOCUMENT
                    Constant.MESSAGE_TYPE_VIDEO -> VIEW_TYPE_SEND_VIDEO
                    Constant.MESSAGE_TYPE_AUDIO -> VIEW_TYPE_SEND_AUDIO
                    else -> VIEW_TYPE_SEND
                }
            }
            else -> {
                when (message.messageType) {
                    Constant.MESSAGE_TYPE_PHOTO -> VIEW_TYPE_RECEIVED_PHOTO
                    Constant.MESSAGE_TYPE_LOCATION -> VIEW_TYPE_RECEIVED_LOCATION
                    Constant.MESSAGE_TYPE_DOCUMENT -> VIEW_TYPE_RECEIVED_DOCUMENT
                    Constant.MESSAGE_TYPE_VIDEO -> VIEW_TYPE_RECEIVED_VIDEO
                    Constant.MESSAGE_TYPE_AUDIO -> VIEW_TYPE_RECEIVED_AUDIO // ✅ FIXED!
                    else -> VIEW_TYPE_RECEIVED
                }
            }

        }
    }

    class SendDocumentViewHolder(val binding: ItemSendDocumentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(message: ChatMessage) {
            binding.apply {
                tvDocumentName.text = message.documentName ?: "Unnamed"
                tvDateTime.text = message.dateTime

                root.setOnClickListener {
                    openDocument(message.message, message.documentName ?: "document.pdf")
                }
            }
        }

        private fun openDocument(documentUrl: String, documentName: String) {
            val intent = Intent(itemView.context, PdfRendererActivity::class.java).apply {
                putExtra("documentUrl", documentUrl)
                putExtra("documentName", documentName)
            }
            itemView.context.startActivity(intent)
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
                    openDocument(message.message, message.documentName ?: "document.pdf", context)
                }
            }
        }

        private fun openDocument(documentUrl: String, documentName: String, context: Context) {
            val intent = Intent(context, PdfRendererActivity::class.java).apply {
                putExtra("documentUrl", documentUrl)
                putExtra("documentName", documentName)
            }
            context.startActivity(intent)
        }
    }
}
