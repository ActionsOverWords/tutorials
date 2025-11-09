package tutorials.springdocs.dto

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tutorials.springdocs.base.extentions.logger

class UserDtoTest {

  @Nested
  inner class UserRegisterResponseTest {
    val objectMapper = jacksonObjectMapper()

    @Test
    fun deserialisation() {
      val json = """
      {"id":"tutorials-id","username":"test"}
    """.trimIndent()

      val response = objectMapper.readValue(json, UserRegisterResponse::class.java)
      assertEquals("tutorials-id", response.id)
      assertEquals("test", response.username)
    }
  }

  @Nested
  inner class UserUpdateRequestTest {
    @Test
    fun toInstantByBirthDate() {
      val request = UserUpdateRequest(birthDate = "2020-11-01")
      val birthDate = request.toInstantByBirthDate()
      logger().info("birthDate: {}", birthDate)
    }
  }

}
