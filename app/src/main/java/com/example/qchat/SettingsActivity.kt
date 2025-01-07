package com.example.qchat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import fragments.ChatFragment
import fragments.CallsFragment
import fragments.ContactsFragment
import fragments.SettingsFragment

class SettingsActivity : AppCompatActivity() {
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottomNavigation)

        bottomNav.setOnItemSelectedListener { menuItem ->
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
                    loadFragment(ContactsFragment())
                    true
                }
                R.id.nav_settings -> {
                    loadFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }

        // Set settings as selected
        bottomNav.selectedItemId = R.id.nav_chat
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}