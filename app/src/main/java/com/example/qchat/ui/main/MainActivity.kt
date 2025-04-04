package com.example.qchat.ui.main

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.example.qchat.R
import com.example.qchat.utils.Constant
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.fragment.app.Fragment
import com.example.qchat.databinding.ActivityMainBinding
import com.example.qchat.model.User
import fragments.CallsFragment
import fragments.ChatFragment
import fragments.GroupsFragment
import com.example.qchat.ui.settings.SettingsFragment

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var pref: SharedPreferences

    @Inject
    lateinit var fireStore: FirebaseFirestore

    lateinit var documentReference: DocumentReference

    private lateinit var navHostFragment: NavHostFragment


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getWindow().setStatusBarColor(getResources().getColor(R.color.black));
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment

        navigateToChatFragmentIfNeeded()

        documentReference = fireStore.collection(Constant.KEY_COLLECTION_USERS)
            .document(pref.getString(Constant.KEY_USER_ID, null).toString())


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Default Fragment
        loadFragment(ChatFragment())

        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_chat -> {
                    loadFragment(ChatFragment())
                    true
                }

                R.id.nav_calls -> {
                    loadFragment(CallsFragment())
                    true
                }

                R.id.nav_contacts -> {
                    loadFragment(GroupsFragment())
                    true
                }

                R.id.nav_settings -> {
                    loadFragment(SettingsFragment())
                    true
                }

                else -> false
            }
        }
    }

    fun setBottomNavigationVisibility(visibility: Int) {
        binding.bottomNavigation.visibility = visibility
    }


    override fun onPause() {
        super.onPause()
        documentReference.update(Constant.KEY_AVAILABILITY, 0)
    }

    override fun onResume() {
        super.onResume()
        documentReference.update(Constant.KEY_AVAILABILITY, 1)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        navigateToChatFragmentIfNeeded()
    }

    private fun navigateToChatFragmentIfNeeded() {
        // Check if the action is to show the ChatFragment
        if (intent?.action == Constant.ACTION_SHOW_CHAT_FRAGMENT) {
            // Retrieve the user from the intent extras
            val user = intent?.getSerializableExtra(Constant.KEY_USER) as? User

            // If user is not null, navigate to ChatFragment with the user data
            user?.let {
                val bundle = Bundle()
                bundle.putSerializable(Constant.KEY_USER, it)
                // Use the correct action to navigate to the ChatFragment
                navHostFragment.findNavController().navigate(R.id.action_mainFragment_to_chatFragment, bundle)
            }
        }
    }


    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView, fragment)
            .commit()
    }


}