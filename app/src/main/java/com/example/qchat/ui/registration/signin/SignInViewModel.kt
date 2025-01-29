package com.example.qchat.ui.registration.signin

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qchat.repository.RegistrationRepository
import com.example.qchat.utils.Constant
import com.example.qchat.utils.Resource
import com.example.qchat.utils.putAny
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import org.mindrot.jbcrypt.BCrypt

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val registrationRepository: RegistrationRepository,
    private val pref: SharedPreferences,
) : ViewModel() {

    private val _signIn = MutableLiveData<Resource<Boolean>>()
    val signIn: LiveData<Resource<Boolean>> = _signIn

    fun signIn(email: String, password: String) {
        _signIn.postValue(Resource.Loading())
        viewModelScope.launch {
            when (val userSignIn = registrationRepository.userSignIn(email)) {  // ✅ Pass only email
                is Resource.Success -> {
                    userSignIn.data?.let { querySnapshot ->
                        if (querySnapshot.documents.isNotEmpty()) {
                            val doc = querySnapshot.documents[0]

                            val storedHashedPassword = doc.getString(Constant.KEY_PASSWORD)

                            if (storedHashedPassword != null && BCrypt.checkpw(password, storedHashedPassword)) {
                                // ✅ Password matches, log in user
                                pref.putAny(Constant.KEY_IS_SIGNED_IN, true)
                                pref.putAny(Constant.KEY_USER_ID, doc.id)
                                doc.getString(Constant.KEY_NAME)?.let { name -> pref.putAny(Constant.KEY_NAME, name) }
                                doc.getString(Constant.KEY_IMAGE)?.let { image -> pref.putAny(Constant.KEY_IMAGE, image) }
                                pref.putAny(Constant.KEY_EMAIL, email)

                                _signIn.postValue(Resource.Success(true))
                                Log.d("SignInViewModel", "User signed in successfully.")
                            } else {
                                // ❌ Incorrect password
                                _signIn.postValue(Resource.Error("Invalid email or password"))
                                Log.e("SignInViewModel", "Incorrect password for email: $email")
                            }
                        } else {
                            // ❌ User not found
                            _signIn.postValue(Resource.Error("User not found"))
                            Log.e("SignInViewModel", "No user found with email: $email")
                        }
                    }
                }
                is Resource.Error -> {
                    _signIn.postValue(Resource.Error(userSignIn.message ?: "Error during sign-in"))
                }
                is Resource.Empty -> {
                    _signIn.postValue(Resource.Error("Unexpected empty response"))
                }
                is Resource.Loading -> {
                    Log.d("SignInViewModel", "Signing in user...")
                }
            }
        }
    }

    fun clearSignInData() {
        if (_signIn.value != null) {
            _signIn.postValue(Resource.Empty())
        }
    }
}
