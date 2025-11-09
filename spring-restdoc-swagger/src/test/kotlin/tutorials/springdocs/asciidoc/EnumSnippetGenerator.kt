package tutorials.springdocs.asciidoc

import org.junit.jupiter.api.Test
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.PayloadDocumentation.beneathPath
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadSubsectionExtractor
import org.springframework.restdocs.snippet.Attributes.attributes
import org.springframework.restdocs.snippet.Attributes.key
import org.springframework.restdocs.snippet.Snippet
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tutorials.springdocs.asciidoc.enums.EnumAdocGenerator
import tutorials.springdocs.asciidoc.enums.EnumMetadata
import tutorials.springdocs.asciidoc.enums.EnumMetadataReader
import tutorials.springdocs.config.AbstractRestDocsTest

class EnumSnippetGenerator: AbstractRestDocsTest() {

  private val enumMetadataReader = EnumMetadataReader()
  private val enumAdocGenerator = EnumAdocGenerator()

  @Test
  fun generateEnumSnippets() {
    val enumMap = enumMetadataReader.getEnumsMetadataMap()
    enumAdocGenerator.generateEnumAdoc(enumMap.keys)

    mockMvc.perform(
      get("/api/v1/enums")
    )
      .andDo(print())
      .andExpect(status().isOk)
      .andDo(
        document(
          "enums",
          *generateEnumSnippets(enumMap),
        ),
      )
  }

  private fun generateEnumSnippets(enums: Map<String, Set<EnumMetadata>>): Array<Snippet> {
    return enums.keys
      .map { key ->
        customResponseFields(
          beneathPath(key).withSubsectionId(key),
          attributes(key("title").value(key)),
          *enumConvertFieldDescriptor(enums[key]!!)
        )
      }
      .toTypedArray()
  }

  private fun enumConvertFieldDescriptor(enums: Set<EnumMetadata>): Array<FieldDescriptor> {
    return enums.map {
      fieldWithPath(it.name).description(it.description)
    }.toTypedArray<FieldDescriptor>()
  }

  private fun customResponseFields(
    subsectionExtractor: PayloadSubsectionExtractor<*>?,
    attributes: Map<String?, Any?>?,
    vararg descriptors: FieldDescriptor?
  ): CustomResponseFieldsSnippet {
    return CustomResponseFieldsSnippet(
      type = "enum-response",
      subsectionExtractor = subsectionExtractor,
      descriptors = descriptors.toList(),
      attributes = attributes,
      ignoreUndocumentedFields = true,
    )
  }

}
