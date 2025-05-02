package com.example.qchat.network

import com.google.gson.JsonObject
import com.example.qchat.model.MessageBody
import retrofit2.Response
import retrofit2.http.*

interface Api {

    @POST("v1/projects/qchat-bd937/messages:send")
    suspend fun sendMessage(
        @Header("Authorization") bearerToken: String,
        @Body messageBody: MessageBody
    ): Response<JsonObject>

}