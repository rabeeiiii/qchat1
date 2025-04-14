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
import com.example.qchat.model.User
import com.example.qchat.utils.Constant
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GroupChatFragment : Fragment() {

    private var _binding: FragmentGroupChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GroupViewModel by viewModels()
    private lateinit var messagesAdapter: GroupMessagesAdapter
    private lateinit var group: Group

    @Inject
    lateinit var usersAdapter: UsersAdapter

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
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        loadGroupMessages()
        hideUnnecessaryComponents()
    }

    private fun hideUnnecessaryComponents() {
        // Hide the create group FAB and other unnecessary components
        activity?.findViewById<View>(R.id.fabCreateGroup)?.visibility = View.GONE
        
        // Set full screen to properly hide background content
        activity?.window?.decorView?.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
        
        // Add a root layout with solid background to prevent see-through
        binding.root.setBackgroundResource(android.R.color.white)
        
        // Log that we've hidden components
        android.util.Log.d("GroupChatFragment", "Hiding unnecessary components")
    }

    private fun setupRecyclerView() {
        messagesAdapter = GroupMessagesAdapter()
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
                    viewModel.sendMessage(message, group.id)
                    editTextMessage.text?.clear()
                }
            }

            imageViewAttachment.setOnClickListener {
                // TODO: Implement attachment functionality
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.groupMessages.collectLatest { messages ->
                messagesAdapter.submitList(messages)
                binding.recyclerViewMessages.scrollToPosition(messages.size - 1)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collectLatest { errorMessage ->
                // Show error message to user
                // You can use Snackbar or Toast here
            }
        }
    }

    private fun loadGroupMessages() {
        viewModel.loadGroupMessages(group.id)
        updateGroupInfo()
    }

    private fun updateGroupInfo() {
        binding.apply {
            textViewGroupName.text = group.name
            textViewMemberCount.text = "${group.members.size} members"
            
            Glide.with(requireContext())
                .load(group.image)
                .circleCrop()
                .into(imageViewGroup)
        }
    }

    private fun showGroupOptionsMenu() {
        val options = arrayOf("View Members", "Group Info", "Leave Group")
        MaterialAlertDialogBuilder(requireContext())
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
        val membersView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_group_members, null)

        val recyclerView = membersView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerViewMembers)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = usersAdapter

        // Load and display group members
        viewModel.loadGroupMembers(group.id)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Group Members")
            .setView(membersView)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showGroupInfo() {
        val infoView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_group_info, null)

        // Set group information
        infoView.findViewById<android.widget.TextView>(R.id.textViewGroupName).text = group.name
        infoView.findViewById<android.widget.TextView>(R.id.textViewDescription).text = group.description
        infoView.findViewById<android.widget.TextView>(R.id.textViewCreatedBy).text = "Created by: ${group.createdBy}"
        infoView.findViewById<android.widget.TextView>(R.id.textViewMemberCount).text = "${group.members.size} members"

        MaterialAlertDialogBuilder(requireContext())
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
        super.onDestroyView()
        _binding = null
    }
} 