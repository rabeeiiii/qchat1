package com.example.qchat.utils
import android.util.Base64
import android.util.Log
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
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
            // Generate a secure random IV
            val iv = ByteArray(12) // 12 bytes for GCM IV
            SecureRandom().nextBytes(iv)

            // Initialize cipher for encryption
            val cipher = Cipher.getInstance(AES_MODE)
            val gcmSpec = GCMParameterSpec(TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

            // Encrypt the plaintext
            val encryptedData = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // Prepend IV to the encrypted data
            val cipherTextWithIv = iv + encryptedData

            // Encode to Base64 for safe transmission
            val base64CipherText = Base64.encodeToString(cipherTextWithIv, Base64.NO_WRAP)

            // Log successful encryption
            Log.d("AES", "Encryption successful: $base64CipherText")
            return base64CipherText
        } catch (e: Exception) {
            // Log encryption failure
            Log.e("AES", "Encryption failed: ${e.message}")
            e.printStackTrace()
        }

        // Return an empty string if encryption fails
        return ""
    }


    // Decrypt AES-encrypted message
    fun decryptMessage(encryptedText: String, secretKey: SecretKey): String {
        try {
            // Decode Base64-encoded ciphertext with IV
            val cipherTextWithIv = Base64.decode(encryptedText, Base64.NO_WRAP)

            // Extract the IV (first 12 bytes)
            val iv = cipherTextWithIv.copyOfRange(0, 12)

            // Extract the actual ciphertext (remaining bytes)
            val cipherText = cipherTextWithIv.copyOfRange(12, cipherTextWithIv.size)

            // Initialize cipher for decryption
            val cipher = Cipher.getInstance(AES_MODE)
            val gcmSpec = GCMParameterSpec(TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            // Perform decryption
            val decryptedData = cipher.doFinal(cipherText)
            val decryptedMessage = String(decryptedData)

            // Log successful decryption
            Log.d("AES", "Decryption successful: $decryptedMessage")
            return decryptedMessage

        } catch (e: BadPaddingException) {
            Log.e("AES", "Decryption failed: Invalid padding. Message might be corrupted. EncryptedText: $encryptedText", e)
        } catch (e: IllegalBlockSizeException) {
            Log.e("AES", "Decryption failed: Invalid block size. EncryptedText: $encryptedText", e)
        } catch (e: IllegalArgumentException) {
            Log.e("AES", "Decryption failed: Malformed Base64 input. EncryptedText: $encryptedText", e)
        } catch (e: Exception) {
            Log.e("AES", "Decryption failed: ${e.message}. EncryptedText: $encryptedText", e)
        }

        // Return empty string if decryption fails
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

    fun encryptByteArray(byteArray: ByteArray): ByteArray {
        return try {
            val secretKey = generateAESKey()
            val cipher = Cipher.getInstance(AES_MODE)
            val iv = ByteArray(12)
            SecureRandom().nextBytes(iv)

            val gcmSpec = GCMParameterSpec(TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
            val encryptedData = cipher.doFinal(byteArray)

            // Combine IV and encrypted data
            iv + encryptedData
        } catch (e: Exception) {
            Log.e("AES", "Error encrypting byte array: ${e.message}")
            byteArray // Return original if encryption fails
        }
    }

}



