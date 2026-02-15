package tutorials.multitenancy.security

import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import tutorials.multitenancy.repository.UserRepository

@Service
class CustomUserDetailsService(
  private val userRepository: UserRepository
) : UserDetailsService {

  override fun loadUserByUsername(username: String): UserDetails {
    val user = userRepository.findByUsernameAndEnabledTrue(username)
      ?: throw UsernameNotFoundException("User not found: $username")

    return CustomUserDetails(
      username = user.username,
      password = user.password,
      authorities = emptyList(),
      tenantId = user.tenantId,
      enabled = user.enabled
    )
  }
}
