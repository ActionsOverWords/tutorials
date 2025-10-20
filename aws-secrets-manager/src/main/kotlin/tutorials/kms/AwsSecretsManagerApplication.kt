package tutorials.kms

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AwsSecretsManagerApplication

fun main(args: Array<String>) {
  runApplication<AwsSecretsManagerApplication>(*args)
}
