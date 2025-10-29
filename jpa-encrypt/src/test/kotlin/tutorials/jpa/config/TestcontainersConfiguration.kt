package tutorials.jpa.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

  @Bean
  @ServiceConnection
  fun mariaDbContainer(): MariaDBContainer<*> {
    return MariaDBContainer(DockerImageName.parse("mariadb:latest"))
      .withDatabaseName("tutorials")
  }

}
