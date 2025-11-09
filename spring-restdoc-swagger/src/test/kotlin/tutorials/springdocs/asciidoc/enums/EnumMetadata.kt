package tutorials.springdocs.asciidoc.enums

const val ENUM_ADOC_PATH = "src/docs/asciidoc/enums"
const val DEFAULT_SNIPPET_DIR = "build/generated-snippets"
const val ENUM_DOCUMENT_ID = "enums"
const val ENUM_SNIPPET_NAME = "enum-response-fields"
const val ENUM_BASE_PACKAGE = "tutorials.springdocs"

data class EnumMetadata(
  val name: String,
  val description: String,
)
