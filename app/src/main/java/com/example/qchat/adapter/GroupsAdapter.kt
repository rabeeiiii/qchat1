package com.example.qchat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.qchat.R
import com.example.qchat.databinding.ItemGroupBinding
import com.example.qchat.model.Group
import com.example.qchat.utils.decodeToBitmap
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

                textViewLastMessage.text = try {
                    if (!group.lastMessage.isNullOrEmpty() && group.aesKey != null) {
                        val secretKey = com.example.qchat.utils.AesUtils.base64ToKey(group.aesKey!!)
                        com.example.qchat.utils.AesUtils.decryptGroupMessage(
                            group.lastMessage,
                            secretKey
                        )
                    } else {
                        "No messages yet"
                    }
                } catch (e: Exception) {
                    "Encrypted message"
                }


                // Format the last message time
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                textViewLastMessageTime.text = if (group.lastMessageTime > 0) {
                    timeFormat.format(Date(group.lastMessageTime))
                } else {
                    ""
                }

                if (!group.image.isNullOrEmpty()) {
                    try {
                        val bitmap =
                            group.image.decodeToBitmap() // uses your existing extension function
                        imageViewGroup.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        imageViewGroup.setImageResource(R.drawable.group) // fallback
                    }
                } else {
                    imageViewGroup.setImageResource(R.drawable.group)
                }
            }
        }

//                // Load group image using Glide
//                Glide.with(imageViewGroup)
//                    .load(group.image)
//                    .circleCrop()
//                    .into(imageViewGroup)
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