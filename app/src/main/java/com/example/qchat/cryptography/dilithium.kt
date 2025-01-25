import android.os.Build
import androidx.annotation.RequiresApi
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

class AES {
        private lateinit var secretKeySpec: SecretKeySpec
        private lateinit var ivParameterSpec: IvParameterSpec

        fun init(key: ByteArray, iv: ByteArray) {
                secretKeySpec = SecretKeySpec(key, "AES")
                ivParameterSpec = IvParameterSpec(iv)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun encrypt(input: String): String {
                val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)
                val encrypted = cipher.doFinal(input.toByteArray())
                return Base64.getEncoder().encodeToString(encrypted)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun decrypt(input: String): String {
                val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, secretKeySpec)
                val decrypted = cipher.doFinal(Base64.getDecoder().decode(input))
                return String(decrypted)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun encryptCBC(input: String, iv: ByteArray): String {
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, IvParameterSpec(iv))
                val encrypted = cipher.doFinal(input.toByteArray())
                return Base64.getEncoder().encodeToString(encrypted)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun decryptCBC(input: String, iv: ByteArray): String {
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, IvParameterSpec(iv))
                val decrypted = cipher.doFinal(Base64.getDecoder().decode(input))
                return String(decrypted)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun encryptCTR(input: String): String {
                val cipher = Cipher.getInstance("AES/CTR/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec)
                val encrypted = cipher.doFinal(input.toByteArray())
                return Base64.getEncoder().encodeToString(encrypted)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun decryptCTR(input: String): String {
                val cipher = Cipher.getInstance("AES/CTR/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)
                val decrypted = cipher.doFinal(Base64.getDecoder().decode(input))
                return String(decrypted)
        }
}
