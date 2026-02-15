package tutorials.keycloak.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.util.matcher.RegexRequestMatcher
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

  @Bean
  fun securityFilterChain(
    http: HttpSecurity,
    clientRegistrationRepository: ClientRegistrationRepository
  ): SecurityFilterChain {
    http
      .csrf { csrf -> csrf.disable() }
      .authorizeHttpRequests {
        it
          .requestMatchers("/api/secure", "/api/secure/**").authenticated()
          .anyRequest().permitAll()
      }
      .oauth2Login {
        it.defaultSuccessUrl("/", true)
      }
      .logout {
        it.logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository))
      }
      .oauth2ResourceServer { oauth2 -> oauth2.jwt {} }
      .exceptionHandling {
        it.defaultAuthenticationEntryPointFor(
          HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
          RegexRequestMatcher("^/api/.*", null)
        )
      }

    return http.build()
  }

  private fun oidcLogoutSuccessHandler(
    clientRegistrationRepository: ClientRegistrationRepository
  ): OidcClientInitiatedLogoutSuccessHandler {
    val handler = OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository)
    handler.setPostLogoutRedirectUri("{baseUrl}/")
    return handler
  }

  @Bean
  fun jwtDecoder(
    @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    issuerUri: String,
    @Value("\${spring.security.oauth2.client.registration.keycloak.client-id}")
    audience: String
  ): JwtDecoder {
    val normalizedIssuerUri = issuerUri.trimEnd('/')
    val jwtDecoder = NimbusJwtDecoder.withJwkSetUri("$normalizedIssuerUri/protocol/openid-connect/certs")
      .build()
    val issuerValidator = JwtValidators.createDefaultWithIssuer(normalizedIssuerUri)
    val audienceValidator = JwtAudienceValidator(audience)

    jwtDecoder.setJwtValidator(DelegatingOAuth2TokenValidator(issuerValidator, audienceValidator))
    return jwtDecoder
  }
}
