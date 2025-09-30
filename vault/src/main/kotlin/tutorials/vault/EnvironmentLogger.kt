package tutorials.vault

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.jdbc.support.JdbcUtils
import org.springframework.stereotype.Component
import tutorials.vault.extentions.logger
import javax.sql.DataSource

@Component
class EnvironmentLogger(
  @Value("\${api-key:none}") private val apiKey: String,
  private val env: Environment,

  private val dataSource: DataSource,
) {
  private val log = logger()

  @PostConstruct
  fun init() {
    log.info("=========================================")
    logEnv()
    logDataBaseVersion()
    log.info("=========================================")
  }

  private fun logEnv() {
    log.info("val: {}", apiKey)
    log.info("env: {}", env.getProperty("api-key", "env-none"))
  }

  private fun logDataBaseVersion() {
    JdbcUtils.extractDatabaseMetaData(dataSource) {
      mapOf(
        "vendor" to it.databaseProductName,
        "version" to it.databaseProductVersion,
        "user" to it.userName,
      )
    }.apply {
      log.info("database: $this")
    }
  }

}
