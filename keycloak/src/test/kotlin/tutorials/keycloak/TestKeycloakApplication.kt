package tutorials.keycloak

import org.springframework.boot.fromApplication
import org.springframework.boot.with

fun main(args: Array<String>) {
  fromApplication<KeycloakApplication>().with(TestcontainersConfiguration::class).run(*args)
}
