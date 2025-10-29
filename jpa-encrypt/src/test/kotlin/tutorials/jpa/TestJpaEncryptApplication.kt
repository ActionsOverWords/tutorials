package tutorials.jpa

import org.springframework.boot.fromApplication
import org.springframework.boot.with
import tutorials.jpa.config.TestcontainersConfiguration

fun main(args: Array<String>) {
  fromApplication<JpaEncryptApplication>()
    .with(TestcontainersConfiguration::class)
    .run(*args)
}
