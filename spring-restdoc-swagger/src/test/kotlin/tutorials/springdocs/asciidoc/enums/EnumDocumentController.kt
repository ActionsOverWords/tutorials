package tutorials.springdocs.asciidoc.enums

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class EnumDocumentController(
) {
  private val enumMetadataReader = EnumMetadataReader()

  @GetMapping("/api/v1/enums")
  fun getEnums(): ResponseEntity<Map<String, Map<String, String>>> {
    val enums = enumMetadataReader.getEnumsMetadataMap()
    val response = createResponse(enums)
    return ResponseEntity.ok(response)
  }

  private fun createResponse(enums: Map<String, Set<EnumMetadata>>): MutableMap<String, Map<String, String>> {
    val responseMap = mutableMapOf<String, Map<String, String>>()
    enums.forEach { (key, value) ->
      val enumMap = mutableMapOf<String, String>()
      value.forEach {
        enumMap[it.name] = it.description
      }
      responseMap[key] = enumMap
    }
    return responseMap
  }

}
