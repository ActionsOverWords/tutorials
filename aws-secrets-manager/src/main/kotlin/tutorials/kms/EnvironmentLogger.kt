package tutorials.kms

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest
import software.amazon.awssdk.services.secretsmanager.model.ListSecretsRequest
import tutorials.kms.extentions.logger

@Component
class EnvironmentLogger(
  private val env: Environment,
  private val secretsManagerClient: SecretsManagerClient,
  private val objectMapper: ObjectMapper,

  @param:Value("\${tutorial-key:value-none}") private val tutorialKey: String,
) {

  private val log = logger()

  @PostConstruct
  fun init() {
    log.info("=========================================")
    logApiKey()
    log.info("=========================================")
  }

  private fun logApiKey() {
    log.info("getAll: {}", getAllSecrets())

    log.info("-----------------------------------------")
    log.info("tutorials/api-key: {}", getSecretAsString("/tutorials/api-key"))
    log.info("tutorials/api-key-json: {}", getSecretAsObject<ApiKeyJson>("/tutorials/api-key-json"))

    log.info("-----------------------------------------")
    log.info("value:tutorial-key: {}", tutorialKey)
    log.info("env:tutorial-key: {}", env.getProperty("tutorial-key", "env-none"))
    log.info("application:kms: {}", env.getProperty("kms"))
  }

  private fun getSecretAsString(secretName: String): String {
    val request = GetSecretValueRequest.builder()
      .secretId(secretName)
      .build()

    val response = secretsManagerClient.getSecretValue(request)
    return response.secretString()
  }

  private inline fun <reified T> getSecretAsObject(secretName: String): T {
    val secretString = getSecretAsString(secretName)
    return objectMapper.readValue(secretString, T::class.java)
  }

  private fun getAllSecrets(): Map<String, String> {
    val secrets = mutableMapOf<String, String>()

    try {
      val listRequest = ListSecretsRequest.builder().build()
      val listResponse = secretsManagerClient.listSecrets(listRequest)

      listResponse.secretList().forEach { secret ->
        try {
          secrets[secret.name()] = getSecretAsString(secret.name())
        } catch (e: UnsupportedOperationException) {
          secrets[secret.name()] = "Error: ${e.message}"
        }
      }
    } catch (e: UnsupportedOperationException) {
      log.error("Error listing secrets: ${e.message}", e)
    }

    return secrets
  }

}

data class ApiKeyJson(
  val apiKey: String,
  val apiSecret: String,
)
