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
class InfluxDb2Properties {
  @NotBlank lateinit var url: String
  @NotBlank lateinit var token: String
  @NotBlank lateinit var org: String
  @NotBlank lateinit var bucket: String
}

@Configuration
@EnableConfigurationProperties(InfluxDb2Properties::class)
class InfluxDb2Config {

  @Bean(destroyMethod = "close")
  fun influxDB2Client(properties: InfluxDb2Properties): InfluxDBClient {
    return InfluxDBClientFactory.create(
      properties.url,
      properties.token.toCharArray(),
      properties.org,
      properties.bucket
    )
  }

  @Bean(destroyMethod = "close")
  fun influxDB2ClientReactive(properties: InfluxDb2Properties): InfluxDBClientReactive {
    return InfluxDBClientReactiveFactory.create(
      properties.url,
      properties.token.toCharArray(),
      properties.org,
      properties.bucket
    )
  }

}
