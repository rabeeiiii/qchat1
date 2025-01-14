package com.example.qchat.ui.users

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qchat.model.User
import com.example.qchat.repository.MainRepository
import com.example.qchat.utils.Constant
import com.example.qchat.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UsersViewModel @Inject constructor(
    private val pref: SharedPreferences,
    private val repository: MainRepository,
) : ViewModel() {

    private val _usersList = MutableLiveData<Resource<List<User>>>()
    val usersList: LiveData<Resource<List<User>>> = _usersList

    init {
        fetchUserList()
    }


    private fun fetchUserList() {

        viewModelScope.launch {
            _usersList.postValue(Resource.Loading())

            when (val response = repository.getAllUsers()) {

                is Resource.Success -> {
                    val currentUserId = pref.getString(Constant.KEY_USER_ID, null)
                    response.data?.let { snapshot ->
                        val userList = mutableListOf<User>()

                        for (user in snapshot.documents) {
                            if (currentUserId == user.id) continue

                            userList.add(User(
                                user[Constant.KEY_NAME].toString(),
                                user[Constant.KEY_IMAGE].toString(),
                                user[Constant.KEY_EMAIL].toString(),
                                user[Constant.KEY_FCM_TOKEN].toString(),
                                user.id
                            ))
                        }
                        _usersList.postValue(Resource.Success(userList))
                        return@launch
                    }
                }

                is Resource.Error -> {
                    _usersList.postValue(Resource.Error(response.message?:"An Unknown Error Occurred"))
                }

                is Resource.Empty -> {
                    _usersList.postValue(Resource.Empty(response.message))
                }

                is Resource.Loading -> TODO()
            }

        }

    }


}