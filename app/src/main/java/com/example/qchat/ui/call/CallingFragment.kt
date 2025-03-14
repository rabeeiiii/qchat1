package com.example.qchat.ui.chat

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.qchat.R
import com.example.qchat.model.User
import com.example.qchat.ui.call.CallActivity
import com.example.qchat.utils.Constant

class CallingFragment : Fragment() {

    private lateinit var tvCallStatus: TextView
    private lateinit var tvCallerName: TextView
    private lateinit var btnMute: Button
    private lateinit var btnEndCall: Button
    private var currentUser: User? = null // Assuming you have a way to set/get the current user

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_ongoing_call, container, false)

        tvCallerName = view.findViewById(R.id.ongoing_caller_name)
        btnMute = view.findViewById(R.id.mute_button)
        btnEndCall = view.findViewById(R.id.end_call_button)

        // Example to set caller name dynamically
        tvCallerName.text = currentUser?.name ?: "Unknown" // Dynamically set user name

        btnEndCall.setOnClickListener {
            // Start CallActivity
            val intent = Intent(activity, CallActivity::class.java).apply {
                putExtra("USER", currentUser) // Pass User object here
                putExtra("IS_VIDEO_CALL", false) // Set to true if it's a video call
            }
            startActivity(intent)
            activity?.finish() // Close this fragment/activity
        }

        return view
    }
}