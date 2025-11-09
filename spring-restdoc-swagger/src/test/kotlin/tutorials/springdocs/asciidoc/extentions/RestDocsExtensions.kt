package tutorials.springdocs.asciidoc.extentions

import org.springframework.restdocs.snippet.Attributes
import tutorials.springdocs.asciidoc.enums.ENUM_DOCUMENT_ID

import kotlin.reflect.KClass

fun getDateFormat(): Attributes.Attribute {
  return Attributes.Attribute("format", "yyyy-MM-dd")
}

fun enumPopupLink(linkText: String, enumClass: KClass<*>): String {
  return "link:$ENUM_DOCUMENT_ID/${enumClass.simpleName}.html[${linkText},role=\"popup\"]"
}
