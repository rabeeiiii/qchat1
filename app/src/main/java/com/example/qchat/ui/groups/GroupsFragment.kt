package com.example.qchat.ui.groups

import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.qchat.R
import com.example.qchat.adapter.GroupsAdapter
import com.example.qchat.adapter.UsersAdapter
import com.example.qchat.databinding.FragmentGroupsBinding
import com.example.qchat.model.Group
import com.example.qchat.model.User
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GroupsFragment : Fragment() {

    private var _binding: FragmentGroupsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GroupViewModel by viewModels()
    private lateinit var groupsAdapter: GroupsAdapter

    @Inject
    lateinit var usersAdapter: UsersAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        // Load users when fragment is created
        viewModel.loadUsers()
    }

    private fun setupRecyclerView() {
        groupsAdapter = GroupsAdapter { group ->
            // Navigate to group chat using fragment transaction
            val groupChatFragment = GroupChatFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("group", group)
                }
            }
            parentFragmentManager.beginTransaction()
                .replace(android.R.id.content, groupChatFragment)
                .addToBackStack(null)
                .commit()
        }

        binding.recyclerViewGroups.apply {
            adapter = groupsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupClickListeners() {
        binding.fabCreateGroup.setOnClickListener {
            showCreateGroupDialog()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.groups.collectLatest { groups ->
                groupsAdapter.submitList(groups)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.groupMembers.collectLatest { users ->
                usersAdapter.submitList(users)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collectLatest { errorMessage ->
                com.google.android.material.snackbar.Snackbar.make(
                    binding.root,
                    errorMessage,
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }
    private fun showCreateGroupDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(com.example.qchat.R.layout.dialog_create_group, null)

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.MyApp_DialogTheme)
            .setTitle("Create New Group")
            .setView(dialogView)
            .setPositiveButton("Next", null) // Set to null first
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            // Apply margins programmatically
            (negativeButton.layoutParams as? LinearLayout.LayoutParams)?.apply {
                marginEnd = resources.getDimensionPixelSize(R.dimen.button_margin_end)
            }

            positiveButton.setOnClickListener {
                val nameInput = dialogView.findViewById<TextInputEditText>(com.example.qchat.R.id.editTextGroupName)
                val descriptionInput = dialogView.findViewById<TextInputEditText>(com.example.qchat.R.id.editTextGroupDescription)
                val name = nameInput.text?.toString()?.trim()
                val description = descriptionInput.text?.toString()?.trim()

                if (!name.isNullOrEmpty()) {
                    showMemberSelectionDialog(name, description ?: "")
                    dialog.dismiss()
                }
            }

            negativeButton.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }
    private fun showMemberSelectionDialog(groupName: String, groupDescription: String) {
        // Create the dialog view
        val memberSelectionView = LayoutInflater.from(requireContext())
            .inflate(com.example.qchat.R.layout.dialog_select_members, null)

        val recyclerView = memberSelectionView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerViewUsers);
        val progressBar = memberSelectionView.findViewById<android.widget.ProgressBar>(R.id.progressBar);
        val textViewError = memberSelectionView.findViewById<android.widget.TextView>(R.id.textViewError);
        // Show loading
        progressBar?.visibility = View.VISIBLE
        recyclerView?.visibility = View.GONE
        textViewError?.visibility = View.GONE

        recyclerView?.layoutManager = LinearLayoutManager(requireContext())

        // Clear any previous selection
        usersAdapter.clearSelections()
        recyclerView?.adapter = usersAdapter

        // Create and show the dialog
        val dialog = MaterialAlertDialogBuilder(requireContext() , R.style.MyApp_selectusersTheme)
            .setTitle("Select Members")
            .setView(memberSelectionView)
            .setPositiveButton("Create") { dialog, _ ->
                val selectedUsers = usersAdapter.getSelectedUsers()
                android.util.Log.d("GroupsFragment", "Selected ${selectedUsers.size} users for group creation")
                if (selectedUsers.isNotEmpty()) {
                    viewModel.createGroup(groupName, groupDescription, selectedUsers.map { it.id })
                } else {
                    com.google.android.material.snackbar.Snackbar.make(
                        binding.root,
                        "Please select at least one member",
                        com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                    ).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            // Apply button styling
            positiveButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.signal_blue))
            negativeButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))

            // Center buttons
            (negativeButton.parent as? LinearLayout)?.gravity = Gravity.CENTER
        }
        dialog.show()

        // Load users after dialog is shown
        viewModel.loadUsers()

        // Observe users changes
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.groupMembers.collectLatest { users ->
                // Update UI based on users list
                progressBar?.visibility = View.GONE

                if (users.isEmpty()) {
                    textViewError?.visibility = View.VISIBLE
                    textViewError?.text = "No users available"
                    recyclerView?.visibility = View.GONE
                } else {
                    textViewError?.visibility = View.GONE
                    recyclerView?.visibility = View.VISIBLE
                    usersAdapter.submitList(users)
                }

                android.util.Log.d("GroupsFragment", "Updated users in dialog: ${users.size}")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 