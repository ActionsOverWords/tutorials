package tutorials.jpa.config

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.math.min

@Component
@Converter
class EncryptedStringConverter(
  @Value("\${kms.db.secret_key}") secretKeyString: String,
) : AttributeConverter<String?, String?> {

  lateinit var secretKey: SecretKeySpec

  init {
    val keyBytes = secretKeyString.toByteArray(StandardCharsets.UTF_8)
    val paddedKey = ByteArray(16)
    System.arraycopy(keyBytes, 0, paddedKey, 0, min(keyBytes.size, 16))
    this.secretKey = SecretKeySpec(paddedKey, "AES")
  }

  override fun convertToDatabaseColumn(attribute: String?): String? {
    if (attribute == null) return null

    try {
      val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
      cipher.init(Cipher.ENCRYPT_MODE, secretKey)
      val encrypted = cipher.doFinal(attribute.toByteArray(StandardCharsets.UTF_8))
      return bytesToHex(encrypted)
    } catch (e: Exception) {
      throw RuntimeException("Error encrypting data", e)
    }
  }

  override fun convertToEntityAttribute(dbData: String?): String? {
    if (dbData == null) return null

    try {
      val encryptedBytes = hexToBytes(dbData)
      val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
      cipher.init(Cipher.DECRYPT_MODE, secretKey)
      val decrypted = cipher.doFinal(encryptedBytes)
      return String(decrypted, StandardCharsets.UTF_8)
    } catch (e: Exception) {
      throw RuntimeException("Error decrypting data", e)
    }
  }

  companion object {
    private fun bytesToHex(bytes: ByteArray): String {
      val sb = StringBuilder()
      for (b in bytes) {
        sb.append(String.format("%02X", b))
      }
      return sb.toString()
    }

    private fun hexToBytes(hex: String): ByteArray {
      val len = hex.length
      val data = ByteArray(len / 2)

      for (i in 0 until len step 2) {
        val digit1 = hex[i].digitToIntOrNull(16) ?: 0
        val digit2 = hex[i + 1].digitToIntOrNull(16) ?: 0

        data[i / 2] = ((digit1 shl 4) + digit2).toByte()
      }

      return data
    }
  }
}

