package com.example.qchat.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.qchat.databinding.ActivityHelpSettingsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HelpSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHelpSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelpSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Help & Support"
    }

    private fun setupClickListeners() {
        binding.btnHelpCenter.setOnClickListener {
            openUrl("https://help.qchat.com")
        }

        binding.btnContactUs.setOnClickListener {
            openUrl("https://qchat.com/contact")
        }

        binding.btnPrivacyPolicy.setOnClickListener {
            openUrl("https://qchat.com/privacy")
        }

        binding.btnTerms.setOnClickListener {
            openUrl("https://qchat.com/terms")
        }

        binding.btnFaq.setOnClickListener {
            openUrl("https://qchat.com/faq")
        }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
