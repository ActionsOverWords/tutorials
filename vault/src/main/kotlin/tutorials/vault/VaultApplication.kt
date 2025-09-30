package tutorials.vault

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class VaultApplication

fun main(args: Array<String>) {
	runApplication<VaultApplication>(*args)
}
