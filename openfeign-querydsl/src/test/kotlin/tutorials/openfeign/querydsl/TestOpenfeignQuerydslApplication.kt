package tutorials.openfeign.querydsl

import org.springframework.boot.fromApplication
import org.springframework.boot.with
import tutorials.openfeign.querydsl.config.TestcontainersConfiguration

fun main(args: Array<String>) {
  fromApplication<OpenfeignQuerydslApplication>()
    .with(TestcontainersConfiguration::class)
    .run(*args)
}
