package tutorials.jpa

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class JpaEncryptApplication

fun main(args: Array<String>) {
  runApplication<JpaEncryptApplication>(*args)
}
