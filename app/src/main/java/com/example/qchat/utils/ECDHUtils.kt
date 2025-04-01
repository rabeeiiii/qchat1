package com.example.qchat.utils

import android.util.Base64
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import java.security.PrivateKey
import java.security.PublicKey
import kotlinx.coroutines.tasks.await
import java.security.KeyPair
import java.security.KeyPairGenerator

object ECDHUtils {
    private const val EC_ALGORITHM = "EC"
    private const val KEY_AGREEMENT_ALGORITHM = "ECDH"
    private const val CURVE_NAME = "secp256r1"
    private const val AES_KEY_SIZE = 32

    private val fireStore = FirebaseFirestore.getInstance()

    fun generateKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(EC_ALGORITHM)
        val ecGenParameterSpec = java.security.spec.ECGenParameterSpec(CURVE_NAME)
        keyPairGenerator.initialize(ecGenParameterSpec)
        return keyPairGenerator.generateKeyPair()
    }

    fun publicKeyToString(publicKey: PublicKey): String {
        return Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
    }

    fun privateKeyToString(privateKey: PrivateKey): String {
        return Base64.encodeToString(privateKey.encoded, Base64.NO_WRAP)
    }

    fun publicKeyFromString(publicKeyString: String): PublicKey {
        val bytes = Base64.decode(publicKeyString, Base64.NO_WRAP)
        val keyFactory = java.security.KeyFactory.getInstance(EC_ALGORITHM)
        val keySpec = java.security.spec.X509EncodedKeySpec(bytes)
        return keyFactory.generatePublic(keySpec)
    }

    fun privateKeyFromString(privateKeyString: String): PrivateKey {
        val bytes = Base64.decode(privateKeyString, Base64.NO_WRAP)
        val keyFactory = java.security.KeyFactory.getInstance(EC_ALGORITHM)
        val keySpec = java.security.spec.PKCS8EncodedKeySpec(bytes)
        return keyFactory.generatePrivate(keySpec)
    }

    suspend fun getReceiverECDHKeys(receiverId: String): Pair<String, String>? {
        val receiverDoc = fireStore.collection("users").document(receiverId).get().await()
        val publicKey = receiverDoc.getString("ecdhPublicKey")
        val privateKey = receiverDoc.getString("ecdhPrivateKey")

        if (publicKey != null && privateKey != null) {
            return Pair(publicKey, privateKey)
        } else {
            Log.e("ECDHUtils", "Failed to retrieve receiver keys.")
            return null
        }
    }

    suspend fun getSenderECDHKeys(senderId: String): Pair<String, String>? {
        val senderDoc = fireStore.collection("users").document(senderId).get().await()
        val publicKey = senderDoc.getString("ecdhPublicKey")
        val privateKey = senderDoc.getString("ecdhPrivateKey")

        if (publicKey != null && privateKey != null) {
            return Pair(publicKey, privateKey)
        } else {
            Log.e("ECDHUtils", "Failed to retrieve sender keys.")
            return null
        }
    }

    suspend fun generateSharedSecret(senderId: String, receiverId: String): SecretKey? {
        val (receiverPublicKeyString, receiverPrivateKeyString) = getReceiverECDHKeys(receiverId) ?: return null
        val (senderPublicKeyString, senderPrivateKeyString) = getSenderECDHKeys(senderId) ?: return null

        val receiverPrivateKey = privateKeyFromString(receiverPrivateKeyString)
        val senderPublicKey = publicKeyFromString(senderPublicKeyString)

        return try {
            val sharedSecret = generateSharedSecret(receiverPrivateKey, senderPublicKey)
            sharedSecret
        } catch (e: Exception) {
            Log.e("MainRepository", "Error generating shared secret: ${e.message}")
            null
        }
    }

    fun generateSharedSecret(
        privateKey: PrivateKey,
        peerPublicKey: PublicKey
    ): SecretKey {
        try {
            val keyAgreement = KeyAgreement.getInstance(KEY_AGREEMENT_ALGORITHM)
            keyAgreement.init(privateKey)
            keyAgreement.doPhase(peerPublicKey, true)

            val sharedSecret = keyAgreement.generateSecret()
            val aesKeyBytes =
                sharedSecret.copyOfRange(0, AES_KEY_SIZE)
            return SecretKeySpec(aesKeyBytes, "AES")
        } catch (e: Exception) {
            Log.e("ECDHUtils", "Error generating shared secret: ${e.message}")
            throw e
        }
    }
}
