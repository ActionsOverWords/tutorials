package tutorials.influxdb3.config

import com.influxdb.v3.client.InfluxDBClient
import com.influxdb.v3.client.config.ClientConfig
import com.influxdb.v3.client.write.WritePrecision
import org.apache.arrow.flight.CallOption
import org.apache.arrow.flight.FlightCallHeaders
import org.apache.arrow.flight.FlightClient
import org.apache.arrow.flight.HeaderCallOption
import org.apache.arrow.flight.Location
import org.apache.arrow.memory.BufferAllocator
import org.apache.arrow.memory.RootAllocator
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@ConfigurationProperties(prefix = "influxdb3")
data class InfluxDB3Properties (
  val host: String,
  val port: Int,
  val useTls: Boolean,
  val token: String,
  val database: String,
)

@Configuration
@EnableConfigurationProperties(InfluxDB3Properties::class)
class InfluxDB3Config {

  @Bean(destroyMethod = "close")
  fun influxDBClient(properties: InfluxDB3Properties): InfluxDBClient {
    val clientConfig = ClientConfig.Builder()
      .host("http://${properties.host}:${properties.port}")
      .token(properties.token.toCharArray())
      .database(properties.database)
      .writePrecision(WritePrecision.MS)
      .writeTimeout(Duration.ofSeconds(5))
      .queryTimeout(Duration.ofSeconds(10))
      .allowHttpRedirects(true)
      .headers(
        mapOf("Authorization" to "Bearer ${properties.token}")
      )
      .build()

    return InfluxDBClient.getInstance(clientConfig)
  }

  @Bean
  fun bufferAllocator(): BufferAllocator = RootAllocator()

  @Bean(destroyMethod = "close")
  fun flightClient(properties: InfluxDB3Properties, allocator: BufferAllocator): FlightClient {
    val location = if (properties.useTls) {
      Location.forGrpcTls(properties.host, properties.port)
    } else {
      Location.forGrpcInsecure(properties.host, properties.port)
    }

    return FlightClient.builder(allocator, location).build()
  }

  @Bean
  fun callOption(properties: InfluxDB3Properties): CallOption {
    val headers = FlightCallHeaders()
    headers.insert("database", properties.database)
    headers.insert("authorization", "Bearer ${properties.token}")
    return HeaderCallOption(headers)
  }

}
