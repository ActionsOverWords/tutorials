package tutorials.jpa.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "kms")
data class KmsProperties(
  val db: DbProperties,
)

data class DbProperties(
  val secretKey: String,
)
