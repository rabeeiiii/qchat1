package com.example.qchat.ui.users

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.qchat.R
import com.example.qchat.adapter.UsersAdapter
import com.example.qchat.databinding.UsersFragmentBinding
import com.example.qchat.utils.Constant
import com.example.qchat.utils.Resource
import com.example.qchat.utils.gone
import com.example.qchat.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class UsersFragment : Fragment(R.layout.users_fragment) {

    private lateinit var binding: UsersFragmentBinding
    private val viewModel: UsersViewModel by viewModels()

    @Inject
    lateinit var userAdapter: UsersAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = UsersFragmentBinding.bind(view)

        setUpRecyclerview()
        setObserver()
        setClickListener()

        userAdapter.onUserClick = {
            val bundle = Bundle()
            bundle.putSerializable(Constant.KEY_USER, it)
            findNavController().navigate(R.id.action_usersFragment_to_chatFragment, bundle)
        }
    }

    private fun setClickListener() {
        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
    }

    private fun setObserver() {
        viewModel.usersList.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    binding.pb.gone()
                    binding.rvUsers.visible()
                    it.data?.let { list ->
                        if (list.isNotEmpty()) {
                            userAdapter.submitList(list)
                        }
                    }

                }
                is Resource.Error -> {
                    binding.pb.visible()
                    binding.rvUsers.gone()
                    binding.tvErrorMessage.text = it.message
                    binding.tvErrorMessage.visible()
                }
                is Resource.Loading -> {
                    binding.pb.visible()
                    binding.tvErrorMessage.gone()
                    binding.rvUsers.gone()
                }
                is Resource.Empty -> {
                    binding.pb.gone()
                    binding.tvErrorMessage.text = it.message ?: ""
                    binding.tvErrorMessage.visible()
                    binding.rvUsers.gone()
                }
            }
        }
    }

    private fun setUpRecyclerview() = binding.rvUsers.apply {
        adapter = userAdapter
    }

}