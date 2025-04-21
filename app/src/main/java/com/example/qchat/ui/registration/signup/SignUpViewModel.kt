package com.example.qchat.ui.registration.signup

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
class SignUpViewModel @Inject constructor(
    private val registrationRepository: RegistrationRepository,
    private val pref: SharedPreferences
) : ViewModel() {

    private val _signUp = MutableLiveData<Resource<Boolean>>()
    val signUp: LiveData<Resource<Boolean>> = _signUp

    fun signUp(name: String, email: String, password: String, image: String) {
        viewModelScope.launch {
            _signUp.postValue(Resource.Loading())

            val hashedPassword = hashPassword(password)

            val userData = HashMap<String, Any>()
            userData[Constant.KEY_NAME] = name
            userData[Constant.KEY_EMAIL] = email
            userData[Constant.KEY_PASSWORD] = hashedPassword
            userData[Constant.KEY_IMAGE] = image
            userData[Constant.STATUS] = "Just joined QChat — let's talk!"
            when (val documentReference = registrationRepository.userSignUp(userData)) {
                is Resource.Success -> {
                    documentReference.data?.let { userDocument ->
                        pref.putAny(Constant.KEY_IS_SIGNED_IN, true)
                        pref.putAny(Constant.KEY_USER_ID, userDocument.id)
                        pref.putAny(Constant.KEY_NAME, name)
                        pref.putAny(Constant.KEY_EMAIL, email)
                        pref.putAny(Constant.KEY_IMAGE, image)
                        pref.putAny(Constant.STATUS, "Just joined QChat — let's talk!")
                        _signUp.postValue(Resource.Success(true))
                        Log.d("SignUpViewModel", "User signed up successfully with hashed password.")
                    }
                }
                is Resource.Error -> {
                    _signUp.postValue(Resource.Error(documentReference.message ?: "Error during sign up"))
                    Log.e("SignUpViewModel", "Sign-up error: ${documentReference.message}")
                }
                is Resource.Empty -> {
                    _signUp.postValue(Resource.Error("Unexpected empty response"))
                    Log.e("SignUpViewModel", "Sign-up returned empty response")
                }
                is Resource.Loading -> {
                    Log.d("SignUpViewModel", "Signing up user...")
                }
            }
        }
    }

    private fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }
}
