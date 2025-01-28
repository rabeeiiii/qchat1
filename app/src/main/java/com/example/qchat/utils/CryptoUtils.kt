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
        external fun AES_encrypt(input: ByteArray?): ByteArray?
        external fun AES_decrypt(input: ByteArray?): ByteArray?

        //dilithium 2
        const val CRYPTO_PUBLICKEYBYTES = 1312
        const val CRYPTO_SECRETKEYBYTES = 2528
        const val CRYPTO_BYTES = 2420
    }
}