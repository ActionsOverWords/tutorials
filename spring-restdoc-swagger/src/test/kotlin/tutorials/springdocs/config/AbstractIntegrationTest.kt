package tutorials.springdocs.config

import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.snippet.Snippet
import org.springframework.test.context.TestConstructor
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
abstract class AbstractIntegrationTest {
  @Autowired
  lateinit var objectMapper: ObjectMapper
}

@ExtendWith(RestDocumentationExtension::class)
abstract class AbstractRestDocsTest : AbstractIntegrationTest() {
  lateinit var mockMvc: MockMvc

  @BeforeEach
  fun setUp(webApplicationContext: WebApplicationContext, provider: RestDocumentationContextProvider) {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
      .apply<DefaultMockMvcBuilder>(
        documentationConfiguration(provider)
          .operationPreprocessors()
      )
      .build()
  }

  fun MockHttpServletRequestBuilder.jsonContent(value: Any): MockHttpServletRequestBuilder {
    contentType(MediaType.APPLICATION_JSON)
    content(objectMapper.writeValueAsString(value))
    return this
  }

  fun ResultActions.andDocument(
    identifier: String,
    vararg snippets: Snippet,
  ): ResultActions {
    return andDo(
      document(
        identifier,
        preprocessRequest(prettyPrint()),
        preprocessResponse(prettyPrint()),
        *snippets
      )
    )
  }

}
