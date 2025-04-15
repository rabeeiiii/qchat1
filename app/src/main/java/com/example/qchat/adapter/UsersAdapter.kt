package com.example.qchat.adapter

import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.qchat.R
import com.example.qchat.databinding.ItemContainerUserBinding
import com.example.qchat.model.User
import javax.inject.Inject

class UsersAdapter @Inject constructor() : ListAdapter<User, UsersAdapter.UserViewHolder>(UserDiffCallback()) {

    private val selectedUsers = mutableSetOf<User>()
    var onUserClick: ((User) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemContainerUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(private val binding: ItemContainerUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.apply {
                textName.text = user.name
                textEmail.text = user.email
                    try {
                        // Handle both URL and base64 images

                            // Assume it's base64 if not URL
                            val imageBytes = Base64.decode(user.image, Base64.DEFAULT)
                            Glide.with(imageProfile.context)
                                .load(imageBytes)
                                .circleCrop()
                                .into(imageProfile)

                    } catch (e: Exception) {
                        Log.e("UsersAdapter", "Error loading image", e)
                    }
//
//
//                Glide.with(imageProfile.context)
//                    .load(user.image)
//                    .circleCrop()
//                    .into(imageProfile)

                val isSelected = selectedUsers.contains(user)
                root.isSelected = isSelected
                root.setBackgroundResource(
                    if (isSelected) android.R.color.holo_blue_light 
                    else android.R.color.transparent
                )

                android.util.Log.d("UsersAdapter", "Binding user: ${user.name}, id: ${user.id}, selected: $isSelected")

                root.setOnClickListener {
                    if (selectedUsers.contains(user)) {
                        selectedUsers.remove(user)
                        root.isSelected = false
                        root.setBackgroundResource(android.R.color.transparent)
                    } else {
                        selectedUsers.add(user)
                        root.isSelected = true
                        root.setBackgroundResource(R.color.light_gray)
                    }
                    onUserClick?.invoke(user)
                    android.util.Log.d("UsersAdapter", "User clicked: ${user.name}, selected: ${selectedUsers.contains(user)}")
                }
            }
        }
    }

    fun getSelectedUsers(): List<User> = selectedUsers.toList()

    fun clearSelections() {
        selectedUsers.clear()
        notifyDataSetChanged()
    }

    private class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}