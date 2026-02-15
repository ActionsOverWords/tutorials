package tutorials.multitenancy.service

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import tutorials.multitenancy.config.TenantContext
import tutorials.multitenancy.config.TenantResolver
import tutorials.multitenancy.domain.User
import tutorials.multitenancy.dto.LoginRequest
import tutorials.multitenancy.repository.UserRepository
import tutorials.multitenancy.security.jwt.JwtProvider
import java.util.UUID

class AuthenticationServiceTest {

  private val userRepository: UserRepository = mock(UserRepository::class.java)
  private val passwordEncoder: PasswordEncoder = mock(PasswordEncoder::class.java)
  private val jwtProvider: JwtProvider = mock(JwtProvider::class.java)
  private val tenantResolver: TenantResolver = mock(TenantResolver::class.java)
  private val authenticationService = AuthenticationService(
    userRepository,
    passwordEncoder,
    jwtProvider,
    tenantResolver
  )

  @AfterEach
  fun tearDown() {
    TenantContext.clear()
  }

  @Test
  fun `유효한 자격증명과 테넌트로 로그인이 성공해야 한다`() {
    val request = LoginRequest(username = "tenancy-A", password = "tutorial", tenant = "tenant-a")
    val user = User(
      id = UUID.randomUUID().toString(),
      username = "tenancy-A",
      password = "encoded-password",
      tenantId = "tenant-a",
      enabled = true
    )
    val token = "jwt-token"

    `when`(tenantResolver.resolve("tenant-a")).thenReturn("tenant-a")
    `when`(userRepository.findByUsernameAndEnabledTrue("tenancy-A")).thenReturn(user)
    `when`(passwordEncoder.matches("tutorial", "encoded-password")).thenReturn(true)
    `when`(jwtProvider.generateToken("tenancy-A", "tenant-a")).thenReturn(token)

    val response = authenticationService.login(request)

    assertNotNull(response)
    assertEquals(token, response.token)
    assertEquals("tenancy-A", response.username)
    assertEquals("tenant-a", response.tenant)
    assertNull(TenantContext.getTenant(), "TenantContext should be cleared after login")

    verify(tenantResolver, times(1)).resolve("tenant-a")
    verify(userRepository, times(1)).findByUsernameAndEnabledTrue("tenancy-A")
    verify(passwordEncoder, times(1)).matches("tutorial", "encoded-password")
    verify(jwtProvider, times(1)).generateToken("tenancy-A", "tenant-a")
  }

  @Test
  fun `존재하지 않는 사용자로 로그인 시 실패해야 한다`() {
    val request = LoginRequest(username = "tenancy-A", password = "tutorial", tenant = "tenant-a")

    `when`(tenantResolver.resolve("tenant-a")).thenReturn("tenant-a")
    `when`(userRepository.findByUsernameAndEnabledTrue("tenancy-A")).thenReturn(null)

    val exception = assertThrows<BadCredentialsException> {
      authenticationService.login(request)
    }

    assertTrue(exception.message!!.contains("Invalid username or password"))
    assertNull(TenantContext.getTenant(), "TenantContext should be cleared even on failure")
    verify(jwtProvider, never()).generateToken(anyString(), anyString())
  }

  @Test
  fun `잘못된 비밀번호로 로그인 시 실패해야 한다`() {
    val request = LoginRequest(username = "tenancy-A", password = "wrong-password", tenant = "tenant-a")
    val user = User(
      id = UUID.randomUUID().toString(),
      username = "tenancy-A",
      password = "encoded-password",
      tenantId = "tenant-a",
      enabled = true
    )

    `when`(tenantResolver.resolve("tenant-a")).thenReturn("tenant-a")
    `when`(userRepository.findByUsernameAndEnabledTrue("tenancy-A")).thenReturn(user)
    `when`(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false)

    val exception = assertThrows<BadCredentialsException> {
      authenticationService.login(request)
    }

    assertTrue(exception.message!!.contains("Invalid username or password"))
    assertNull(TenantContext.getTenant())
    verify(jwtProvider, never()).generateToken(anyString(), anyString())
  }

  @Test
  fun `요청 테넌트와 사용자 테넌트가 다르면 로그인에 실패해야 한다`() {
    val request = LoginRequest(username = "tenancy-A", password = "tutorial", tenant = "tenant-a")
    val user = User(
      id = UUID.randomUUID().toString(),
      username = "tenancy-A",
      password = "encoded-password",
      tenantId = "tenant-b",
      enabled = true
    )

    `when`(tenantResolver.resolve("tenant-a")).thenReturn("tenant-a")
    `when`(userRepository.findByUsernameAndEnabledTrue("tenancy-A")).thenReturn(user)
    `when`(passwordEncoder.matches("tutorial", "encoded-password")).thenReturn(true)

    val exception = assertThrows<BadCredentialsException> {
      authenticationService.login(request)
    }

    assertTrue(exception.message!!.contains("Tenant mismatch"))
    assertNull(TenantContext.getTenant())
    verify(jwtProvider, never()).generateToken(anyString(), anyString())
  }

  @Test
  fun `테넌트 검증 실패 시 로그인에 실패해야 한다`() {
    val request = LoginRequest(username = "tenancy-A", password = "tutorial", tenant = "unknown")

    `when`(tenantResolver.resolve("unknown"))
      .thenThrow(BadCredentialsException("Invalid tenant"))

    val exception = assertThrows<BadCredentialsException> {
      authenticationService.login(request)
    }

    assertTrue(exception.message!!.contains("Invalid tenant"))
    assertNull(TenantContext.getTenant())
    verify(userRepository, never()).findByUsernameAndEnabledTrue(anyString())
  }

  @Test
  fun `예외 발생 시에도 TenantContext가 정리되어야 한다`() {
    val request = LoginRequest(username = "tenancy-A", password = "tutorial", tenant = "tenant-a")
    `when`(tenantResolver.resolve("tenant-a")).thenReturn("tenant-a")
    `when`(userRepository.findByUsernameAndEnabledTrue("tenancy-A"))
      .thenThrow(RuntimeException("Database error"))

    assertThrows<RuntimeException> {
      authenticationService.login(request)
    }
    assertNull(TenantContext.getTenant(), "TenantContext should be cleared even on exception")
  }

  @Test
  fun `인증 중에는 올바른 TenantContext가 설정되어야 한다`() {
    val request = LoginRequest(username = "tenancy-A", password = "tutorial", tenant = "tenant-a")
    val user = User(
      id = UUID.randomUUID().toString(),
      username = "tenancy-A",
      password = "encoded-password",
      tenantId = "tenant-a",
      enabled = true
    )

    var capturedTenant: String? = null
    `when`(tenantResolver.resolve("tenant-a")).thenReturn("tenant-a")
    `when`(userRepository.findByUsernameAndEnabledTrue("tenancy-A")).thenAnswer {
      capturedTenant = TenantContext.getTenant()
      user
    }
    `when`(passwordEncoder.matches("tutorial", "encoded-password")).thenReturn(true)
    `when`(jwtProvider.generateToken("tenancy-A", "tenant-a")).thenReturn("token")

    authenticationService.login(request)

    assertEquals("tenant-a", capturedTenant)
    assertNull(TenantContext.getTenant())
  }
}
