package tutorials.multitenancy.security.jwt

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import tutorials.multitenancy.base.extensions.logger
import tutorials.multitenancy.config.TenantContext
import tutorials.multitenancy.config.TenantResolver
import tutorials.multitenancy.security.CustomUserDetails

@Component
class JwtAuthenticationFilter(
  private val jwtProvider: JwtProvider,
  private val userDetailsService: UserDetailsService,
  private val tenantResolver: TenantResolver
) : OncePerRequestFilter() {

  private val log by logger()

  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain
  ) {
    try {
      val jwt = extractJwtFromRequest(request)

      if (jwt != null) {
        val claims = jwtProvider.parseAndValidate(jwt)
        val username = claims.username
        val tenant = tenantResolver.resolve(claims.tenant)

        TenantContext.setTenant(tenant)
        log.debug("Tenant context set to: $tenant")

        val userDetails = userDetailsService.loadUserByUsername(username)

        if (userDetails is CustomUserDetails) {
          if (userDetails.tenantId != tenant) {
            log.warn("Tenant mismatch: JWT=$tenant, User=${userDetails.tenantId} for username=$username")
            throw BadCredentialsException("Tenant validation failed")
          }
        }

        val authentication = UsernamePasswordAuthenticationToken(
          userDetails, null, userDetails.authorities
        )
        authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
        SecurityContextHolder.getContext().authentication = authentication
      }

      filterChain.doFilter(request, response)
    } catch (e: AuthenticationException) {
      SecurityContextHolder.clearContext()
      log.warn("Authentication failed: ${e.message}")
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.message ?: "Unauthorized")
    } catch (e: Exception) {
      SecurityContextHolder.clearContext()
      log.error("Authentication processing failed: ${e.message}", e)
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication processing failed")
    } finally {
      TenantContext.clear()
    }
  }

  private fun extractJwtFromRequest(request: HttpServletRequest): String? {
    val bearerToken = request.getHeader("Authorization")
    return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
      bearerToken.substring(7)
    } else null
  }
}
