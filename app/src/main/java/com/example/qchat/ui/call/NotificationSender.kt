package com.example.qchat.notifications

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object NotificationSender {

    private const val FCM_URL = "https://fcm.googleapis.com/fcm/send"
    private const val SERVER_KEY = "YOUR_SERVER_KEY_HERE" // Replace with your Firebase server key

    /**
     * Sends a call notification via Firebase Cloud Messaging.
     *
     * @param receiverToken The FCM token of the user receiving the call.
     * @param callerName The name of the caller.
     * @param callType The type of call, either "voice" or "video".
     */
    fun sendCallNotification(receiverToken: String, callerName: String, callType: String) {
        val payload = JSONObject().apply {
            put("to", receiverToken)
            put("data", JSONObject().apply {
                put("type", "call")
                put("callerName", callerName)
                put("callType", callType)
            })
        }

        val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(FCM_URL)
            .addHeader("Authorization", "key=$SERVER_KEY")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("NotificationSender", "FCM send failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        Log.d("NotificationSender", "FCM send success: ${it.body?.string()}")
                    } else {
                        Log.e("NotificationSender", "FCM send error: ${it.body?.string()}")
                    }
                }
            }
        })
    }
}
