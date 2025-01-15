package com.example.qchat.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qchat.R
import com.example.qchat.adapter.SettingsAdapter
import com.example.qchat.model.SettingItem
import com.google.android.material.appbar.MaterialToolbar
import com.example.qchat.utils.decodeToBitmap
import android.content.SharedPreferences
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.example.qchat.ui.main.MainViewModel
import com.example.qchat.ui.registration.RegistrationActivity
import com.example.qchat.utils.Constant
import com.example.qchat.utils.toast
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    @Inject
    lateinit var sharedPreferences: SharedPreferences
    private val viewModel: SettingsViewModel by viewModels()
    private val settingsItems = listOf(
        SettingItem(
            R.drawable.ic_key,
            "Account",
            "Privacy, security, change number"
        ),
        SettingItem(
            R.drawable.ic_chat,
            "Chat",
            "Chat history, themes, wallpapers"
        ),
        SettingItem(
            R.drawable.ic_notifications,
            "Notifications",
            "Messages, group and others"
        ),
        SettingItem(
            R.drawable.ic_help,
            "Help",
            "Help center, contact us, privacy policy"
        ),
        SettingItem(
            R.drawable.ic_storage,
            "Storage and data",
            "Network usage, storage usage"
        ),
        SettingItem(
            R.drawable.ic_invite,
            "Invite a friend",
            "Share this app with friends"
        ),
        SettingItem(
            R.drawable.ic_signout,
            "Sign Out",
            " "
        ),
    )


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(view)
        setupRecyclerView(view)
        updateUserDetails(view)
    }

    private fun updateUserDetails(view: View) {
        // Fetch user details from SharedPreferences (or your data source)
        val userName = sharedPreferences.getString(Constant.KEY_NAME, "User") ?: "User"
        val userImage = sharedPreferences.getString(Constant.KEY_IMAGE, null)

        // Update the UI with user details
        view.findViewById<TextView>(R.id.tvName).text = userName
        userImage?.let {
            view.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.ivProfile)
                .setImageBitmap(it.decodeToBitmap())
        }
    }

    private fun setupRecyclerView(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.settingsRecyclerView)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = SettingsAdapter(settingsItems) { settingItem ->
                // Handle the click event here
                onSettingItemClick(settingItem)
            }
        }
    }

    private fun setupToolbar(view: View) {
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        (requireActivity() as AppCompatActivity).apply {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "Settings"
        }
    }
    private fun signOut() {

        viewModel.signOut().observe(viewLifecycleOwner) {
            if (it) {
                requireContext().toast("SignOut")
                val intent = Intent(requireActivity(), RegistrationActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)

            } else {
                requireContext().toast("Sign Out Successfully!")
            }
        }


    }

    private fun onSettingItemClick(settingItem: SettingItem) {
        when (settingItem.title) {
            "Account" -> {
                // Handle Account settings click
            }
            "Chat" -> {
                // Handle Chat settings click
            }
            "Notifications" -> {
                // Handle Notifications settings click
            }
            "Help" -> {
                // Handle Help settings click
            }
            "Storage and data" -> {
                // Handle Storage and Data settings click
            }
            "Invite a friend" -> {
                // Handle Invite Friend click
            }
            "Sign Out" -> {
                signOut()

            }
        }
    }
}
