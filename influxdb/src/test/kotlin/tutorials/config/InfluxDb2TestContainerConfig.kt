package tutorials.config

import com.influxdb.client.InfluxDBClient
import com.influxdb.client.InfluxDBClientFactory
import com.influxdb.client.reactive.InfluxDBClientReactive
import com.influxdb.client.reactive.InfluxDBClientReactiveFactory
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class InfluxDB2TestContainerConfig {

  companion object {
    private const val INFLUX_DB_2_PORT = 8086
    private const val ORG = "test-org"
    const val BUCKET = "test-bucket"
    private const val USERNAME = "test-user"
    private const val PASSWORD = "test-password"
    private const val TOKEN = "test-token"

    private val INFLUX_DB2_IMAGE_NAME = DockerImageName.parse("influxdb:2.7")

    @JvmStatic
    @ServiceConnection
    val influxDb2Container: GenericContainer<*> = GenericContainer(INFLUX_DB2_IMAGE_NAME)
      .withEnv("DOCKER_INFLUXDB_INIT_MODE", "setup")
      .withEnv("DOCKER_INFLUXDB_INIT_USERNAME", USERNAME)
      .withEnv("DOCKER_INFLUXDB_INIT_PASSWORD", PASSWORD)
      .withEnv("DOCKER_INFLUXDB_INIT_ORG", ORG)
      .withEnv("DOCKER_INFLUXDB_INIT_BUCKET", BUCKET)
      .withEnv("DOCKER_INFLUXDB_INIT_ADMIN_TOKEN", TOKEN)
      .withExposedPorts(INFLUX_DB_2_PORT)
      .waitingFor(Wait.forHttp("/health").forPort(INFLUX_DB_2_PORT))
  }

  @Bean
  fun influxDb2Container(): GenericContainer<*> = influxDb2Container

  @Bean
  @Primary
  fun influxDb2Client(): InfluxDBClient {
    val url = "http://${influxDb2Container.host}:${influxDb2Container.firstMappedPort}"
    return InfluxDBClientFactory.create(url, TOKEN.toCharArray(), ORG, BUCKET)
  }

  @Bean
  @Primary
  fun influxDb2ClientReactive(): InfluxDBClientReactive {
    val url = "http://${influxDb2Container.host}:${influxDb2Container.firstMappedPort}"
    return InfluxDBClientReactiveFactory.create(url, TOKEN.toCharArray(), ORG, BUCKET)
  }

}
