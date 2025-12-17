package tutorials.javers

import org.springframework.boot.fromApplication
import org.springframework.boot.with

fun main(args: Array<String>) {
  fromApplication<JaversApplication>().with(TestcontainersConfiguration::class).run(*args)
}
