package com.example.qchat.ui.groups

import android.os.Bundle
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
import androidx.core.content.ContentProviderCompat.requireContext
import com.example.qchat.utils.decodeToBitmap
import kotlinx.coroutines.flow.distinctUntilChanged

@AndroidEntryPoint
class GroupChatFragment : Fragment() {

    private var _binding: FragmentGroupChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GroupViewModel by viewModels()
    private lateinit var group: Group
    
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
    }


    override fun onResume() {
        super.onResume()
        
        // Force reload messages when resuming
//        messagesAdapter.clearMessages()
        loadGroupMessages()
        
        Log.d("GroupChatFragment", "Fragment resumed, reloading messages")
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