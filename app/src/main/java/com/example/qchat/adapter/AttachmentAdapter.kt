package com.example.qchat.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.qchat.R

class AttachmentAdapter(
    private val context: Context,
    private val items: Array<String>,
    private val images: Array<Int>
) : BaseAdapter() {

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Any = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = LayoutInflater.from(context)
        val view = convertView ?: inflater.inflate(R.layout.grid_item, parent, false)

        val textView: TextView = view.findViewById(R.id.textLabel)
        val imageView: ImageView = view.findViewById(R.id.imageIcon)

        textView.text = items[position]
        imageView.setImageResource(images[position])

        return view
    }
}
