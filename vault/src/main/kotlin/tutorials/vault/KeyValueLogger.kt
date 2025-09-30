package tutorials.vault

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import tutorials.vault.extentions.logger

@Component
class KeyValueLogger(
  @Value("\${api-key:none}") private val apiKey: String,
  private val env: Environment,
) {
  private val log = logger()

  @PostConstruct
  fun init() {
    log.info("=========================================")
    log.info("val: {}", apiKey)
    log.info("env: {}", env.getProperty("api-key"))
    log.info("=========================================")
  }

}
