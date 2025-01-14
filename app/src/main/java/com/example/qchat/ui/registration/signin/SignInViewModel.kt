package com.example.qchat.ui.registration.signin

import android.content.SharedPreferences
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
            when (val userSignIn = registrationRepository.userSignIn(email, password)) {
                is Resource.Success -> {
                    userSignIn.data?.let {
                        if (it.documents.size > 0) {
                            val doc = it.documents[0]
                            pref.putAny(Constant.KEY_IS_SIGNED_IN,true)
                            pref.putAny(Constant.KEY_USER_ID,doc.id)
                            doc.getString(Constant.KEY_NAME)
                                ?.let { it1 -> pref.putAny(Constant.KEY_NAME, it1) }
                            doc.getString(Constant.KEY_IMAGE)
                                ?.let { it1 -> pref.putAny(Constant.KEY_IMAGE, it1) }
                            pref.putAny(Constant.KEY_EMAIL,email)
                            _signIn.postValue(Resource.Success(true))
                        }
                    }
                }
                is Resource.Error -> {
                    userSignIn.message?.let { _signIn.postValue(Resource.Error(it)) }
                }

                is Resource.Empty -> TODO()
                is Resource.Loading -> TODO()
            }
        }

    }

    fun clearSignInData(){
        if (_signIn.value != null){
            _signIn.postValue(Resource.Empty())
        }
    }

}