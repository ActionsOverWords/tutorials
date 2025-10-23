package tutorials.lgtm

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class LgtmStackApplication

fun main(args: Array<String>) {
	runApplication<LgtmStackApplication>(*args)
}
