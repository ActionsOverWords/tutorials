package tutorials

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class InfluxdbApplication

fun main(args: Array<String>) {
  runApplication<InfluxdbApplication>(*args)
}
