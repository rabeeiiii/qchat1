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
    private const val AES_KEY_SIZE = 256
    private const val TAG_LENGTH = 128

    fun generateAESKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM)
        keyGenerator.init(AES_KEY_SIZE)
        return keyGenerator.generateKey()
    }

    fun encryptMessage(plainText: String, secretKey: SecretKey): String {
        try {
            val iv = ByteArray(12)
            SecureRandom().nextBytes(iv)

            val cipher = Cipher.getInstance(AES_MODE)
            val gcmSpec = GCMParameterSpec(TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
            val encryptedData = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val cipherTextWithIv = iv + encryptedData
            val base64CipherText = Base64.encodeToString(cipherTextWithIv, Base64.NO_WRAP)
            Log.d("AES", "Encryption successful: $base64CipherText")
            return base64CipherText
        } catch (e: Exception) {
            Log.e("AES", "Encryption failed: ${e.message}")
            e.printStackTrace()
        }

        return ""
    }


    fun decryptMessage(encryptedText: String, secretKey: SecretKey): String {
        try {
            val cipherTextWithIv = Base64.decode(encryptedText, Base64.NO_WRAP)

            if (cipherTextWithIv.size < 12) {
                Log.e("AES", "Decryption failed: Encrypted text is too short to contain IV. EncryptedText: $encryptedText")
                return ""
            }

            val iv = cipherTextWithIv.copyOfRange(0, 12)
            val cipherText = cipherTextWithIv.copyOfRange(12, cipherTextWithIv.size)

            val cipher = Cipher.getInstance(AES_MODE)
            val gcmSpec = GCMParameterSpec(TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            val decryptedData = cipher.doFinal(cipherText)
            val decryptedMessage = String(decryptedData)

            if (decryptedMessage.isEmpty()) {
                Log.e("AES", "Decryption failed: Message is empty. EncryptedText: $encryptedText")
            }
            return decryptedMessage
        } catch (e: Exception) {
            Log.e("AES", "Decryption failed: ${e.message}. EncryptedText: $encryptedText", e)
        }
        return ""
    }


    fun keyToBase64(key: SecretKey): String {
        return Base64.encodeToString(key.encoded, Base64.NO_WRAP)
    }

    fun base64ToKey(base64Key: String): SecretKey {
        val decodedKey = Base64.decode(base64Key, Base64.NO_WRAP)
        return SecretKeySpec(decodedKey, 0, decodedKey.size, AES_ALGORITHM)
    }

    fun encryptBytes(data: ByteArray, secretKey: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(AES_MODE)
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val gcmSpec = GCMParameterSpec(TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        val encryptedData = cipher.doFinal(data)
        return iv + encryptedData
    }

    fun decryptBytes(encryptedData: ByteArray, secretKey: SecretKey): ByteArray {
        try {
            if (encryptedData.size < 12) {
                Log.e("AES", "Decryption failed: Encrypted data is too small to contain IV")
                return ByteArray(0)
            }
            val iv = encryptedData.copyOfRange(0, 12)
            val cipherText = encryptedData.copyOfRange(12, encryptedData.size)
            val cipher = Cipher.getInstance(AES_MODE)
            val gcmSpec = GCMParameterSpec(TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            return cipher.doFinal(cipherText)

        } catch (e: BadPaddingException) {
            Log.e("AES", "Decryption failed: Invalid padding. Encrypted data might be corrupted.", e)
        } catch (e: IllegalBlockSizeException) {
            Log.e("AES", "Decryption failed: Invalid block size. Encrypted data might be corrupted.", e)
        } catch (e: IllegalArgumentException) {
            Log.e("AES", "Decryption failed: Malformed input data.", e)
        } catch (e: Exception) {
            Log.e("AES", "Decryption failed: ${e.message}. EncryptedData: ${Base64.encodeToString(encryptedData, Base64.NO_WRAP)}", e)
        }

        return ByteArray(0)
    }

}