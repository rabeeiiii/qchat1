package com.example.qchat.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.qchat.databinding.ActivityChatSettingsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Chat Settings"
    }

    private fun setupClickListeners() {
        binding.switchBackup.setOnCheckedChangeListener { _, isChecked ->
        }

        binding.switchPreview.setOnCheckedChangeListener { _, isChecked ->
        }

        binding.switchMedia.setOnCheckedChangeListener { _, isChecked ->
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 