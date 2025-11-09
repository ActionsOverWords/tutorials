package tutorials.springdocs

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringRestDocSwaggerApplication

fun main(args: Array<String>) {
  runApplication<SpringRestDocSwaggerApplication>(*args)
}
