package com.example.qchat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.qchat.databinding.ItemGroupBinding
import com.example.qchat.model.Group
import com.example.qchat.utils.getReadableDate
import java.text.SimpleDateFormat
import java.util.*

class GroupsAdapter(
    private val onGroupClick: (Group) -> Unit
) : ListAdapter<Group, GroupsAdapter.GroupViewHolder>(GroupDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemGroupBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GroupViewHolder(
        private val binding: ItemGroupBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onGroupClick(getItem(position))
                }
            }
        }

        fun bind(group: Group) {
            binding.apply {
                textViewGroupName.text = group.name
                textViewLastMessage.text = group.lastMessage ?: "No messages yet"
                
                // Format the last message time
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                textViewLastMessageTime.text = if (group.lastMessageTime > 0) {
                    timeFormat.format(Date(group.lastMessageTime))
                } else {
                    ""
                }

                // Load group image using Glide
                Glide.with(imageViewGroup)
                    .load(group.image)
                    .circleCrop()
                    .into(imageViewGroup)
            }
        }
    }

    private class GroupDiffCallback : DiffUtil.ItemCallback<Group>() {
        override fun areItemsTheSame(oldItem: Group, newItem: Group): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Group, newItem: Group): Boolean {
            return oldItem == newItem
        }
    }
} 