package com.example.qchat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qchat.model.ChatMessage

class chatActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val chatList = listOf(
            ChatMessage("Hi", "2:31 PM", true),
            ChatMessage("Eh ya joe?", "2:31 PM", false),
            ChatMessage("i Like the UI", "2:31 PM", true),
            ChatMessage("Ah gameel awy", "3:01 PM", false)
        )

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView_chat)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ChatAdapter(this, chatList)
    }
}
