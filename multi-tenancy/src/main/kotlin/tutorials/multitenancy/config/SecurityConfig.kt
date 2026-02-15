package tutorials.multitenancy.config

import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import tutorials.multitenancy.base.extensions.logger
import tutorials.multitenancy.security.jwt.JwtAuthenticationFilter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
  private val jwtAuthenticationFilter: JwtAuthenticationFilter,
  private val userDetailsService: UserDetailsService
) {
  private val logger by logger()

  @Bean
  fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

  @Bean
  fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager =
    config.authenticationManager

  @PostConstruct
  fun logAuthenticationProviders() {
    logger.info("=== SecurityConfig initialized ===")
    logger.info("UserDetailsService: ${userDetailsService.javaClass.simpleName}")
  }

  @Bean
  fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
    val unauthorizedEntryPoint = AuthenticationEntryPoint { _, response, _ ->
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
    }

    http
      .csrf { it.disable() }
      .authorizeHttpRequests { authorize ->
        authorize
          .requestMatchers("/auth/login").permitAll()
          .anyRequest().authenticated()
      }
      .exceptionHandling { exceptions ->
        exceptions.authenticationEntryPoint(unauthorizedEntryPoint)
      }
      .sessionManagement { session ->
        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      }
      .userDetailsService(userDetailsService)
      .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

    return http.build()
  }
}
