package com.example.qchat.network

import com.example.qchat.model.User
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    @GET("users")
    suspend fun getUsers(): Response<List<User>>

    @GET("users/{userId}")
    suspend fun getUserById(@Path("userId") userId: String): Response<User>
} 