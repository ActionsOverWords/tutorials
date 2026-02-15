package tutorials.keycloak.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tutorials.keycloak.TestcontainersConfiguration
import tutorials.keycloak.config.KeycloakContainerTest
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@KeycloakContainerTest
class OAuth2LoginIntegrationTests : TestcontainersConfiguration() {

  @Test
  fun `authorization endpoint redirects to keycloak login page`() {
    val client = HttpClient.newBuilder()
      .followRedirects(HttpClient.Redirect.NEVER)
      .build()

    val authorizationStart = get(client, appUrl("/oauth2/authorization/keycloak"))
    assertThat(authorizationStart.statusCode()).isEqualTo(302)
    val location = locationOf(authorizationStart)
    assertThat(location).contains("/protocol/openid-connect/auth")
    assertThat(location).contains("client_id=tutorials-keycloak")
  }

  private fun appUrl(path: String): String = "http://localhost:8080$path"

  private fun get(client: HttpClient, url: String): HttpResponse<String> {
    val request = HttpRequest.newBuilder(URI.create(url))
      .GET()
      .build()
    return client.send(request, HttpResponse.BodyHandlers.ofString())
  }

  private fun locationOf(response: HttpResponse<String>): String {
    return response.headers().firstValue("Location")
      .orElseThrow { IllegalStateException("Location header is missing") }
  }
}
