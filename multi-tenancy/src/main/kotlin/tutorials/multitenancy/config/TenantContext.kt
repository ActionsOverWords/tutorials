package tutorials.multitenancy.config

object TenantContext {
  private val currentTenant = ThreadLocal<String>()

  fun setTenant(tenant: String) {
    currentTenant.set(tenant)
  }

  fun getTenant(): String? {
    return currentTenant.get()
  }

  fun clear() {
    currentTenant.remove()
  }
}
