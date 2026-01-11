package tutorials.proxysql

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ProxysqlApplication

fun main(args: Array<String>) {
  runApplication<ProxysqlApplication>(*args)
}
