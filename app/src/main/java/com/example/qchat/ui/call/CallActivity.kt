package com.example.qchat.ui.call

import SignalingClient
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.qchat.R
import com.example.qchat.model.User
import com.example.qchat.utils.decodeToBitmap
import com.example.qchat.webrtc.WebRtcManager
import org.webrtc.EglBase

class CallActivity : AppCompatActivity() {

    private lateinit var ivUserImage: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var btnEndCall: Button
    private lateinit var eglBase: EglBase
    private var webRtcManager: WebRtcManager? = null
    private var isVideoCall: Boolean = false // Track if it's a video call

    // Add signalingClient as a property
    private lateinit var signalingClient: SignalingClient

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call) // Use the appropriate layout

        ivUserImage = findViewById(R.id.ivUserImage)
        tvUserName = findViewById(R.id.tvName)
        btnEndCall = findViewById(R.id.btnEndCall)

        // Initialize signalingClient
        val userId = "currentUserId" // Replace with the actual current user ID
        val remoteUserId = "remoteUserId" // Replace with the actual remote user ID
        signalingClient = SignalingClient(userId, remoteUserId)

        val user = intent.getSerializableExtra("USER", User::class.java)
        isVideoCall = intent.getBooleanExtra("IS_VIDEO_CALL", false) // Check if it's a video call

        if (user != null) {
            tvUserName.text = user.name
            user.image?.decodeToBitmap()?.let {
                ivUserImage.setImageBitmap(it)
            }
        }

        btnEndCall.setOnClickListener {
            // End the call
            endCall()
        }

        if (isVideoCall) {
            if (user != null) {
                initializeVideoCall(user)
            }
        } else {
            if (user != null) {
                initializeVoiceCall(user)
            }
        }
    }

    private fun initializeVideoCall(remoteUser: User) {
        eglBase = EglBase.create()
        webRtcManager = WebRtcManager(
            context = this,
            signalingClient = signalingClient, // Pass the signaling client
            localVideoRenderer = findViewById(R.id.localVideoView),
            remoteVideoRenderer = findViewById(R.id.remoteVideoView)
        )
        webRtcManager?.setupLocalMediaTracks() // Setup for video
        webRtcManager?.createPeerConnection()
        webRtcManager?.startCall(isAudioOnly = false) // Start the video call
    }

    private fun initializeVoiceCall(remoteUser: User) {
        webRtcManager = WebRtcManager(
            context = this,
            signalingClient = signalingClient, // Pass the signaling client
            localVideoRenderer = null,
            remoteVideoRenderer = null
        )
        webRtcManager?.setupLocalMediaTracks(isAudioOnly = true) // Only audio
        webRtcManager?.createPeerConnection()
        webRtcManager?.startCall(isAudioOnly = true) // Start the voice call
    }

    private fun endCall() {
        try {
            // End the WebRTC call and release resources
            webRtcManager?.endCall()
            webRtcManager = null
            eglBase.release() // Release EGL context if video call
        } catch (e: Exception) {
            Log.e("CallActivity", "Error during WebRTC cleanup: ${e.message}")
        }
        finish() // Close the activity
    }

    override fun onDestroy() {
        super.onDestroy()
        endCall() // Ensure cleanup on activity destroy
    }
}