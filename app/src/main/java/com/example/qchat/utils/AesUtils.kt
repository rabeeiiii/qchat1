package com.example.qchat.utils
import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import java.security.NoSuchAlgorithmException
import javax.crypto.BadPaddingException
import javax.crypto.IllegalBlockSizeException
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec

object AesUtils {

    private const val AES_ALGORITHM = "AES"
    private const val AES_MODE = "AES/GCM/NoPadding"
    private const val AES_KEY_SIZE = 256 // AES key size (in bits)
    private const val TAG_LENGTH = 128 // GCM tag length (in bits)

    // Generate a random AES key
    fun generateAESKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM)
        keyGenerator.init(AES_KEY_SIZE)
        return keyGenerator.generateKey()
    }

    // Encrypt message using AES encryption
    fun encryptMessage(plainText: String, secretKey: SecretKey): String {
        try {
            val cipher = Cipher.getInstance(AES_MODE)
            val iv = ByteArray(12) // 12 bytes for GCM IV
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val encryptedData = cipher.doFinal(plainText.toByteArray())
            val cipherTextWithIv = iv + encryptedData // Prepend IV to the cipher text

            // Encode to Base64 for safe transmission
            return Base64.encodeToString(cipherTextWithIv, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    // Decrypt AES-encrypted message
    fun decryptMessage(encryptedText: String, secretKey: SecretKey): String {
        try {
            val cipherTextWithIv = Base64.decode(encryptedText, Base64.NO_WRAP)
            val iv = cipherTextWithIv.copyOfRange(0, 12)
            val cipherText = cipherTextWithIv.copyOfRange(12, cipherTextWithIv.size)

            val cipher = Cipher.getInstance(AES_MODE)
            val gcmSpec = GCMParameterSpec(TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            val decryptedData = cipher.doFinal(cipherText)
            return String(decryptedData)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    // Helper function to convert a key to string for storage (Base64)
    fun keyToBase64(key: SecretKey): String {
        return Base64.encodeToString(key.encoded, Base64.NO_WRAP)
    }

    // Helper function to convert a string back to SecretKey (from Base64)
    fun base64ToKey(base64Key: String): SecretKey {
        val decodedKey = Base64.decode(base64Key, Base64.NO_WRAP)
        return SecretKeySpec(decodedKey, 0, decodedKey.size, AES_ALGORITHM)
    }
}



