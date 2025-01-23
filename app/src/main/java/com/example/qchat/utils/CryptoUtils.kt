package com.example.qchat.utils
class CryptoUtils {
    companion object {
        // Load the native library
        init {
            System.loadLibrary("native-lib")
        }

        // Declare native methods
        external fun generateDilithiumKeyPair(): ByteArray
        external fun signMessage(sk: ByteArray, message: ByteArray): ByteArray
        external fun verifySignature(pk: ByteArray, message: ByteArray, signature: ByteArray): Boolean
        external fun AES_init_ctx(key: ByteArray?)
        external fun AES_init_ctx_iv(key: ByteArray?, iv: ByteArray?)
        external fun AES_ctx_set_iv(iv: ByteArray?)
        external fun AES_encrypt(input: ByteArray?): ByteArray? // Example for encryption
        external fun AES_decrypt(input: ByteArray?): ByteArray?
    }
}