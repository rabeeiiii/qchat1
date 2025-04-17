package com.example.qchat.ui.settings

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QuerySnapshot
import com.example.qchat.model.ChatMessage
import com.example.qchat.repository.MainRepository
import com.example.qchat.utils.Constant
import com.example.qchat.utils.clearAll
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.w3c.dom.DocumentType
import java.util.*
import javax.inject.Inject
import kotlin.collections.HashMap

@HiltViewModel
class SettingsViewModel@Inject constructor(
    private val pref: SharedPreferences,
    private val repository: MainRepository,
    private val firestore: FirebaseFirestore

) : ViewModel() {
    fun getUserStatus(userId: String): LiveData<String?> {
        val statusLiveData = MutableLiveData<String?>()

        firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("SettingsViewModel", "Error fetching status", error)
                    return@addSnapshotListener
                }

                val status = snapshot?.getString("status")
                statusLiveData.value = status
            }

        return statusLiveData
    }

    init {
        viewModelScope.launch {
            pref.getString(Constant.KEY_USER_ID, null)
                ?.let { repository.updateToken(repository.getToken(), it) }
        }
    }
    fun signOut(): LiveData<Boolean> {
        val signOut = MutableLiveData(false)
        viewModelScope.launch {
            val userData = HashMap<String, Any>()
            userData[Constant.KEY_FCM_TOKEN] = FieldValue.delete()
            val isSignOut =
                repository.userSignOut(pref.getString(Constant.KEY_USER_ID, null)!!, userData)
            if (isSignOut) {
                pref.clearAll()
            }
            signOut.postValue(isSignOut)
        }
        return signOut
    }
}