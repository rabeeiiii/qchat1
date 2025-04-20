package com.example.qchat.ui.settings

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.qchat.databinding.ActivityInviteFriendBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InviteFriendActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInviteFriendBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInviteFriendBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Invite Friends"
    }

    private fun setupClickListeners() {
        binding.btnShareApp.setOnClickListener {
            shareApp()
        }

        binding.btnCopyLink.setOnClickListener {
            copyInviteLink()
        }
    }

    private fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Check out QChat!")
            putExtra(Intent.EXTRA_TEXT, "Hey! Join me on QChat - the best messaging app! Download it from: https://play.google.com/store/apps/details?id=com.example.qchat")
        }
        startActivity(Intent.createChooser(shareIntent, "Share QChat"))
    }

    private fun copyInviteLink() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("QChat Invite Link", "https://play.google.com/store/apps/details?id=com.example.qchat")
        clipboard.setPrimaryClip(clip)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 