package com.example.qchat.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.qchat.databinding.ActivityNotificationsSettingsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NotificationsSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationsSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Notifications Settings"
    }

    private fun setupClickListeners() {
        binding.switchMessageNotifications.setOnCheckedChangeListener { _, isChecked ->
        }

        binding.switchGroupNotifications.setOnCheckedChangeListener { _, isChecked ->
        }

        binding.switchSound.setOnCheckedChangeListener { _, isChecked ->
        }

        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 