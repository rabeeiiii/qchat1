package com.example.qchat.network

import com.example.qchat.model.MessageBody
import com.example.qchat.model.Message
import com.example.qchat.model.Notification
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

@Singleton
class NotificationService @Inject constructor(
    private val api: Api,
    private val fcmAuthUtils: FcmAuthUtils
) {
    suspend fun sendNotification(
        fcmToken: String,
        title: String,
        body: String,
        data: Map<String, String>? = null
    ): Result<Unit> {
        return try {
            if (fcmToken.isEmpty()) {
                Log.e("NotificationService", "FCM token is empty")
                return Result.failure(Exception("FCM token is empty"))
            }

            val accessToken = fcmAuthUtils.getAccessToken()
                ?: return Result.failure(Exception("Failed to get access token"))

            val messageBody = MessageBody(
                message = Message(
                    token = fcmToken,
                    notification = Notification(title = title, body = body),
                    data = data
                )
            )

            val response = api.sendMessage(
                bearerToken = "Bearer $accessToken",
                messageBody = messageBody
            )

            if (response.isSuccessful) {
                Log.d("NotificationService", "Notification sent successfully")
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("NotificationService", "Failed to send notification: $errorBody")
                Result.failure(Exception("Failed to send notification: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("NotificationService", "Error sending notification: ${e.message}")
            Result.failure(e)
        }
    }
} 