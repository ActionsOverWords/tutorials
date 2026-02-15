package tutorials.multitenancy.security.jwt

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
  var secret: String = "",
  var expirationMs: Long = 3600000
)
