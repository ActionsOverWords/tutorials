package tutorials.keycloak.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.ClientHttpResponse
import org.springframework.util.StreamUtils
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import tutorials.keycloak.auth.KeycloakAuthenticationException
import tutorials.keycloak.auth.KeycloakTokenResponse
import java.nio.charset.StandardCharsets

@Configuration
class KeycloakClientConfig(
  @param:Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
  private val issuerUri: String
) {
  companion object {
    private const val DEFAULT_AUTH_FAILURE_MESSAGE = "authentication failed"
  }

  @Bean
  fun keycloakClient(
    builder: RestClient.Builder,
    objectMapper: ObjectMapper
  ): KeycloakClient {
    return builder.toExchange(issuerUri.trimEnd('/'), objectMapper)
  }

  private inline fun <reified T> RestClient.Builder.toExchange(
    baseUrl: String,
    objectMapper: ObjectMapper
  ): T {
    val restClient = this.baseUrl(baseUrl)
      .defaultStatusHandler(
        { status -> status.isError },
        errorHandler(objectMapper)
      )
      .build()

    val adapter = RestClientAdapter.create(restClient)
    val factory = HttpServiceProxyFactory.builderFor(adapter)
      .build()

    return factory.createClient(T::class.java)
  }

  private fun errorHandler(objectMapper: ObjectMapper): RestClient.ResponseSpec.ErrorHandler {
    return RestClient.ResponseSpec.ErrorHandler { _, response ->
      throw KeycloakAuthenticationException(extractErrorMessage(response, objectMapper))
    }
  }

  private fun extractErrorMessage(response: ClientHttpResponse, objectMapper: ObjectMapper): String {
    val body = StreamUtils.copyToString(response.body, StandardCharsets.UTF_8)
    val tokenResponse = runCatching {
      objectMapper.readValue(body, KeycloakTokenResponse::class.java)
    }.getOrNull()

    return tokenResponse?.errorDescription
      ?: tokenResponse?.error
      ?: DEFAULT_AUTH_FAILURE_MESSAGE
  }
}
