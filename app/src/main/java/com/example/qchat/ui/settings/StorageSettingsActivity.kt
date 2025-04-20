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
        }

        binding.btnClearCache.setOnClickListener {
            clearCache()
        }

        binding.btnClearData.setOnClickListener {
            clearData()
        }
    }

    private fun updateStorageInfo() {
        binding.tvStorageUsed.text = "1.2 GB used"
        binding.tvCacheSize.text = "256 MB"
        binding.tvDataSize.text = "945 MB"
    }

    private fun clearCache() {
        binding.tvCacheSize.text = "0 MB"
    }

    private fun clearData() {
        binding.tvDataSize.text = "0 MB"
        binding.tvStorageUsed.text = "0 MB used"
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 