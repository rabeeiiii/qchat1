package com.example.qchat.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
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
            openHelpCenter()
        }

        binding.btnContactUs.setOnClickListener {
            openContactUs()
        }

        binding.btnPrivacyPolicy.setOnClickListener {
            openPrivacyPolicy()
        }

        binding.btnTerms.setOnClickListener {
            openTermsOfService()
        }

        binding.btnFaq.setOnClickListener {
            openFAQ()
        }
    }

    private fun openHelpCenter() {
        // Animate button press
        binding.btnHelpCenter.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                binding.btnHelpCenter.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()

        // Open help center URL
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://help.qchat.com"))
        startActivity(intent)
    }

    private fun openContactUs() {
        binding.btnContactUs.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                binding.btnContactUs.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:support@qchat.com")
            putExtra(Intent.EXTRA_SUBJECT, "QChat Support Request")
        }
        startActivity(Intent.createChooser(intent, "Contact Support"))
    }

    private fun openPrivacyPolicy() {
        binding.btnPrivacyPolicy.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                binding.btnPrivacyPolicy.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://qchat.com/privacy"))
        startActivity(intent)
    }

    private fun openTermsOfService() {
        binding.btnTerms.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                binding.btnTerms.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://qchat.com/terms"))
        startActivity(intent)
    }

    private fun openFAQ() {
        binding.btnFaq.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                binding.btnFaq.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://qchat.com/faq"))
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 