package tutorials.jpa.config

import org.springframework.boot.CommandLineRunner
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import tutorials.jpa.base.extentions.logger

@Component
class DatabaseFunctionInitializer(
  val jdbcTemplate: JdbcTemplate
) : CommandLineRunner {

  override fun run(vararg args: String?) {
    try {
      createEncryptFunction()
      createDecryptFunction()

      logger().info("Database encryption functions created successfully")
    } catch (e: Exception) {
      logger().error("Failed to create database encryption functions", e)
      throw RuntimeException("Failed to initialize database encryption functions", e)
    }
  }

  private fun createEncryptFunction() {
    jdbcTemplate.execute("DROP FUNCTION IF EXISTS encrypt_column")

    val createEncryptSql = """
        CREATE FUNCTION encrypt_column(plain_text VARCHAR(255), secret_key VARCHAR(255))
        RETURNS VARCHAR(512)
        DETERMINISTIC
        BEGIN
          RETURN HEX(AES_ENCRYPT(plain_text, secret_key));
        END
        """.trimIndent()

    jdbcTemplate.execute(createEncryptSql)
    logger().debug("Created encrypt_column function with secret_key parameter")
  }

  private fun createDecryptFunction() {
    jdbcTemplate.execute("DROP FUNCTION IF EXISTS decrypt_column")

    val createDecryptSql = """
        CREATE FUNCTION decrypt_column(encrypted_text VARCHAR(512), secret_key VARCHAR(255))
        RETURNS VARCHAR(255)
        DETERMINISTIC
        BEGIN
          RETURN CAST(AES_DECRYPT(UNHEX(encrypted_text), secret_key) AS CHAR);
        END
        """.trimIndent()

    jdbcTemplate.execute(createDecryptSql)
    logger().debug("Created decrypt_column function with secret_key parameter")
  }
}

