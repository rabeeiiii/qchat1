package com.example.qchat.adapter

import android.util.Log
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
import android.util.Base64

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

                textViewLastMessage.text = when (group.lastMessageType) {
                    "text" -> {
                        try {
                            if (!group.lastMessage.isNullOrEmpty() && !group.aesKey.isNullOrEmpty()) {
                                // Check if it's a valid encrypted string (base64, long enough to contain IV)
                                if (group.lastMessage.length > 24 && Base64.DEFAULT != -1)
                                {
                                    val secretKey = com.example.qchat.utils.AesUtils.base64ToKey(group.aesKey)
                                    val decrypted = com.example.qchat.utils.AesUtils.decryptGroupMessage(group.lastMessage, secretKey)
                                    if (decrypted.isNullOrBlank()) "Text message" else decrypted
                                } else {
                                    group.lastMessage // assume it's plain text
                                }
                            } else {
                                "Text message"
                            }
                        } catch (e: Exception) {
                            Log.e("GroupsAdapter", "Decryption failed", e)
                            "Text message"
                        }
                    }



                    "photo" -> "📷 Photo"
                    "location" -> "📍 Location"
                    "document" -> "📄 Document"
                    "video" -> "\uD83D\uDCF9 Video"
                    "audio" -> "\uD83C\uDF99\uFE0F Audio"
                    else -> "New message"
                }

                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                textViewLastMessageTime.text = if (group.lastMessageTime > 0) {
                    timeFormat.format(Date(group.lastMessageTime))
                } else {
                    ""
                }

                if (!group.image.isNullOrEmpty()) {
                    try {
                        val bitmap = group.image.decodeToBitmap()
                        imageViewGroup.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        imageViewGroup.setImageResource(R.drawable.group)
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