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

        getArgument()
        setClickListener()
        setRecyclerview()

        binding.tvName.text = user.name
        binding.ivUserImage.setImageBitmap(user.image?.decodeToBitmap())

        observeChat()

        // Set OnClickListener for the pop menu
        binding.ivAdd.setOnClickListener {
            togglePopMenuVisibility()
        }

        // Set OnClickListener for the username to navigate to the profile
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
        viewModel.eventListener(user.id, object : ChatObserver {
            override fun observeChat(newChat: List<ChatMessage>) {
                if (newChat.isNotEmpty()) {
                    chatAdapter.addMessage(newChat, binding.rvChat)
                }
                binding.pb.visibility = View.GONE
                viewModel.conversionId.isEmpty().let {
                    if (chatAdapter.getMessageSize() != 0) {
                        viewModel.checkForConversation(user.id)
                    }
                }
            }
        })
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
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupPopMenu() {
        val adapter = AttachmentAdapter(
            requireContext(),
            arrayOf("Photos", "Camera", "Location"),
            arrayOf(R.drawable.gallery, R.drawable.camera, R.drawable.location)
        )
        binding.gridView.adapter = adapter

        binding.gridView.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> openGallery()
                1 -> openCamera()
                2 -> openLocationPicker()
            }
            togglePopMenuVisibility()
        }
    }

    private fun togglePopMenuVisibility() {
        if (binding.popMenuLayout.isVisible) {
            binding.popMenuLayout.visibility = View.GONE
        } else {
            binding.popMenuLayout.visibility = View.VISIBLE
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*") // Open gallery to select an image
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivity(intent)
    }

    private fun openLocationPicker() {
        // Implement your location picker functionality here
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
