package tutorials.config

import com.influxdb.client.InfluxDBClient
import com.influxdb.client.InfluxDBClientFactory
import com.influxdb.client.reactive.InfluxDBClientReactive
import com.influxdb.client.reactive.InfluxDBClientReactiveFactory
import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "influxdb2")
class InfluxDB2Properties {
  @NotBlank lateinit var url: String
  @NotBlank lateinit var token: String
  @NotBlank lateinit var org: String
  @NotBlank lateinit var bucket: String
}

@Configuration
@EnableConfigurationProperties(InfluxDB2Properties::class)
class InfluxDB2Config {

  @Bean(destroyMethod = "close")
  fun influxDB2Client(properties: InfluxDB2Properties): InfluxDBClient {
    return InfluxDBClientFactory.create(
      properties.url,
      properties.token.toCharArray(),
      properties.org,
      properties.bucket
    )
  }

  @Bean(destroyMethod = "close")
  fun influxDB2ClientReactive(properties: InfluxDB2Properties): InfluxDBClientReactive {
    return InfluxDBClientReactiveFactory.create(
      properties.url,
      properties.token.toCharArray(),
      properties.org,
      properties.bucket
    )
  }

}
