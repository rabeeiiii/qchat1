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
import androidx.core.location.LocationManagerCompat.getCurrentLocation
import com.example.qchat.utils.AesUtils
import com.google.android.gms.location.*
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

    // Request permission
    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getCurrentLocation()
        } else {
            Log.e("ChatFragment", "Location permission denied")
        }
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

        // Initialize the gallery launcher
        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val bitmap = uriToBitmap(uri)
                bitmap?.let {
                    encodeBitmapToBase64(it) { encodedImage ->
                        viewModel.sendPhoto(encodedImage, user) // Send the encoded image as a message
                    }
                }
            }
        }
        videoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                Log.d("ChatFragment", "Selected video URI: $uri")
                handleVideoSelection(uri)
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
                val resizedBitmap = resizeBitmap(bitmap, 800, 800) // Resize the image for optimization
                val byteArrayOutputStream = ByteArrayOutputStream()
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream) // Compress the image
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

    private fun setupPopMenu() {
        val adapter = AttachmentAdapter(
            requireContext(),
            arrayOf("Photos", "Camera", "Video", "Location"),
            arrayOf(R.drawable.gallery, R.drawable.camera, R.drawable.ic_play, R.drawable.location)
        )
        binding.gridView.adapter = adapter

        binding.gridView.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> openGallery()
                1 -> openCamera()
                2 -> openVideoPicker()
                3 -> openLocationPicker()
            }
            togglePopMenuVisibility()
        }
    }
    private fun openVideoPicker() {
        videoLauncher.launch("video/*")
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
                    // Request new location update if last location is null
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

    private fun handleVideoSelection(videoUri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            val thumbnail = generateVideoThumbnail(videoUri)
            val encryptedVideoUri = encryptVideo(videoUri)

            withContext(Dispatchers.Main) {
                if (encryptedVideoUri != null && thumbnail != null) {
                    viewModel.sendVideo(encryptedVideoUri, thumbnail, user)
                } else {
                    Log.e("ChatFragment", "Failed to process video selection")
                }
            }
        }
    }

    private fun generateVideoThumbnail(videoUri: Uri): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(requireContext(), videoUri)
            val bitmap = retriever.getFrameAtTime(1, MediaMetadataRetriever.OPTION_CLOSEST)
            retriever.release()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun encryptVideo(videoUri: Uri): Uri? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(videoUri)
            val byteArray = inputStream?.readBytes() ?: return null
            inputStream.close()

            val encryptedBytes = AesUtils.encryptByteArray(byteArray)

            val encryptedFile = File(requireContext().cacheDir, "encrypted_video.mp4")
            encryptedFile.writeBytes(encryptedBytes)

            Uri.fromFile(encryptedFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }




}
