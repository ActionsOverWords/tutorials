package tutorials.keycloak

import org.junit.jupiter.api.Test
import tutorials.keycloak.config.KeycloakContainerTest

@KeycloakContainerTest
class KeycloakApplicationTests : TestcontainersConfiguration() {

  @Test
  fun contextLoads() {
  }
}
