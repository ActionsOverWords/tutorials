package tutorials.springdocs.controller

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put
import org.springframework.restdocs.payload.JsonFieldType.NUMBER
import org.springframework.restdocs.payload.JsonFieldType.STRING
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tutorials.springdocs.asciidoc.extentions.enumPopupLink
import tutorials.springdocs.asciidoc.extentions.getDateFormat
import tutorials.springdocs.config.AbstractRestDocsTest
import tutorials.springdocs.domain.Gender
import tutorials.springdocs.dto.UserRegisterRequest
import tutorials.springdocs.dto.UserUpdateRequest
import tutorials.springdocs.service.UserService

class UserControllerTest(
  val userService: UserService,
) : AbstractRestDocsTest() {

  @Test
  fun register() {
    val request = UserRegisterRequest("test", "password")

    mockMvc.perform(
      post("/users")
        .jsonContent(request)
    )
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.id").isNotEmpty)
      .andDocument(
        "users/register",
        requestFields(
          fieldWithPath("username").type(STRING).description("User ID"),
          fieldWithPath("password").type(STRING).description("Password"),
        ),
        responseFields(
          fieldWithPath("id").type(STRING).description("User ID"),
          fieldWithPath("username").type(STRING).description("Username")
        ),
      )
  }

  @Test
  fun find() {
    val request = UserRegisterRequest("test", "password")
    val user = userService.register(request)

    mockMvc.perform(
      get("/users/{id}", user.id)
    )
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.id").isNotEmpty)
      .andDocument(
        "users/detail",
        pathParameters(
          parameterWithName("id").description("User ID"),
        ),
        responseFields(
          fieldWithPath("id").type(STRING).description("User ID"),
          fieldWithPath("username").type(STRING).description("Username"),
          fieldWithPath("nickname").type(STRING).description("Nickname").optional(),
          fieldWithPath("birthDate").type(NUMBER).description("Birth Date").optional(),
          fieldWithPath("gender").type(STRING).description(enumPopupLink("Gender", Gender::class)).optional(),
        ),
      )
  }

  @Test
  fun update() {
    val request = UserUpdateRequest("nickname", "2020-11-01", Gender.MALE)
    val requestJson = objectMapper.writeValueAsString(request)

    val user = userService.register(UserRegisterRequest("test", "password"))

    mockMvc.perform(
      put("/users/{id}", user.id)
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestJson)
    )
      .andExpect(status().isOk)
      .andDocument(
        "users/update",
        pathParameters(
          parameterWithName("id").description("User ID"),
        ),
        requestFields(
          fieldWithPath("nickname").type(STRING).description("Nickname").optional(),
          fieldWithPath("birthDate").type(STRING).attributes(getDateFormat()).description("Birth Date").optional(),
          fieldWithPath("gender").type(STRING).description(enumPopupLink("Gender", Gender::class)).optional(),
        ),
      )
  }

}
