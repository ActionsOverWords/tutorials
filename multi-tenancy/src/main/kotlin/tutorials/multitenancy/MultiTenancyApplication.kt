package tutorials.multitenancy

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import tutorials.multitenancy.security.jwt.JwtProperties

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties::class)
class MultiTenancyApplication

fun main(args: Array<String>) {
  runApplication<MultiTenancyApplication>(*args)
}
