#include <jni.h>
#include <string>
#include "dilithium.h"
#include "aes.h"
#include "api.h"

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_example_qchat_utils_CryptoUtils_00024Companion_generateDilithiumKeyPair(
        JNIEnv* env,
        jobject /* this */) {
    uint8_t pk[CRYPTO_PUBLICKEYBYTES];
    uint8_t sk[CRYPTO_SECRETKEYBYTES];

    // Generate the key pair
    if (crypto_sign_keypair(pk, sk) != 0) {
        return nullptr; // Failed to generate keys
    }

    // Combine public and private keys into a single byte array
    jbyteArray result = env->NewByteArray(CRYPTO_PUBLICKEYBYTES + CRYPTO_SECRETKEYBYTES);
    env->SetByteArrayRegion(result, 0, CRYPTO_PUBLICKEYBYTES, reinterpret_cast<jbyte*>(pk));
    env->SetByteArrayRegion(result, CRYPTO_PUBLICKEYBYTES, CRYPTO_SECRETKEYBYTES, reinterpret_cast<jbyte*>(sk));

    return result;
}
extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_example_qchat_utils_CryptoUtils_00024Companion_signMessage(
        JNIEnv* env,
        jobject /* this */,
        jbyteArray sk,
        jbyteArray message) {

    if (sk == nullptr || message == nullptr) {
        return nullptr;
    }

    jsize skLen = env->GetArrayLength(sk);
    if (skLen != CRYPTO_SECRETKEYBYTES) {
        return nullptr;
    }

    jbyte* skBytes = env->GetByteArrayElements(sk, nullptr);
    jbyte* messageBytes = env->GetByteArrayElements(message, nullptr);
    jsize messageLength = env->GetArrayLength(message);

    uint8_t sig[CRYPTO_BYTES];
    size_t siglen = 0;

    int result = pqcrystals_dilithium2_ref_signature(
            sig, &siglen,
            reinterpret_cast<const uint8_t*>(messageBytes),
            static_cast<size_t>(messageLength),
            nullptr,  // Context set to NULL
            0,        // Context length set to 0
            reinterpret_cast<const uint8_t*>(skBytes)
    );

    env->ReleaseByteArrayElements(sk, skBytes, JNI_ABORT);
    env->ReleaseByteArrayElements(message, messageBytes, JNI_ABORT);

    if (result != 0) {
        return nullptr;
    }

    jbyteArray resultArray = env->NewByteArray(static_cast<jsize>(siglen));
    env->SetByteArrayRegion(resultArray, 0, static_cast<jsize>(siglen), reinterpret_cast<jbyte*>(sig));

    return resultArray;
}


extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_qchat_utils_CryptoUtils_00024Companion_verifySignature(
        JNIEnv* env,
        jobject /* this */,
        jbyteArray pk,
        jbyteArray message,
        jbyteArray signature) {

    if (pk == nullptr || message == nullptr || signature == nullptr) {
        return JNI_FALSE;
    }

    jbyte* pkBytes = env->GetByteArrayElements(pk, nullptr);
    jbyte* messageBytes = env->GetByteArrayElements(message, nullptr);
    jbyte* signatureBytes = env->GetByteArrayElements(signature, nullptr);
    jsize messageLength = env->GetArrayLength(message);
    jsize signatureLength = env->GetArrayLength(signature);

    if (pkBytes == nullptr || signatureBytes == nullptr) {
        return JNI_FALSE;
    }

    int result = pqcrystals_dilithium2_ref_verify(
            reinterpret_cast<const uint8_t*>(signatureBytes),
            static_cast<size_t>(signatureLength),
            reinterpret_cast<const uint8_t*>(messageBytes),
            static_cast<size_t>(messageLength),
            NULL,  // Context set to NULL
            0,     // Context length set to 0
            reinterpret_cast<const uint8_t*>(pkBytes)
    );

    env->ReleaseByteArrayElements(pk, pkBytes, JNI_ABORT);
    env->ReleaseByteArrayElements(message, messageBytes, JNI_ABORT);
    env->ReleaseByteArrayElements(signature, signatureBytes, JNI_ABORT);

    return (result == 0) ? JNI_TRUE : JNI_FALSE;
}



extern "C" JNIEXPORT void JNICALL
Java_com_example_qchat_utils_CryptoUtils_00024Companion_AES_1init_1ctx(JNIEnv *env, jobject thiz,
                                                                       jbyteArray key) {
    // Convert the Java byte array to a C array
    jbyte* keyBytes = env->GetByteArrayElements(key, NULL);
    jsize keyLength = env->GetArrayLength(key);

    // Assuming AES_key_size is handled by the key length (AES128, AES192, AES256)
    AES_init_ctx(reinterpret_cast<AES_ctx*>(keyBytes), reinterpret_cast<uint8_t*>(keyBytes));

    // Release the array
    env->ReleaseByteArrayElements(key, keyBytes, 0);
}
extern "C" JNIEXPORT void JNICALL
Java_com_example_qchat_utils_CryptoUtils_00024Companion_AES_1init_1ctx_1iv(JNIEnv *env,
                                                                           jobject thiz,
                                                                           jbyteArray key,
                                                                           jbyteArray iv) {
    // Convert the Java byte arrays to C arrays
    jbyte* keyBytes = env->GetByteArrayElements(key, NULL);
    jbyte* ivBytes = env->GetByteArrayElements(iv, NULL);

    AES_init_ctx_iv(reinterpret_cast<AES_ctx*>(keyBytes), reinterpret_cast<uint8_t*>(keyBytes), reinterpret_cast<uint8_t*>(ivBytes));

    // Release the arrays
    env->ReleaseByteArrayElements(key, keyBytes, 0);
    env->ReleaseByteArrayElements(iv, ivBytes, 0);
}
extern "C" JNIEXPORT void JNICALL
Java_com_example_qchat_utils_CryptoUtils_00024Companion_AES_1ctx_1set_1iv(JNIEnv *env, jobject thiz,
                                                                          jbyteArray iv) {
    // Convert the Java byte array to a C array
    jbyte* ivBytes = env->GetByteArrayElements(iv, NULL);

    AES_ctx_set_iv(reinterpret_cast<AES_ctx*>(ivBytes), reinterpret_cast<uint8_t*>(ivBytes));

    // Release the array
    env->ReleaseByteArrayElements(iv, ivBytes, 0);
}
extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_example_qchat_utils_CryptoUtils_00024Companion_AES_1encrypt(JNIEnv *env, jobject thiz,
                                                                     jbyteArray input) {
    jbyte* inputBytes = env->GetByteArrayElements(input, NULL);
    jsize inputLength = env->GetArrayLength(input);

    uint8_t* encryptedBytes = new uint8_t[inputLength];  // Assume it fits in the buffer
    AES_ECB_encrypt(reinterpret_cast<AES_ctx*>(inputBytes), encryptedBytes);  // Replace with your encryption logic

    jbyteArray encrypted = env->NewByteArray(inputLength);
    env->SetByteArrayRegion(encrypted, 0, inputLength, reinterpret_cast<jbyte*>(encryptedBytes));

    delete[] encryptedBytes;
    env->ReleaseByteArrayElements(input, inputBytes, 0);
    return encrypted;
}
extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_example_qchat_utils_CryptoUtils_00024Companion_AES_1decrypt(JNIEnv *env, jobject thiz,
                                                                     jbyteArray input) {
    jbyte* inputBytes = env->GetByteArrayElements(input, NULL);
    jsize inputLength = env->GetArrayLength(input);

    uint8_t* decryptedBytes = new uint8_t[inputLength];  // Assume it fits in the buffer
    AES_ECB_decrypt(reinterpret_cast<AES_ctx*>(inputBytes), decryptedBytes);  // Replace with your decryption logic

    jbyteArray decrypted = env->NewByteArray(inputLength);
    env->SetByteArrayRegion(decrypted, 0, inputLength, reinterpret_cast<jbyte*>(decryptedBytes));

    delete[] decryptedBytes;
    env->ReleaseByteArrayElements(input, inputBytes, 0);
    return decrypted;
}