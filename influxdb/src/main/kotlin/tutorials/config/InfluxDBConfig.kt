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
@ConfigurationProperties(prefix = "influxdb")
class InfluxDbProperties {
  @NotBlank lateinit var url: String
  @NotBlank lateinit var token: String
  @NotBlank lateinit var org: String
  @NotBlank lateinit var bucket: String
}

@Configuration
@EnableConfigurationProperties(InfluxDbProperties::class)
class InfluxConfig {

  @Bean(destroyMethod = "close")
  fun influxDBClient(properties: InfluxDbProperties): InfluxDBClient {
    return InfluxDBClientFactory.create(
      properties.url,
      properties.token.toCharArray(),
      properties.org,
      properties.bucket
    )
  }

  @Bean(destroyMethod = "close")
  fun influxDBClientReactive(properties: InfluxDbProperties): InfluxDBClientReactive {
    return InfluxDBClientReactiveFactory.create(
      properties.url,
      properties.token.toCharArray(),
      properties.org,
      properties.bucket
    )
  }

}
