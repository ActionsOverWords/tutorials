package tutorials.proxysql

import org.springframework.boot.fromApplication
import org.springframework.boot.with

fun main(args: Array<String>) {
  fromApplication<ProxysqlApplication>().with(TestcontainersConfiguration::class).run(*args)
}
