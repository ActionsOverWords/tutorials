package tutorials

import org.springframework.boot.fromApplication
import org.springframework.boot.with
import tutorials.config.InfluxDB2TestContainerConfig

fun main(args: Array<String>) {
  fromApplication<InfluxdbApplication>()
    .with(InfluxDB2TestContainerConfig::class)
    .run(*args)
}
