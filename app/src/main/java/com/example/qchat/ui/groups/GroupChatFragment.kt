package com.example.qchat.ui.groups

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.qchat.R
import com.example.qchat.adapter.GroupMessagesAdapter
import com.example.qchat.adapter.UsersAdapter
import com.example.qchat.databinding.FragmentGroupChatBinding
import com.example.qchat.model.Group
import com.example.qchat.model.GroupMessage
import com.example.qchat.model.User
import com.example.qchat.utils.Constant
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContentProviderCompat.requireContext
import com.example.qchat.adapter.AttachmentAdapter
import com.example.qchat.utils.decodeToBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@AndroidEntryPoint
class GroupChatFragment : Fragment() {

    private var _binding: FragmentGroupChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GroupViewModel by viewModels()
    private lateinit var group: Group

    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var documentLauncher: ActivityResultLauncher<String>
    private lateinit var videoLauncher: ActivityResultLauncher<String>
    
    // Set to keep track of message IDs we've already processed
    private val processedMessageIds = mutableSetOf<String>()

    @Inject
    lateinit var usersAdapter: UsersAdapter
    
    @Inject
    lateinit var messagesAdapter: GroupMessagesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        group = arguments?.getSerializable("group") as Group

        // ✅ Clear old messages from both adapter and ViewModel
        messagesAdapter.clearMessages()
        viewModel.clearGroupMessages()

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        updateGroupInfo()
        hideUnnecessaryComponents()
        loadGroupMessages()

        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val bitmap = uriToBitmap(uri)
                bitmap?.let {
                    encodeBitmapToBase64(it) { encodedImage ->
                        viewModel.sendGroupPhoto(encodedImage, group.id)
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


        binding.imageViewAttachment.setOnClickListener {
            togglePopMenuVisibility()
        }


    }


    override fun onResume() {
        super.onResume()
        
        // Force reload messages when resuming
//        messagesAdapter.clearMessages()
        loadGroupMessages()
        
        Log.d("GroupChatFragment", "Fragment resumed, reloading messages")
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
                1 -> {}
                2 -> openVideoPicker()
                3 -> {}
                4 -> openDocumentPicker()
            }
            togglePopMenuVisibility()
        }
    }

    private fun togglePopMenuVisibility() {
        if (binding.popMenuLayout.visibility == View.VISIBLE) {
            binding.popMenuLayout.visibility = View.GONE
        } else {
            binding.popMenuLayout.visibility = View.VISIBLE
            setupPopMenu()
        }
    }


    private fun openDocumentPicker() {
        documentLauncher.launch("application/*")
    }

    private fun openVideoPicker() {
        videoLauncher.launch("video/*")
    }
    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun handleDocumentSelection(uri: Uri) {
        val contentResolver = requireContext().contentResolver
        val inputStream = contentResolver.openInputStream(uri)
        val fileBytes = inputStream?.readBytes()
        val fileName = getFileNameFromUri(uri)

        fileBytes?.let {
            viewModel.sendGroupDocument(it, fileName, group.id)
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


    private fun handleVideoSelection(uri: Uri) {
        lifecycleScope.launch {

            try {
                val thumbnail = generateVideoThumbnail(uri)

                val videoBytes = withContext(Dispatchers.IO) {
                    context?.contentResolver?.openInputStream(uri)?.readBytes()
                } ?: return@launch

                val thumbnailBytes = ByteArrayOutputStream().apply {
                    thumbnail.compress(Bitmap.CompressFormat.JPEG, 80, this)
                }.toByteArray()

                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(requireContext(), uri)
                val durationMillis = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
                val durationFormatted = String.format("%02d:%02d", durationMillis / 60000, (durationMillis / 1000) % 60)
                retriever.release()

                viewModel.sendGroupVideo(videoBytes, thumbnailBytes, group.id, durationFormatted)

            } catch (e: Exception) {
                Toast.makeText(context, "Failed to process video", Toast.LENGTH_SHORT).show()
                Log.e("GroupChatFragment", "Error processing video: ${e.message}")
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



    private fun hideUnnecessaryComponents() {
        

        
//        activity?.window?.decorView?.systemUiVisibility = (
//            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//        )
//
        
        binding.root.setBackgroundResource(android.R.color.white)

        
        android.util.Log.d("GroupChatFragment", "Hiding unnecessary components")
    }

    private fun setupRecyclerView() {
        binding.recyclerViewMessages.apply {
            adapter = messagesAdapter
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            imageViewBack.setOnClickListener {
                parentFragmentManager.popBackStack()
            }

            imageViewMore.setOnClickListener {
                showGroupOptionsMenu()
            }

            imageViewSend.setOnClickListener {
                val message = editTextMessage.text?.toString()?.trim()
                if (!message.isNullOrEmpty()) {
                    
                    val tempId = "temp_${System.currentTimeMillis()}"


                    viewModel.sendMessage(message, group.id) { newMessage ->
                        val localMessage = newMessage.copy(
                            id = tempId,
                            message = message // show decrypted plain text immediately
                        )
                        messagesAdapter.addMessage(localMessage, binding.recyclerViewMessages)
                    }


                    editTextMessage.text?.clear()
                }
            }

            imageViewAttachment.setOnClickListener {
               
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoadingMessages.collectLatest { isLoading ->
                Log.d("GroupChatFragment", "Loading state: $isLoading")
                binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.groupMessages
                .collectLatest { messages ->
                    Log.d("GroupChatFragment", "Received ${messages.size} messages")
                    messagesAdapter.addMessages(messages, binding.recyclerViewMessages)
                }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collectLatest { errorMessage ->
                Log.e("GroupChatFragment", "Error: $errorMessage")
            }
        }
    }


    private fun loadGroupMessages() {
        viewModel.loadGroupMessages(group.id)
    }

    private fun updateGroupInfo() {
        binding.apply {
            textViewGroupName.text = group.name
            textViewMemberCount.text = "${group.members.size} members"

            if (!group.image.isNullOrEmpty()) {
                try {
                    val bitmap = group.image!!.decodeToBitmap() // using your extension
                    insideimageViewGroup.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    insideimageViewGroup.setImageResource(R.drawable.group) // fallback
                }
            } else {
                insideimageViewGroup.setImageResource(R.drawable.group)
            }
        }
    }

    private fun showGroupOptionsMenu() {
        val options = arrayOf("View Members", "Group Info", "Leave Group")
        MaterialAlertDialogBuilder(requireContext(),R.style.MyApp_selectusersTheme)
            .setTitle("Group Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showMembersList()
                    1 -> showGroupInfo()
                    2 -> showLeaveGroupConfirmation()
                }
            }
            .show()
    }

    private fun showMembersList() {

        viewModel.loadGroupMembers(group.id)
        
        val membersView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_group_members, null)

        val recyclerView = membersView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerViewMembers)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        
       
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.groupMembers.collectLatest { members ->
                usersAdapter.submitList(members)
            }
        }
        
        recyclerView.adapter = usersAdapter

        MaterialAlertDialogBuilder(requireContext(), R.style.MyApp_selectusersTheme)
            .setTitle("Group Members")
            .setView(membersView)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showGroupInfo() {
        val infoView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_group_info, null)

        
        infoView.findViewById<android.widget.TextView>(R.id.textViewGroupName).text = group.name
        infoView.findViewById<android.widget.TextView>(R.id.textViewDescription).text = group.description
        infoView.findViewById<android.widget.TextView>(R.id.textViewMemberCount).text = "${group.members.size} members"
        
       
        viewLifecycleOwner.lifecycleScope.launch {
            
            val creatorTextView = infoView.findViewById<android.widget.TextView>(R.id.textViewCreatedBy)
            creatorTextView.text = "Created by: Loading..."
            
            viewModel.getUserName(group.createdBy) { creatorName ->
                creatorTextView.text = "Created by: $creatorName"
            }
        }

        MaterialAlertDialogBuilder(requireContext(),R.style.MyApp_selectusersTheme)
            .setTitle("Group Information")
            .setView(infoView)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showLeaveGroupConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Leave Group")
            .setMessage("Are you sure you want to leave this group?")
            .setPositiveButton("Leave") { _, _ ->
                val userId = requireContext().getSharedPreferences(Constant.KEY_PREFERENCE_NAME, 0)
                    .getString(Constant.KEY_USER_ID, null)
                if (userId != null) {
                    viewModel.leaveGroup(group.id, userId)
                    parentFragmentManager.popBackStack()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
       
        messagesAdapter.clearMessages()
        
        super.onDestroyView()
        _binding = null
    }
} 