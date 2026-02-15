package tutorials.multitenancy.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "datasource")
data class TenantDataSourceProperties(
  val tenants: Map<String, TenantConfig> = emptyMap()
)

data class TenantConfig(
  val url: String,
  val username: String,
  val password: String,
  val driverClassName: String = "org.mariadb.jdbc.Driver"
)
