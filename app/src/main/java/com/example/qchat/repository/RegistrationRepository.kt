package com.example.qchat.repository

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.example.qchat.utils.Constant
import com.example.qchat.utils.Resource
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.Exception

class RegistrationRepository @Inject constructor(private val fireStore: FirebaseFirestore) {


    suspend fun userSignUp(userData: HashMap<String, Any>):Resource<DocumentReference> {
        return try {
            val await = fireStore.collection(Constant.KEY_COLLECTION_USERS)
                .add(userData)
                .await()
            Resource.Success(await)
        }catch (e:Exception){
            Resource.Error(e.message?:"An Unknown Error Occurred")
        }
    }


    suspend fun userSignIn(email: String): Resource<QuerySnapshot> {
        return try {
            val query = FirebaseFirestore.getInstance()
                .collection("users")
                .whereEqualTo(Constant.KEY_EMAIL, email)  // Ensure query filters by email only
                .get()
                .await()

            if (!query.isEmpty) {
                Resource.Success(query)
            } else {
                Resource.Error("User not found")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error fetching user")
        }
    }


}