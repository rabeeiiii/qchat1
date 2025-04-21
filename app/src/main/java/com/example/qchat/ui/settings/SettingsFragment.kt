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
import com.example.qchat.ui.profile.EditProfileActivity
import com.example.qchat.ui.registration.RegistrationActivity
import com.example.qchat.utils.Constant
import com.example.qchat.utils.toast
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    @Inject
    lateinit var sharedPreferences: SharedPreferences // Inject SharedPreferences here

    lateinit var fireStore: FirebaseFirestore // Inject FirebaseFirestore here

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
            R.drawable.plus_grey,
            "Invite a friend",
            "Share this app with friends"
        ),
        SettingItem(
            R.drawable.ic_signout,
            "Sign Out",
            "Sign out of this Device "
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
        observeUserStatus(view) // Add this line

    }

    private fun observeUserStatus(view: View) {
        val userId = sharedPreferences.getString(Constant.KEY_USER_ID, "") ?: ""

        viewModel.getUserStatus(userId).observe(viewLifecycleOwner) { status ->
            view.findViewById<TextView>(R.id.status).text = status ?: "Just joined QChat — let's talk!"
        }

        // Load initial user details
        updateUserDetails(view)
    }

    private fun updateUserDetails(view: View) {
        val userName = sharedPreferences.getString(Constant.KEY_NAME, "User") ?: "User"
        val userImage = sharedPreferences.getString(Constant.KEY_IMAGE, null)

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
        viewModel.signOut().observe(viewLifecycleOwner) { signedOut ->
            if (signedOut) {
                requireContext().toast("Sign Out Successfully!")
                startActivity(Intent(requireActivity(), RegistrationActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } else {
//                requireContext().toast("Error during Sign Out")
            }
        }
    }

    private fun onSettingItemClick(settingItem: SettingItem) {
        when (settingItem.title) {
            "Account" -> {
                val userId = sharedPreferences.getString(Constant.KEY_USER_ID, "") ?: ""
                val userName = sharedPreferences.getString(Constant.KEY_NAME, "User") ?: "User"
                val userEmail = sharedPreferences.getString(Constant.KEY_EMAIL, "") ?: ""
                val userImage = sharedPreferences.getString(Constant.KEY_IMAGE, null)

                val intent = Intent(requireContext(), EditProfileActivity::class.java).apply {
                    putExtra("USER_ID", userId)
                    putExtra("USER_NAME", userName)
                    putExtra("USER_EMAIL", userEmail)
                    putExtra("USER_IMAGE", userImage)
                }
                startActivity(intent)
            }
            "Chat" -> {
                startActivity(Intent(requireContext(), ChatSettingsActivity::class.java))
            }
            "Notifications" -> {
                startActivity(Intent(requireContext(), NotificationsSettingsActivity::class.java))
            }
            "Help" -> {
                startActivity(Intent(requireContext(), HelpSettingsActivity::class.java))
            }
            "Storage and data" -> {
                startActivity(Intent(requireContext(), StorageSettingsActivity::class.java))
            }
            "Invite a friend" -> {
                startActivity(Intent(requireContext(), InviteFriendActivity::class.java))
            }
            "Sign Out" -> signOut()
        }
    }
}
