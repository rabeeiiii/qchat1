package com.example.qchat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.qchat.R
import com.example.qchat.model.SettingItem

class SettingsAdapter(
    private val items: List<SettingItem>,
    private val onItemClick: (SettingItem) -> Unit // Pass a click listener
) : RecyclerView.Adapter<SettingsAdapter.SettingViewHolder>() {

    class SettingViewHolder(view: View, private val onItemClick: (SettingItem) -> Unit) : RecyclerView.ViewHolder(view) {
        private val icon: ImageView = view.findViewById(R.id.icon)
        private val title: TextView = view.findViewById(R.id.title)
        private val subtitle: TextView = view.findViewById(R.id.subtitle)

        fun bind(item: SettingItem) {
            icon.setImageResource(item.icon)
            title.text = item.title
            subtitle.text = item.subtitle

            // Change color for "Sign Out" item
            if (item.title == "Sign Out") {
                title.setTextColor(ContextCompat.getColor(itemView.context, R.color.red))
                subtitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.red))
                icon.setColorFilter(ContextCompat.getColor(itemView.context, R.color.red))
            } else {
                // Reset to default colors if needed
                title.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
                subtitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
            }

            itemView.setOnClickListener { onItemClick(item) } // Call the listener when the item is clicked
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_settings_option, parent, false)
        return SettingViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: SettingViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size
}
