package com.example.qchat.ui.call

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.qchat.R
import com.example.qchat.model.User
import com.example.qchat.utils.Constant

class VoiceCallActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_call)

        val remoteUser = intent.getSerializableExtra(Constant.KEY_REMOTE_USER) as? User
        if (remoteUser == null) {
            Log.e("VoiceCallActivity", "Remote user is null")
            Toast.makeText(this, "Failed to start the call. Remote user is missing.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Start CallActivity
        val intent = Intent(this, CallActivity::class.java).apply {
            putExtra("USER", remoteUser)
            putExtra("IS_VIDEO_CALL", false) // Set to false for voice call
        }
        startActivity(intent)
        finish() // Close this activity
    }
}