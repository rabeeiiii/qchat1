package com.example.qchat.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.qchat.databinding.ActivityStorageSettingsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StorageSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStorageSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStorageSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
        updateStorageInfo()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Storage and Data"
    }

    private fun setupClickListeners() {
        binding.switchMediaAutoDownload.setOnCheckedChangeListener { _, isChecked ->
            // Save media auto-download preference
        }

        binding.switchLowDataUsage.setOnCheckedChangeListener { _, isChecked ->
            // Save low data usage preference
        }

        binding.btnClearCache.setOnClickListener {
            // Clear app cache
            clearCache()
        }

        binding.btnClearData.setOnClickListener {
            // Clear app data
            clearData()
        }
    }

    private fun updateStorageInfo() {
        // Update storage usage information
        binding.tvStorageUsed.text = "1.2 GB used"
        binding.tvCacheSize.text = "256 MB"
        binding.tvDataSize.text = "945 MB"
    }

    private fun clearCache() {
        // Clear app cache
        binding.tvCacheSize.text = "0 MB"
    }

    private fun clearData() {
        // Clear app data
        binding.tvDataSize.text = "0 MB"
        binding.tvStorageUsed.text = "0 MB used"
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 