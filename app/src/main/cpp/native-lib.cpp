#include <jni.h>
#include <string>
#include "dilithium.h"
#include "aes.h"

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_example_qchat_utils_CryptoUtils_00024Companion_generateDilithiumKeyPair(
        JNIEnv* env,
        jobject /* this */) {
    uint8_t pk[CRYPTO_PUBLICKEYBYTES];
    uint8_t sk[CRYPTO_SECRETKEYBYTES];

    // Generate the key pair
    crypto_sign_keypair(pk, sk);

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

    // Check for null input arrays
    if (sk == nullptr || message == nullptr) {
        return nullptr; // Return null if any argument is null
    }

    // Get the byte arrays
    jbyte* skBytes = env->GetByteArrayElements(sk, nullptr);
    if (skBytes == nullptr) {
        return nullptr; // Failed to allocate skBytes
    }

    jbyte* messageBytes = env->GetByteArrayElements(message, nullptr);
    if (messageBytes == nullptr) {
        env->ReleaseByteArrayElements(sk, skBytes, JNI_ABORT); // Release skBytes
        return nullptr; // Failed to allocate messageBytes
    }

    jsize messageLength = env->GetArrayLength(message);

    // Ensure CRYPTO_BYTES is sufficient
    uint8_t sig[CRYPTO_BYTES];
    size_t siglen;

    // Provide null context (ctx) and set context length (ctxlen) to 0
    const uint8_t* ctx = nullptr;
    size_t ctxlen = 0;

    // Call the crypto_sign_signature function
    int result = crypto_sign_signature(
            sig, &siglen,                                           // Signature buffer and its length
            reinterpret_cast<const uint8_t*>(messageBytes),         // Message bytes
            static_cast<size_t>(messageLength),                     // Message length
            reinterpret_cast<const uint8_t*>(skBytes),              // Secret key
            (size_t) ctx,
            reinterpret_cast<const uint8_t *>(ctxlen)                                             // Context and context length
    );

    // Release the JNI resources early
    env->ReleaseByteArrayElements(sk, skBytes, JNI_ABORT);
    env->ReleaseByteArrayElements(message, messageBytes, JNI_ABORT);

    // Handle signing errors
    if (result != 0) {
        return nullptr; // Return null if signing fails
    }

    // Create a Java byte array for the signature
    jbyteArray resultArray = env->NewByteArray(static_cast<jsize>(siglen));
    if (resultArray == nullptr) {
        return nullptr; // Return null if byte array creation fails
    }

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
    jbyte* pkBytes = env->GetByteArrayElements(pk, nullptr);
    jbyte* messageBytes = env->GetByteArrayElements(message, nullptr);
    jbyte* signatureBytes = env->GetByteArrayElements(signature, nullptr);
    jsize messageLength = env->GetArrayLength(message);
    jsize signatureLength = env->GetArrayLength(signature);

    // Placeholder for output length (not used in verification)
    size_t msglen_out = 0;

    // Verify the signature
    int result = crypto_sign_verify(
            reinterpret_cast<const uint8_t*>(signatureBytes), signatureLength,
            reinterpret_cast<const uint8_t*>(messageBytes), messageLength,
            reinterpret_cast<const uint8_t*>(pkBytes),
            CRYPTO_PUBLICKEYBYTES, // Public key length
            reinterpret_cast<const uint8_t *>(&msglen_out)            // Message length output
    );

    // Release resources
    env->ReleaseByteArrayElements(pk, pkBytes, 0);
    env->ReleaseByteArrayElements(message, messageBytes, 0);
    env->ReleaseByteArrayElements(signature, signatureBytes, 0);

    return result == 0; // Return true if verification succeeds
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