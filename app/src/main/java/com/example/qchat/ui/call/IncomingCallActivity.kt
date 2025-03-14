package com.example.qchat.ui.call

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.qchat.R
import com.example.qchat.model.User
import com.example.qchat.utils.Constant

class IncomingCallActivity : AppCompatActivity() {

    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incomig_call)

        val callerName = intent.getStringExtra(Constant.KEY_CALLER_NAME)
        val callType = intent.getStringExtra(Constant.KEY_CALL_TYPE)

        val tvCallerName = findViewById<TextView>(R.id.caller_name)
        val tvCallType = findViewById<TextView>(R.id.call_status)
        val btnAcceptCall = findViewById<Button>(R.id.accept_button)
        val btnDeclineCall = findViewById<Button>(R.id.reject_button)

        tvCallerName.text = callerName
        tvCallType.text = if (callType == "video") "Video Call" else "Voice Call"

        btnAcceptCall.setOnClickListener {
            // Start the appropriate call activity (Voice or Video)
            val targetActivity = if (callType == "video") VideoCallActivity::class.java else VoiceCallActivity::class.java
            val intent = Intent(this, targetActivity).apply {
                // Pass caller/receiver details
                putExtra(Constant.KEY_CALLER_NAME, callerName)
                putExtra(Constant.KEY_CALL_TYPE, callType)
            }
            startActivity(intent)
            finish() // Close incoming call screen
        }

        btnDeclineCall.setOnClickListener {
            finish() // Dismiss the incoming call screen
        }
    }
}
