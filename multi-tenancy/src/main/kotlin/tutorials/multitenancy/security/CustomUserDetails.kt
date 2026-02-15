package tutorials.multitenancy.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class CustomUserDetails(
  private val username: String,
  private val password: String,
  private val authorities: Collection<GrantedAuthority>,
  val tenantId: String,
  private val enabled: Boolean = true
) : UserDetails {

  override fun getAuthorities(): Collection<GrantedAuthority> = authorities

  override fun getPassword(): String = password

  override fun getUsername(): String = username

  override fun isAccountNonExpired(): Boolean = true

  override fun isAccountNonLocked(): Boolean = true

  override fun isCredentialsNonExpired(): Boolean = true

  override fun isEnabled(): Boolean = enabled
}
