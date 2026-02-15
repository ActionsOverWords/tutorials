package tutorials.multitenancy.security.jwt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.authentication.BadCredentialsException

class JwtProviderTest {

  private lateinit var jwtProvider: JwtProvider
  private lateinit var jwtProperties: JwtProperties

  @BeforeEach
  fun setUp() {
    jwtProperties = JwtProperties(
      secret = "test-secret-key-that-is-long-enough-for-hs256-algorithm",
      expirationMs = 3600000 // 1 hour
    )
    jwtProvider = JwtProvider(jwtProperties)
  }

  @Test
  fun `토큰 생성 시 사용자명과 테넌트 정보를 포함한 유효한 JWT를 생성해야 한다`() {
    val username = "tenancy-A"
    val tenant = "tenant-a"

    val token = jwtProvider.generateToken(username, tenant)

    assertNotNull(token)
    assertTrue(token.isNotEmpty())
    assertTrue(token.split(".").size == 3)
  }

  @Test
  fun `유효한 토큰에 대해 검증이 성공해야 한다`() {
    val username = "tenancy-A"
    val tenant = "tenant-a"
    val token = jwtProvider.generateToken(username, tenant)

    val claims = jwtProvider.parseAndValidate(token)

    assertEquals(username, claims.username)
    assertEquals(tenant, claims.tenant)
  }

  @Test
  fun `유효하지 않은 토큰에 대해 검증이 실패해야 한다`() {
    val invalidToken = "invalid.jwt.token"

    assertThrows<BadCredentialsException> {
      jwtProvider.parseAndValidate(invalidToken)
    }
  }

  @Test
  fun `만료된 토큰에 대해 검증이 실패해야 한다`() {
    val expiredProperties = JwtProperties(
      secret = "test-secret-key-that-is-long-enough-for-hs256-algorithm",
      expirationMs = -1000 // Expired 1 second ago
    )
    val expiredJwtProvider = JwtProvider(expiredProperties)
    val token = expiredJwtProvider.generateToken("tenancy-A", "tenant-a")

    assertThrows<BadCredentialsException> {
      expiredJwtProvider.parseAndValidate(token)
    }
  }

  @Test
  fun `변조된 토큰에 대해 검증이 실패해야 한다`() {
    val token = jwtProvider.generateToken("tenancy-A", "tenant-a")
    val tamperedToken = token.substring(0, token.length - 5) + "xxxxx"

    assertThrows<BadCredentialsException> {
      jwtProvider.parseAndValidate(tamperedToken)
    }
  }

  @Test
  fun `서로 다른 사용자에 대해 서로 다른 토큰을 생성해야 한다`() {
    val token1 = jwtProvider.generateToken("tenancy-A", "tenant-a")
    val token2 = jwtProvider.generateToken("tenancy-B", "tenant-b")

    assertNotEquals(token1, token2)
    assertEquals("tenancy-A", jwtProvider.parseAndValidate(token1).username)
    assertEquals("tenancy-B", jwtProvider.parseAndValidate(token2).username)
    assertEquals("tenant-a", jwtProvider.parseAndValidate(token1).tenant)
    assertEquals("tenant-b", jwtProvider.parseAndValidate(token2).tenant)
  }

  @Test
  fun `서로 다른 시간에 생성된 토큰은 달라야 한다`() {
    val username = "tenancy-A"
    val tenant = "tenant-a"

    val token1 = jwtProvider.generateToken(username, tenant)
    Thread.sleep(1000) // Wait 1 second to ensure different timestamp
    val token2 = jwtProvider.generateToken(username, tenant)

    assertNotEquals(token1, token2, "Tokens should be different due to different issue times")
  }
}
