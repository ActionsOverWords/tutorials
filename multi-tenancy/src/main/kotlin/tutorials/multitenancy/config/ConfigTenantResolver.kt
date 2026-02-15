package tutorials.multitenancy.config

import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Component

@Component
class ConfigTenantResolver(
  private val tenantDataSourceProperties: TenantDataSourceProperties
) : TenantResolver {

  private val tenantsByLowercase: Map<String, String> by lazy {
    tenantDataSourceProperties.tenants.keys.associateBy { it.lowercase() }
  }

  override fun resolve(tenant: String?): String {
    val normalized = tenant?.trim()?.lowercase()
      ?: throw BadCredentialsException("Tenant is required")

    if (normalized.isBlank()) {
      throw BadCredentialsException("Tenant is required")
    }

    return tenantsByLowercase[normalized]
      ?: throw BadCredentialsException("Invalid tenant")
  }
}
