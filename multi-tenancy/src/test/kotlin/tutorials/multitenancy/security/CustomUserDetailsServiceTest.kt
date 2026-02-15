package tutorials.multitenancy.security

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import org.springframework.security.core.userdetails.UsernameNotFoundException
import tutorials.multitenancy.domain.User
import tutorials.multitenancy.repository.UserRepository
import java.time.Instant
import java.util.*

class CustomUserDetailsServiceTest {

  private lateinit var userRepository: UserRepository
  private lateinit var userDetailsService: CustomUserDetailsService

  @BeforeEach
  fun setUp() {
    userRepository = mock(UserRepository::class.java)
    userDetailsService = CustomUserDetailsService(userRepository)
  }

  @Test
  fun `사용자 조회 시 UserDetails를 반환해야 한다`() {
      val username = "tenancy-A"
    val user = User(
      id = UUID.randomUUID().toString(),
      username = username,
      password = "encoded-password",
      tenantId = "tenant-a",
      createdAt = Instant.now(),
      enabled = true
    )
    `when`(userRepository.findByUsernameAndEnabledTrue(username)).thenReturn(user)

      val userDetails = userDetailsService.loadUserByUsername(username)

      assertNotNull(userDetails)
    assertEquals(username, userDetails.username)
    assertEquals("encoded-password", userDetails.password)
    assertTrue(userDetails.isEnabled)
    assertTrue(userDetails is CustomUserDetails)
    assertEquals("tenant-a", (userDetails as CustomUserDetails).tenantId)
    verify(userRepository, times(1)).findByUsernameAndEnabledTrue(username)
  }

  @Test
  fun `존재하지 않는 사용자 조회 시 예외가 발생해야 한다`() {
      val username = "non-existent-user"
    `when`(userRepository.findByUsernameAndEnabledTrue(username)).thenReturn(null)

    val exception = assertThrows<UsernameNotFoundException> {
      userDetailsService.loadUserByUsername(username)
    }
    assertTrue(exception.message!!.contains(username))
    verify(userRepository, times(1)).findByUsernameAndEnabledTrue(username)
  }

  @Test
  fun `비활성화된 사용자는 조회되지 않아야 한다`() {
      val username = "disabled-user"
    `when`(userRepository.findByUsernameAndEnabledTrue(username)).thenReturn(null)

    assertThrows<UsernameNotFoundException> {
      userDetailsService.loadUserByUsername(username)
    }
    verify(userRepository, times(1)).findByUsernameAndEnabledTrue(username)
  }

  @Test
  fun `tenant-a 사용자 조회 시 올바른 테넌트 정보를 반환해야 한다`() {
      val username = "tenancy-A"
    val user = User(
      id = UUID.randomUUID().toString(),
      username = username,
      password = "password-a",
      tenantId = "tenant-a",
      enabled = true
    )
    `when`(userRepository.findByUsernameAndEnabledTrue(username)).thenReturn(user)

      val userDetails = userDetailsService.loadUserByUsername(username) as CustomUserDetails

      assertEquals("tenant-a", userDetails.tenantId)
  }

  @Test
  fun `tenant-b 사용자 조회 시 올바른 테넌트 정보를 반환해야 한다`() {
      val username = "tenancy-B"
    val user = User(
      id = UUID.randomUUID().toString(),
      username = username,
      password = "password-b",
      tenantId = "tenant-b",
      enabled = true
    )
    `when`(userRepository.findByUsernameAndEnabledTrue(username)).thenReturn(user)

      val userDetails = userDetailsService.loadUserByUsername(username) as CustomUserDetails

      assertEquals("tenant-b", userDetails.tenantId)
  }

  @Test
  fun `사용자 조회 시 빈 권한 목록을 반환해야 한다`() {
      val username = "tenancy-A"
    val user = User(
      id = UUID.randomUUID().toString(),
      username = username,
      password = "password",
      tenantId = "tenant-a",
      enabled = true
    )
    `when`(userRepository.findByUsernameAndEnabledTrue(username)).thenReturn(user)

      val userDetails = userDetailsService.loadUserByUsername(username)

      assertTrue(userDetails.authorities.isEmpty())
  }

  @Test
  fun `여러 사용자를 순차적으로 조회할 수 있어야 한다`() {
      val user1 = User(
      id = UUID.randomUUID().toString(),
      username = "tenancy-A",
      password = "password-a",
      tenantId = "tenant-a",
      enabled = true
    )
    val user2 = User(
      id = UUID.randomUUID().toString(),
      username = "tenancy-B",
      password = "password-b",
      tenantId = "tenant-b",
      enabled = true
    )
    `when`(userRepository.findByUsernameAndEnabledTrue("tenancy-A")).thenReturn(user1)
    `when`(userRepository.findByUsernameAndEnabledTrue("tenancy-B")).thenReturn(user2)

      val userDetails1 = userDetailsService.loadUserByUsername("tenancy-A") as CustomUserDetails
    val userDetails2 = userDetailsService.loadUserByUsername("tenancy-B") as CustomUserDetails

      assertEquals("tenant-a", userDetails1.tenantId)
    assertEquals("tenant-b", userDetails2.tenantId)
    verify(userRepository, times(1)).findByUsernameAndEnabledTrue("tenancy-A")
    verify(userRepository, times(1)).findByUsernameAndEnabledTrue("tenancy-B")
  }
}
