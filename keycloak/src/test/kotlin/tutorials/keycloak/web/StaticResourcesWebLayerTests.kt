package tutorials.keycloak.web

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import tutorials.keycloak.TestcontainersConfiguration
import tutorials.keycloak.config.KeycloakContainerTest

@KeycloakContainerTest
class StaticResourcesWebLayerTests(
  private val restTemplate: TestRestTemplate
) : TestcontainersConfiguration() {

  @Test
  fun `home page is accessible`() {
    val homeResult = restTemplate.getForEntity("/", String::class.java)

    assertThat(homeResult.statusCode).isEqualTo(HttpStatus.OK)
    assertThat(homeResult.body).contains("Keycloak Playground")

    val indexResult = restTemplate.getForEntity("/index.html", String::class.java)

    assertThat(indexResult.statusCode).isEqualTo(HttpStatus.OK)
    assertThat(indexResult.body).contains("Keycloak Playground")
  }

  @Test
  fun `static assets are accessible`() {
    val cssResult = restTemplate.getForEntity("/css/app.css", String::class.java)

    assertThat(cssResult.statusCode).isEqualTo(HttpStatus.OK)
    assertThat(cssResult.body).contains(".panel")

    val jsResult = restTemplate.getForEntity("/js/app.js", String::class.java)

    assertThat(jsResult.statusCode).isEqualTo(HttpStatus.OK)
    assertThat(jsResult.body).contains("loginWithPassword")
  }
}
