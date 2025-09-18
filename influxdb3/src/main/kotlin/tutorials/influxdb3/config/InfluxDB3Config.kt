package tutorials.influxdb3.config

import com.influxdb.v3.client.InfluxDBClient
import com.influxdb.v3.client.config.ClientConfig
import com.influxdb.v3.client.write.WritePrecision
import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.validation.annotation.Validated
import java.time.Duration

@Validated
@ConfigurationProperties(prefix = "influxdb3")
class InfluxDB3Properties {
  @NotBlank lateinit var url: String
  @NotBlank lateinit var token: String
  @NotBlank lateinit var database: String
}

@Configuration
@EnableConfigurationProperties(InfluxDB3Properties::class)
class InfluxDB3Config {

  @Bean(destroyMethod = "close")
  fun influxDBClient(properties: InfluxDB3Properties): InfluxDBClient {
    val clientConfig = ClientConfig.Builder()
      .host(properties.url)
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

}
