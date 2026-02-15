package tutorials.keycloak.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant

class JwtAudienceValidatorTests {

  @Test
  fun `returns success when required audience exists`() {
    val validator = JwtAudienceValidator("tutorials-keycloak")
    val jwt = jwtWithAudience("tutorials-keycloak")

    val result = validator.validate(jwt)

    assertThat(result.hasErrors()).isFalse()
  }

  @Test
  fun `returns failure when required audience is missing`() {
    val validator = JwtAudienceValidator("tutorials-keycloak")
    val jwt = jwtWithAudience("another-client")

    val result = validator.validate(jwt)

    assertThat(result.hasErrors()).isTrue()
    assertThat(result.errors).anySatisfy {
      assertThat(it.errorCode).isEqualTo("invalid_token")
      assertThat(it.description).isEqualTo("The required audience is missing")
    }
  }

  private fun jwtWithAudience(vararg audience: String): Jwt {
    val now = Instant.parse("2026-01-01T00:00:00Z")

    return Jwt.withTokenValue("token-value")
      .header("alg", "none")
      .subject("demo-user")
      .claim("aud", audience.toList())
      .issuedAt(now)
      .expiresAt(now.plusSeconds(300))
      .build()
  }
}
