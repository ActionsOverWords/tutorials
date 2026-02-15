package tutorials.multitenancy.config

interface TenantResolver {
  fun resolve(tenant: String?): String
}
