package com.example.qchat.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "QChatPrefs"
        private const val KEY_USER_ID = "userId"
        private const val KEY_USER_NAME = "userName"
        private const val KEY_USER_EMAIL = "userEmail"
    }

    fun saveUser(userId: String, userName: String, userEmail: String) {
        val editor = sharedPreferences.edit()
        editor.putString(KEY_USER_ID, userId)
        editor.putString(KEY_USER_NAME, userName)
        editor.putString(KEY_USER_EMAIL, userEmail)
        editor.apply()
    }

    fun getUserId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }

    fun getUserName(): String? {
        return sharedPreferences.getString(KEY_USER_NAME, null)
    }

    fun getUserEmail(): String? {
        return sharedPreferences.getString(KEY_USER_EMAIL, null)
    }

    fun clearUser() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }
} 