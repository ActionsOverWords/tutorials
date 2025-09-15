package tutorials

import org.springframework.boot.fromApplication
import org.springframework.boot.with
import tutorials.config.InfluxDb2TestContainerConfig

fun main(args: Array<String>) {
  fromApplication<InfluxdbApplication>()
    .with(InfluxDb2TestContainerConfig::class)
    .run(*args)
}
