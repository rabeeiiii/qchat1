package com.example.qchat.network

import android.content.Context
import android.util.Log
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.auth.http.HttpTransportFactory
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmAuthUtils @Inject constructor(
    private val context: Context
) {
    private val transportFactory = object : HttpTransportFactory {
        override fun create(): HttpTransport {
            return NetHttpTransport()
        }
    }

    suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        try {
            val SCOPES = listOf("https://www.googleapis.com/auth/firebase.messaging")
            Log.d("FcmAuthUtils", "Initializing credentials with scopes: $SCOPES")
            
            val serviceAccountJson = """
                {
                    "type": "service_account",
                    "project_id": "qchat-bd937",
                    "private_key_id": "657c7f5b33ac6218adfce239ce44474caac0a233",
                    "private_key": "-----BEGIN PRIVATE KEY-----\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCnvMoqzx/KfRg2\ndHvTtlATKvLR/ZThFQXbLUi59HggfLbs/nCAeR8TtaHNThhay/U+LRgq9FT/v7g8\nvrKRvvspP1iit5uDy2/TVX0EXYrvHz6hvAk7Bds88eWRG2dKHfQ91amt75SIvJ1K\nzF/uQU114m1cRTFo2zLnb4F5l3utt82XcLgMQI0ptxgLlsoE9EfBoFiNo8QYcMZd\nzE70p2dYX7pg+dJQY6dR4fv0GedxlPOAP22fPaI8iEkchnGDNuRdmCS/HG2dj+eh\nZOmrmSPZ9KzlUXA8qaG6Y0ElL3M4ZJF/Izi/2UpynyBsAZOzS2DtAF+qFaabJP6D\nLDTWnOBlAgMBAAECggEAAJo3f6hICZQ8zWRsBQEoFsfZjHQmYBlos/mm7CkZPO0N\nUQauzWJDqQuDbDItteXEx+uKzaAOzhdHpr2r9NJ2+ll4u89la8vAgjgb9VvYOV27\nXEX4m8dXgB9FBDeN0fACBgX8UIuadqTwCE9DmBfha4w6Gtift8BETvSvkD/O0KPY\nH+p7lqERMoApazcdOb7ZHm2VYLlykcZcR3FREKLJYchuErFYbcyYTJvrYPL/2sWl\ny5xV4X9vKSamTQiqm1BxvusYV/GEmWJrz4F3e2MJh+Ja72T2xAqrf3tn3hIJo0cV\n+ws7wXIAouI4cuX8IlcOSIj+broU258tUPVmfcoMAQKBgQDQr7ft420wP19DGaUC\nMVQtBG1lFnSeKfEzlwpY9j4/7Wcq64L9NWhLuEYKM/Xhsc7U6sELKRuXsByhyaQ2\nb1NjCdsiY7TebVZQsdwRmu7sM+uZr1kqfwe2GIcHyzTWhK2D94/OZ/YE3CNHTZPf\n4kfWxS8KcRnQYayRaj6A0WwK4QKBgQDNxF7hrhDrH9H3SGCV482IaA5L3EwUsB7F\nQHFEA2F2tmVA9POqh4zEiZJXKPIle4G5YUIktCFTL4vZW1WR4du2OlKmNTNy1/z7\n4Y0ml8ViRGKEQYdTTT6V8V0pF1WbCts8rEQeun92UA5vQgVseqdE/Y44N4I97o5N\nzv1qBm/qBQKBgA8YtyCRdEOqQfTztPksol9DU7qdXkBW3mkSAyCeZ7BuNylmsiop\nI+teYEq6qY3zM/g5J0/sYF+f0OJvWN7LPOgMPXsGZX41wKnDxBzN7XzO4LtUcQne\n+KrWqWYJ8D+Yh4jlbtTKtoRxfGfbF9h5YSMLTrTdq7Vka3x1iCH/hGZhAoGBAJeN\nwQyb9l4Xa8CkkG8WADYt3gnF0kuShLdoBYTAsLKNGPrE8At0FxxS73Q+9krhMuTl\nW6BJBBqO9IQ9H0N5hSgswN8mZCR8LRsgP6RjOMBt+dnLoe6bADPUOHehdi3hyyRf\nBTVUy7jKsjcD+5awqC7KSkvhpo4S/MEWadQDtb45AoGBAJOD4lRK4CjhXSJxFp3y\nAN5gzk157EXYjvCiDfneMnG1kCglRCGZLaUezj1Yp0o2c35tWvJhiLMwreolAsFm\n7JGMzxwkyvQUZdA926D3deuHDn7b8dikJfYFVx5HuoYBftEx4cgDnGuIewHOAi8b\nNOiSfMetZHrIkNEjCSFVpgqL\n-----END PRIVATE KEY-----\n",
                    "client_email": "firebase-adminsdk-fbsvc@qchat-bd937.iam.gserviceaccount.com",
                    "client_id": "110078890617361434770",
                    "auth_uri": "https://accounts.google.com/o/oauth2/auth",
                    "token_uri": "https://oauth2.googleapis.com/token",
                    "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
                    "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-fbsvc%40qchat-bd937.iam.gserviceaccount.com"
                }
            """.trimIndent()
            
            Log.d("FcmAuthUtils", "Creating credentials from JSON")
            
            val credentials =   ServiceAccountCredentials.fromStream(
                ByteArrayInputStream(serviceAccountJson.toByteArray()),
                transportFactory
            ).createScoped(SCOPES)
            
            Log.d("FcmAuthUtils", "Credentials created, refreshing token...")
            credentials.refreshIfExpired()
            
            val token = credentials.accessToken.tokenValue
            if (token == null) {
                Log.e("FcmAuthUtils", "Access token is null after refresh")
            } else {
                Log.d("FcmAuthUtils", "Access token obtained successfully")
            }
            token
        } catch (e: IOException) {
            Log.e("FcmAuthUtils", "Error getting access token: ${e.message}")
            e.printStackTrace()
            null
        } catch (e: Exception) {
            Log.e("FcmAuthUtils", "Unexpected error getting access token: ${e.message}")
            e.printStackTrace()
            null
        }
    }
} 