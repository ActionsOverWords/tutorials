package tutorials.multitenancy.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.assertj.MockMvcTester
import tutorials.multitenancy.config.AbstractTests.AbstractIntegrationTest
import tutorials.multitenancy.config.TenantContext
import tutorials.multitenancy.dto.LoginRequest
import tutorials.multitenancy.dto.LoginResponse
import tutorials.multitenancy.util.isEqualTo
import tutorials.multitenancy.util.isNotBlank

@AutoConfigureMockMvc
class AuthenticationControllerTest(
  private val mockMvcTester: MockMvcTester,
  private val objectMapper: ObjectMapper,
) : AbstractIntegrationTest() {

  @AfterEach
  fun tearDown() {
    TenantContext.clear()
  }

  @Test
  fun `tenant-a 사용자로 로그인 시 JWT 토큰을 반환해야 한다`() {
    val loginRequest = LoginRequest(username = "tenancy-A", password = "tutorial", tenant = "tenant-a")

    val result = mockMvcTester.post().uri("/auth/login")
      .contentType(MediaType.APPLICATION_JSON)
      .content(objectMapper.writeValueAsString(loginRequest))
      .exchange()

    assertThat(result)
      .hasStatusOk()
      .bodyJson()
      .hasPathSatisfying("$.token", isNotBlank())
      .hasPathSatisfying("$.username", isEqualTo("tenancy-A"))
      .hasPathSatisfying("$.tenant", isEqualTo("tenant-a"))
      .hasPathSatisfying("$.type", isEqualTo("Bearer"))
  }

  @Test
  fun `잘못된 비밀번호로 로그인 시 401 에러를 반환해야 한다`() {
    val loginRequest = LoginRequest(username = "tenancy-A", password = "wrong-password", tenant = "tenant-a")

    val result = mockMvcTester.post().uri("/auth/login")
      .contentType(MediaType.APPLICATION_JSON)
      .content(objectMapper.writeValueAsString(loginRequest))
      .exchange()

    assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED)
  }

  @Test
  fun `존재하지 않는 사용자로 로그인 시 401 에러를 반환해야 한다`() {
    val loginRequest = LoginRequest(username = "tenancy-C", password = "tutorial", tenant = "tenant-a")

    val result = mockMvcTester.post().uri("/auth/login")
      .contentType(MediaType.APPLICATION_JSON)
      .content(objectMapper.writeValueAsString(loginRequest))
      .exchange()

    assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED)
  }

  @Test
  fun `유효한 JWT 토큰으로 인증된 요청 시 사용자 정보를 반환해야 한다`() {
    val loginRequest = LoginRequest(username = "tenancy-A", password = "tutorial", tenant = "tenant-a")
    val loginResult = mockMvcTester.post().uri("/auth/login")
      .contentType(MediaType.APPLICATION_JSON)
      .content(objectMapper.writeValueAsString(loginRequest))
      .exchange()

    val loginResponse = objectMapper.readValue(
      loginResult.response.contentAsString,
      LoginResponse::class.java
    )

    val result = mockMvcTester.get().uri("/auth/me")
      .header("Authorization", "Bearer ${loginResponse.token}")
      .exchange()

    assertThat(result)
      .hasStatusOk()
      .bodyJson()
      .hasPathSatisfying("$.username", isEqualTo("tenancy-A"))
      .hasPathSatisfying("$.tenant", isEqualTo("tenant-a"))
  }

  @Test
  fun `유효하지 않은 테넌트로 로그인 시 401 에러를 반환해야 한다`() {
    val loginRequest = LoginRequest(username = "tenancy-A", password = "tutorial", tenant = "tenant-c")

    val result = mockMvcTester.post().uri("/auth/login")
      .contentType(MediaType.APPLICATION_JSON)
      .content(objectMapper.writeValueAsString(loginRequest))
      .exchange()

    assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED)
  }

  @Test
  fun `유효하지 않은 JWT 토큰으로 인증 요청 시 401 에러를 반환해야 한다`() {
    val result = mockMvcTester.get().uri("/auth/me")
      .header("Authorization", "Bearer invalid.jwt.token")
      .exchange()

    assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED)
  }

  @Test
  fun `토큰 없이 인증 필요 API 접근 시 401 에러를 반환해야 한다`() {
    val result = mockMvcTester.get().uri("/auth/me")
      .exchange()

    assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED)
  }
}
