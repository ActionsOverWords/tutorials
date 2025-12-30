package tutorials.pdf.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "external")
data class ExternalProperties (
  val quickchartUrl: String,
  val gotenbergUrl: String,
)
