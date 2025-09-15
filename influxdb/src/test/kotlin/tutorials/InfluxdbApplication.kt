package tutorials

import org.springframework.boot.fromApplication
import org.springframework.boot.with
import tutorials.config.InfluxDbTestContainerConfig

fun main(args: Array<String>) {
  fromApplication<InfluxdbApplication>()
    .with(InfluxDbTestContainerConfig::class)
    .run(*args)
}
