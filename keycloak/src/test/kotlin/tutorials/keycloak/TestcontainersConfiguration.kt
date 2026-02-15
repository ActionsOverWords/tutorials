package tutorials.keycloak

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.MountableFile

@TestConfiguration(proxyBeanMethods = false)
open class TestcontainersConfiguration {

  companion object {
    private const val REALM_NAME = "tutorials"
    private const val KEYCLOAK_PORT = 8080

    @Container
    @JvmStatic
    val keycloak = GenericContainer(DockerImageName.parse("quay.io/keycloak/keycloak:26.5.3"))
      .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
      .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin")
      .withCommand("start-dev", "--import-realm")
      .withCopyFileToContainer(
        MountableFile.forHostPath("docker/keycloak/tutorials-realm.json"),
        "/opt/keycloak/data/import/tutorials-realm.json"
      )
      .withExposedPorts(KEYCLOAK_PORT)

    @JvmStatic
    @DynamicPropertySource
    fun registerProperties(registry: DynamicPropertyRegistry) {
      registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri") { issuerUri() }
      registry.add("spring.security.oauth2.client.provider.keycloak.issuer-uri") { issuerUri() }
    }

    fun issuerUri(): String {
      return "http://${keycloak.host}:${keycloak.getMappedPort(KEYCLOAK_PORT)}/realms/$REALM_NAME"
    }
  }
}
