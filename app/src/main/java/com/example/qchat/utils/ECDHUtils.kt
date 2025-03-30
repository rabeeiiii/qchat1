package com.example.qchat.utils

import android.util.Base64
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

object ECDHUtils {
    private const val EC_ALGORITHM = "EC"
    private const val KEY_AGREEMENT_ALGORITHM = "ECDH"
    private const val CURVE_NAME = "secp256r1"
    private const val AES_KEY_SIZE = 32

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

    fun generateSharedSecret(
        privateKey: PrivateKey,
        peerPublicKey: PublicKey
    ): SecretKey {
        val keyAgreement = KeyAgreement.getInstance(KEY_AGREEMENT_ALGORITHM)
        keyAgreement.init(privateKey)
        keyAgreement.doPhase(peerPublicKey, true)

        val sharedSecret = keyAgreement.generateSecret()
        val aesKeyBytes = sharedSecret.copyOfRange(0, AES_KEY_SIZE)
        return SecretKeySpec(aesKeyBytes, "AES")
    }
}