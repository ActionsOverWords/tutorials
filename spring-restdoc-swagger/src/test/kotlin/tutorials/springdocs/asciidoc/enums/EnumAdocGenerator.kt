package tutorials.springdocs.asciidoc.enums

import tutorials.springdocs.base.extentions.logger
import java.io.File
import java.io.FileOutputStream

class EnumAdocGenerator {
  private val log = logger()

  fun generateEnumAdoc(enumClassNames: Set<String>) {
    // 파일이 생성될 디렉토리 삭제
    val enumAdocDirectory = File(ENUM_ADOC_PATH)
    if (enumAdocDirectory.exists()) {
      enumAdocDirectory.deleteRecursively()
    }

    // 파일이 생성될 디렉토리 생성
    if (!enumAdocDirectory.exists()) {
      val isCreated = enumAdocDirectory.mkdirs()
      if (!isCreated) {
        log.error("디렉토리 생성에 실패하였습니다.")
      }
    }

    for (enumClassName in enumClassNames) {
      val sb = StringBuilder()

      // 문서 상단 내용 구성
      sb.append("ifndef::snippets[]").append(System.lineSeparator())
        .append(":snippets: ").append(DEFAULT_SNIPPET_DIR).append(System.lineSeparator())
        .append("endif::[]").append(System.lineSeparator())
        .append("= ").append(enumClassName).append(System.lineSeparator())
        .append(":doctype: book").append(System.lineSeparator())
        .append(System.lineSeparator())

      // 문서 본문 내용 구성
      sb.append("include::{snippets}/${ENUM_DOCUMENT_ID}/${ENUM_SNIPPET_NAME}-")
        .append(enumClassName)
        .append(".adoc[]").append(System.lineSeparator())

      val enumAdoc = File("${ENUM_ADOC_PATH}/${enumClassName}.adoc")

      FileOutputStream(enumAdoc).use { os ->
        os.write(sb.toString().toByteArray(charset("UTF-8")))
      }
    }
  }
}
