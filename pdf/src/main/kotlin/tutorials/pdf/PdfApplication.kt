package tutorials.pdf

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class PdfApplication

fun main(args: Array<String>) {
  runApplication<PdfApplication>(*args)
}
