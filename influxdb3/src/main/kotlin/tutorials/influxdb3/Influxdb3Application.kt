package tutorials.influxdb3

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class Influxdb3Application

fun main(args: Array<String>) {
  runApplication<Influxdb3Application>(*args)
}
