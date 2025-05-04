package com.example.qchat.ui.chat

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.qchat.R
import com.example.qchat.adapter.AttachmentAdapter
import com.example.qchat.adapter.ChatAdapter
import com.example.qchat.databinding.ChatFragmentBinding
import com.example.qchat.model.ChatMessage
import com.example.qchat.model.User
import com.example.qchat.ui.main.MainActivity
import com.example.qchat.utils.Constant
import com.example.qchat.utils.decodeToBitmap
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.media.MediaMetadataRetriever
import android.os.Looper
import android.widget.Toast
import androidx.core.location.LocationManagerCompat.getCurrentLocation
import com.google.android.gms.location.*
import android.media.MediaRecorder
import android.media.MediaPlayer
import java.io.File

@AndroidEntryPoint
class ChatFragment : Fragment(R.layout.chat_fragment) {

    private lateinit var mainActivity: MainActivity
    private lateinit var binding: ChatFragmentBinding
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var user: User
    private lateinit var chatAdapter: ChatAdapter
    @Inject
    lateinit var pref: SharedPreferences

    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var documentLauncher: ActivityResultLauncher<String>
    private lateinit var videoLauncher: ActivityResultLauncher<String>

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationRequest = LocationRequest.create().apply {
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        interval = 5000
        fastestInterval = 2000
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                sendCurrentLocation(location)
            }
        }
    }

    private val requestAudioPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startRecording()
        } else {
            Toast.makeText(requireContext(), "Microphone permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getCurrentLocation()
        } else {
            Log.e("ChatFragment", "Location permission denied")
        }
    }

    private var isReceiverAvailable = false
    private var isRecording = false
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var audioPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var currentPlayingPosition = -1
    private var videoUri: Uri? = null
    private var videoDuration: Long = 0
    private var recordingState: RecordingState = RecordingState.IDLE

    private enum class RecordingState {
        IDLE, RECORDING, RECORDED
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mainActivity.setBottomNavigationVisibility(View.GONE)
        return inflater.inflate(R.layout.chat_fragment, container, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mainActivity.setBottomNavigationVisibility(View.VISIBLE)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = ChatFragmentBinding.bind(view)

        binding.ivBack.setOnClickListener {
            findNavController().popBackStack(R.id.mainFragment, false)
        }



        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val bitmap = uriToBitmap(uri)
                bitmap?.let {
                    encodeBitmapToBase64(it) { encodedImage ->
                        viewModel.sendPhoto(encodedImage, user)
                    }
                }
            }
        }

        documentLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                handleDocumentSelection(it)
            }
        }

        videoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { videoUri ->
                handleVideoSelection(videoUri)
            }
        }

        getArgument()
        setClickListener()
        setRecyclerview()

        binding.tvName.text = user.name
        binding.ivUserImage.setImageBitmap(user.image?.decodeToBitmap())

        observeChat()

        binding.ivAdd.setOnClickListener {
            togglePopMenuVisibility()
        }

        binding.ivUserImage.setOnClickListener {
            val action = ChatFragmentDirections.actionChatFragmentToProfileFragment(user)
            findNavController().navigate(action)
        }

        setupPopMenu()
    }

    private fun handleVideoSelection(uri: Uri) {
        lifecycleScope.launch {
            binding.pb.visibility = View.VISIBLE

            try {
                val thumbnail = generateVideoThumbnail(uri)

                val videoBytes = withContext(Dispatchers.IO) {
                    context?.contentResolver?.openInputStream(uri)?.readBytes()
                } ?: return@launch

                val thumbnailBytes = ByteArrayOutputStream().apply {
                    thumbnail.compress(Bitmap.CompressFormat.JPEG, 80, this)
                }.toByteArray()

                viewModel.sendVideo(videoBytes, thumbnailBytes, user)
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to process video", Toast.LENGTH_SHORT).show()
                Log.e("ChatFragment", "Error processing video: ${e.message}")
            } finally {
                binding.pb.visibility = View.GONE
            }
        }
    }

    private fun generateVideoThumbnail(uri: Uri): Bitmap {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, uri)
        val frame = retriever.frameAtTime
        retriever.release()
        return frame ?: throw Exception("Failed to generate thumbnail")
    }

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(requireContext().contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun encodeBitmapToBase64(bitmap: Bitmap, callback: (String) -> Unit) {
        lifecycleScope.launch {
            withContext(Dispatchers.Default) {
                val resizedBitmap = resizeBitmap(bitmap, 800, 800)
                val byteArrayOutputStream = ByteArrayOutputStream()
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()
                val encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT)
                withContext(Dispatchers.Main) {
                    callback(encodedImage)
                }
            }
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) return bitmap

        val aspectRatio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int

        if (aspectRatio > 1) {
            targetWidth = maxWidth
            targetHeight = (maxWidth / aspectRatio).toInt()
        } else {
            targetHeight = maxHeight
            targetWidth = (maxHeight * aspectRatio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun getArgument() {
        arguments?.let {
            user = ChatFragmentArgs.fromBundle(it).user
        }
    }

    private fun observeChat() {
        binding.pb.visibility = View.VISIBLE

        viewModel.observeChat(user.id) { newChat ->
            Log.d("ChatFragment", "New chat messages received: ${newChat.size}")

            if (newChat.isNotEmpty()) {
                chatAdapter.addMessage(newChat, binding.rvChat)
            }

            binding.pb.visibility = View.GONE

            if (chatAdapter.getMessageSize() != 0) {
                viewModel.checkForConversation(user.id)
            }
        }
    }

    private fun setClickListener() {
        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
        binding.ivSend.setOnClickListener {
            if (binding.etMessage.text.isNullOrBlank()) return@setOnClickListener
            viewModel.sendMessage(binding.etMessage.text.trim().toString(), user)
            binding.etMessage.text.clear()
        }
        binding.ivVoiceMessage.setOnClickListener {
            when (recordingState) {
                RecordingState.IDLE -> startRecording()
                RecordingState.RECORDING -> stopRecording()
                RecordingState.RECORDED -> sendAudioRecording()
            }
        }

    }

    private fun setRecyclerview() {
        chatAdapter = ChatAdapter(pref.getString(Constant.KEY_USER_ID, null).toString(), emptyList())
        user.image?.let {
            chatAdapter.setProfileImage(it.decodeToBitmap())
        }
        binding.rvChat.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(context).apply { stackFromEnd = true }
        }
    }
    private fun startRecording() {
        if (requireContext().checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        try {
            mediaRecorder?.release()
            mediaRecorder = null

            audioFile =
                File(requireContext().cacheDir, "audio_message_${System.currentTimeMillis()}.3gp")
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setOutputFile(audioFile?.absolutePath)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                prepare()
                start()
            }

            isRecording = true
            recordingState = RecordingState.RECORDING
            binding.ivVoiceMessage.setImageResource(R.drawable.stop)

        } catch (e: Exception) {
            Log.e("ChatFragment", "Recording failed: ${e.message}", e)
            Toast.makeText(context, "Recording failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            recordingState = RecordingState.RECORDED

            // Change UI to send button (ready to send)
            binding.ivVoiceMessage.setImageResource(R.drawable.send_voice)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to stop recording: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendAudioRecording() {
        if (audioFile != null && audioFile!!.exists()) {
            val audioBytes = audioFile!!.readBytes()
            val durationMillis = getAudioDuration(audioFile!!)
            viewModel.sendAudio(audioBytes, user, durationMillis)        }

        // After sending, reset UI
        binding.etMessage.visibility = View.VISIBLE
        binding.ivSend.visibility = View.VISIBLE
        binding.ivAdd.visibility = View.VISIBLE

        binding.ivVoiceMessage.setImageResource(R.drawable.ic_mic)
        recordingState = RecordingState.IDLE
    }

    private fun getAudioDuration(file: File): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            retriever.release()
            duration
        } catch (e: Exception) {
            0L
        }
    }
    private fun setupPopMenu() {
        val adapter = AttachmentAdapter(
            requireContext(),
            arrayOf("Photos", "Camera", "Video", "Location", "Document"),
            arrayOf(R.drawable.gallery, R.drawable.camera, R.drawable.videocall, R.drawable.location, R.drawable.document)
        )
        binding.gridView.adapter = adapter

        binding.gridView.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> openGallery()
                1 -> openCamera()
                2 -> openVideoPicker()
                3 -> openLocationPicker()
                4 -> openDocumentPicker()
            }
            togglePopMenuVisibility()
        }
    }

    private fun openDocumentPicker() {
        documentLauncher.launch("application/*")
    }

    private fun openVideoPicker() {
        videoLauncher.launch("video/*")
    }

    private fun handleDocumentSelection(uri: Uri) {
        val contentResolver = requireContext().contentResolver
        val inputStream = contentResolver.openInputStream(uri)
        val fileBytes = inputStream?.readBytes()
        val fileName = getFileNameFromUri(uri)

        fileBytes?.let {
            viewModel.sendDocument(it, user, fileName)
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var name = "document.pdf"
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                if (index >= 0) name = it.getString(index)
            }
        }
        return name
    }

    fun openDocumentFromUrl(documentUrl: String) {
        val uri = Uri.parse(documentUrl)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/pdf")
        startActivity(intent)
    }

    private fun togglePopMenuVisibility() {
        if (binding.popMenuLayout.isVisible) {
            binding.popMenuLayout.visibility = View.GONE
        } else {
            binding.popMenuLayout.visibility = View.VISIBLE
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivity(intent)
    }

    private fun openLocationPicker() {
        if (requireContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        } else {
            requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun getCurrentLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    sendCurrentLocation(location)
                } else {
                    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                }
            }
        } catch (e: SecurityException) {
            Log.e("ChatFragment", "Location access error: ${e.message}")
        }
    }

    private fun sendCurrentLocation(location: Location) {
        val latitude = location.latitude
        val longitude = location.longitude
        viewModel.sendLocation(latitude, longitude, user)
    }

    interface ChatObserver {
        fun observeChat(newChat: List<ChatMessage>)
    }

    override fun onResume() {
        super.onResume()
        viewModel.listenerAvailabilityOfReceiver(user.id) { availability, fcm, profileImage ->
            binding.tvAvailability.isVisible = availability
            user.token = fcm
            if (user.image.isNullOrEmpty()) {
                user.image = profileImage
                binding.ivUserImage.setImageBitmap(user.image?.decodeToBitmap())
                chatAdapter.setProfileImage(user.image?.decodeToBitmap()!!)
            }
        }
    }
}
