package com.example.qchat.adapter

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.util.Base64
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
import android.widget.ImageView
import com.example.qchat.R
import com.example.qchat.adapter.ChatAdapter.ReceivedLocationViewHolder
import com.example.qchat.adapter.ChatAdapter.SendLocationViewHolder
import com.example.qchat.databinding.ItemGroupReceivedAudioBinding
import com.example.qchat.databinding.ItemGroupReceivedDocumentBinding
import com.example.qchat.databinding.ItemGroupReceivedLocationBinding
import com.example.qchat.databinding.ItemGroupReceivedPhotoBinding
import com.example.qchat.databinding.ItemGroupReceivedVideoBinding
import com.example.qchat.databinding.ItemGroupSendAudioBinding
import com.example.qchat.databinding.ItemGroupSendDocumentBinding
import com.example.qchat.databinding.ItemGroupSendLocationBinding
import com.example.qchat.databinding.ItemGroupSendPhotoBinding
import com.example.qchat.databinding.ItemGroupSendVideoBinding
import com.example.qchat.databinding.ItemReceivedAudioBinding
import com.example.qchat.databinding.ItemReceivedLocationBinding
import com.example.qchat.databinding.ItemSendAudioBinding
import com.example.qchat.databinding.ItemSendLocationBinding
import com.example.qchat.model.ChatMessage
import com.example.qchat.ui.chat.PdfRendererActivity
import com.example.qchat.ui.chat.PhotoViewerActivity
import com.example.qchat.ui.chat.VideoPlayerActivity
import com.example.qchat.utils.Constant.VIEW_TYPE_BLOCKED
import com.example.qchat.utils.Constant.VIEW_TYPE_RECEIVED
import com.example.qchat.utils.Constant.VIEW_TYPE_RECEIVED_AUDIO
import com.example.qchat.utils.Constant.VIEW_TYPE_RECEIVED_DOCUMENT
import com.example.qchat.utils.Constant.VIEW_TYPE_RECEIVED_LOCATION
import com.example.qchat.utils.Constant.VIEW_TYPE_RECEIVED_PHOTO
import com.example.qchat.utils.Constant.VIEW_TYPE_RECEIVED_VIDEO
import com.example.qchat.utils.Constant.VIEW_TYPE_SEND
import com.example.qchat.utils.Constant.VIEW_TYPE_SEND_AUDIO
import com.example.qchat.utils.Constant.VIEW_TYPE_SEND_DOCUMENT
import com.example.qchat.utils.Constant.VIEW_TYPE_SEND_LOCATION
import com.example.qchat.utils.Constant.VIEW_TYPE_SEND_PHOTO
import com.example.qchat.utils.Constant.VIEW_TYPE_SEND_VIDEO

class GroupMessagesAdapter @Inject constructor(
    private val preferences: SharedPreferences
) : ListAdapter<GroupMessage, RecyclerView.ViewHolder>(GroupMessageDiffCallback()) {

    private var profileImage: Bitmap? = null

    private val currentUserId: String? 
        get() = preferences.getString(Constant.KEY_USER_ID, null)

    private val messagesList = mutableListOf<GroupMessage>()
    private val messageIds = mutableSetOf<String>()
    private val tempMessageSignatures = mutableSetOf<String>()
    var senderImage: String? = null
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

        return when {
            message.senderId == currentUserId -> {
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
                    Constant.MESSAGE_TYPE_AUDIO -> VIEW_TYPE_RECEIVED_AUDIO
                    else -> VIEW_TYPE_RECEIVED
                }
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {

            VIEW_TYPE_SEND -> {
                SentMessageViewHolder(
                    ItemGroupMessageSentBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            VIEW_TYPE_RECEIVED -> {
                ReceivedMessageViewHolder(
                    ItemGroupMessageReceivedBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            VIEW_TYPE_SEND_PHOTO -> {
                SendPhotoViewHolder(
                    ItemGroupSendPhotoBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            VIEW_TYPE_RECEIVED_PHOTO -> {
                ReceivedPhotoViewHolder(
                    ItemGroupReceivedPhotoBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            VIEW_TYPE_SEND_DOCUMENT -> {
                SendDocumentViewHolder(
                    ItemGroupSendDocumentBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            VIEW_TYPE_RECEIVED_DOCUMENT -> {
                ReceivedDocumentViewHolder(
                    ItemGroupReceivedDocumentBinding.inflate(
                        LayoutInflater.from(
                            parent.context
                        ), parent, false
                    )
                )
            }

            VIEW_TYPE_SEND_VIDEO -> {
                SendVideoViewHolder(
                    ItemGroupSendVideoBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            VIEW_TYPE_RECEIVED_VIDEO -> {
                ReceivedVideoViewHolder(
                    ItemGroupReceivedVideoBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            VIEW_TYPE_SEND_LOCATION -> {
                SendLocationViewHolder(
                    ItemGroupSendLocationBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            VIEW_TYPE_RECEIVED_LOCATION -> {
                ReceivedLocationViewHolder(
                    ItemGroupReceivedLocationBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            VIEW_TYPE_SEND_AUDIO -> {
                SendAudioViewHolder(
                    ItemGroupSendAudioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                )
            }

            VIEW_TYPE_RECEIVED_AUDIO -> {
                ReceivedAudioViewHolder(
                    ItemGroupReceivedAudioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                )
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    private fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        val senderBitmap = message.senderImage?.let { decodeBase64ToBitmap(it) }

        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message, senderBitmap)

            is SendPhotoViewHolder -> holder.setData(message)
            is ReceivedPhotoViewHolder -> holder.setData(message, senderBitmap)

            is SendDocumentViewHolder -> holder.setData(message)
            is ReceivedDocumentViewHolder -> holder.setData(message, senderBitmap, holder.itemView.context)

            is SendVideoViewHolder -> holder.setData(message)
            is ReceivedVideoViewHolder -> holder.setData(message, senderBitmap)

            is SendLocationViewHolder -> holder.setData(message)
            is ReceivedLocationViewHolder -> holder.setData(message, senderBitmap)

            is SendAudioViewHolder -> holder.setData(message)
            is ReceivedAudioViewHolder -> holder.setData(message, senderBitmap)
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

        fun bind(message: GroupMessage, profileImage: Bitmap?) {
            binding.apply {
                textViewMessage.text = message.message
                textViewTime.text = formatTime(message.timestamp)
                textViewSenderName.text = message.senderName
                profileImage?.let { ivProfile.setImageBitmap(it) }
            }
        }

    }
    class SendPhotoViewHolder(val binding: ItemGroupSendPhotoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun setData(message: GroupMessage) {
            try {
                val decodedBytes = Base64.decode(message.message, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                if (bitmap != null) {
                    binding.ivMessageImage.apply {
                        setImageBitmap(bitmap)
                        adjustViewBounds = true
                        scaleType = ImageView.ScaleType.FIT_CENTER
                    }
                    binding.ivMessageImage.setOnClickListener {
                        openPhoto(message.message)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        private fun openPhoto(photoBase64: String) {
            val intent = Intent(itemView.context, PhotoViewerActivity::class.java).apply {
                putExtra("photoBase64", photoBase64)
            }
            itemView.context.startActivity(intent)
        }
    }

    class ReceivedPhotoViewHolder(val binding: ItemGroupReceivedPhotoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun setData(message: GroupMessage, profileImage: Bitmap?) {
            try {
                val decodedBytes = Base64.decode(message.message, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                if (bitmap != null) {
                    binding.ivMessageImage.apply {
                        setImageBitmap(bitmap)
                        adjustViewBounds = true
                        scaleType = ImageView.ScaleType.FIT_CENTER
                    }
                    profileImage?.let { binding.ivProfile.setImageBitmap(it) }
                    binding.ivMessageImage.setOnClickListener {
                        openPhoto(message.message)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        private fun openPhoto(photoBase64: String) {
            val intent = Intent(itemView.context, PhotoViewerActivity::class.java).apply {
                putExtra("photoBase64", photoBase64)
            }
            itemView.context.startActivity(intent)
        }
    }


    class SendVideoViewHolder(val binding: ItemGroupSendVideoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(message: GroupMessage) {
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
            val intent = Intent(itemView.context, VideoPlayerActivity::class.java).apply {
                putExtra("videoUrl", videoUrl)
            }
            itemView.context.startActivity(intent)
        }

    }

    class ReceivedVideoViewHolder(val binding: ItemGroupReceivedVideoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(message: GroupMessage, profileImage: Bitmap?) {
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
            val intent = Intent(itemView.context, VideoPlayerActivity::class.java).apply {
                putExtra("videoUrl", videoUrl)
            }
            itemView.context.startActivity(intent)
        }

    }

    class SendDocumentViewHolder(val binding: ItemGroupSendDocumentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(message: GroupMessage) {
            binding.apply {
                tvDocumentName.text = message.documentName ?: "Unnamed"
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

    class ReceivedDocumentViewHolder(val binding: ItemGroupReceivedDocumentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun setData(message: GroupMessage, profileImage: Bitmap?, context: Context) {
            binding.apply {
                profileImage?.let {
                    ivProfile.setImageBitmap(it)
                }
                tvDocumentName.text = message.documentName ?: "Document"
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
    class SendAudioViewHolder(val binding: ItemGroupSendAudioBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var mediaPlayer: MediaPlayer? = null
        private var isPlaying = false
        private var updateProgressRunnable: Runnable? = null
        private var audioDuration: Long = 0L

        fun setData(message: GroupMessage) {
            // Display the pre-fetched duration
            audioDuration = message.audioDurationInMillis ?: 0L

            val minutes = audioDuration / 1000 / 60
            val seconds = (audioDuration / 1000) % 60
            binding.tvAudioDuration.text = String.format("%02d:%02d", minutes, seconds)

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
                    setOnCompletionListener { stopPlaying() }
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

            binding.progressDot.translationX = 0f

            val minutes = audioDuration / 1000 / 60
            val seconds = (audioDuration / 1000) % 60
            binding.tvAudioDuration.text = String.format("%02d:%02d", minutes, seconds)
        }

        private fun startUpdatingProgress() {
            updateProgressRunnable = object : Runnable {
                override fun run() {
                    mediaPlayer?.let { player ->
                        val currentPosition = player.currentPosition
                        val progress = (currentPosition.toFloat() / audioDuration.toFloat())
                        binding.progressDot.translationX = binding.audioWaveform.width * progress

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

    class ReceivedAudioViewHolder(val binding: ItemGroupReceivedAudioBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var mediaPlayer: MediaPlayer? = null
        private var isPlaying = false
        private var updateProgressRunnable: Runnable? = null
        private var audioDuration: Long = 0L

        fun setData(message: GroupMessage, profileImage: Bitmap?) {
            profileImage?.let { binding.ivProfile.setImageBitmap(it) }

            // Display the pre-fetched duration
            audioDuration = message.audioDurationInMillis ?: 0L
            val minutes = audioDuration / 1000 / 60
            val seconds = (audioDuration / 1000) % 60
            binding.tvAudioDuration.text = String.format("%02d:%02d", minutes, seconds)

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
                    setOnCompletionListener { stopPlaying() }
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

            val minutes = audioDuration / 1000 / 60
            val seconds = (audioDuration / 1000) % 60
            binding.tvAudioDuration.text = String.format("%02d:%02d", minutes, seconds)
        }

        private fun startUpdatingProgress() {
            updateProgressRunnable = object : Runnable {
                override fun run() {
                    mediaPlayer?.let { player ->
                        val currentPosition = player.currentPosition
                        val progress = (currentPosition.toFloat() / audioDuration.toFloat())
                        binding.progressDot.translationX = binding.audioWaveform.width * progress

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

    class SendLocationViewHolder(val binding: ItemGroupSendLocationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun setData(message: GroupMessage) {
            val locationUrl = getStaticMapUrl(message.message)
            Glide.with(binding.root.context)
                .load(locationUrl)
                .placeholder(R.drawable.placeholder_map)
                .into(binding.ivMapPreview)

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

    class ReceivedLocationViewHolder(val binding: ItemGroupReceivedLocationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun setData(message: GroupMessage, profileImage: Bitmap?) {
            val locationUrl = getStaticMapUrl(message.message)
            Glide.with(binding.root.context)
                .load(locationUrl)
                .placeholder(R.drawable.placeholder_map)
                .into(binding.ivMapPreview)

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



    // Call this method when the fragment is destroyed to prevent duplicates when returning
    fun clearMessages() {
        messagesList.clear()
        messageIds.clear()
        tempMessageSignatures.clear()
        super.submitList(emptyList()) // Use super to wipe the diff util state
        Log.d("GroupMessagesAdapter", "Messages cleared")
    }

} 