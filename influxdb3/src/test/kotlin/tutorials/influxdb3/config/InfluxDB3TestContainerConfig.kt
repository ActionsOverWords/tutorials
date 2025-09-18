package tutorials.influxdb3.config

/*
import com.influxdb.v3.client.InfluxDBClient
import com.influxdb.v3.client.config.ClientConfig
import com.influxdb.v3.client.write.WritePrecision
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration

@TestConfiguration(proxyBeanMethods = false)
class InfluxDB3TestContainerConfig {

  companion object {
    private const val INFLUX_DB_3_PORT = 8081
    private const val HOST = "http://localhost:$INFLUX_DB_3_PORT"
    private const val TOKEN = ""
    private const val DATABASE = "tutorials"

    private val INFLUX_DB3_IMAGE_NAME = DockerImageName.parse("influxdb:3.4.2-core")

    @JvmStatic
    @ServiceConnection
    val influxDb3Container: GenericContainer<*> = GenericContainer(INFLUX_DB3_IMAGE_NAME)
      .withExposedPorts(INFLUX_DB_3_PORT)
      .withEnv("INFLUXDB_TOKEN", TOKEN)
      .withEnv("INFLUXDB3_AUTH_TOKEN", TOKEN)
      .withCommand("influxdb3", "serve", "--node-id=node0", "--object-store=memory")
      //.waitingFor(Wait.forHttp("/health").forPort(INFLUX_DB_3_PORT))
      //.waitingFor(Wait.forListeningPort())
  }

  @Bean
  fun influxDbContainer(): GenericContainer<*> = influxDb3Container

  @Bean
  @Primary
  fun influxDbClient(): InfluxDBClient {
    val clientConfig = ClientConfig.Builder()
      .host(HOST)
      .token(TOKEN.toCharArray())
      .database(DATABASE)
      .writePrecision(WritePrecision.MS)
      .timeout(Duration.ofSeconds(5))
      .allowHttpRedirects(true)
      .headers(
        mapOf("Authorization" to "Bearer $TOKEN")
      )
      .build()

    return InfluxDBClient.getInstance(clientConfig)
  }

}
*/
