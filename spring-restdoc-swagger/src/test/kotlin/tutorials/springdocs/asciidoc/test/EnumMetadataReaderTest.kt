package tutorials.springdocs.asciidoc.test

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import tutorials.springdocs.asciidoc.enums.EnumMetadataReader
import tutorials.springdocs.base.extentions.logger

class EnumMetadataReaderTest {

  @Test
  fun getEnumsMetadataMap() {
    val reader = EnumMetadataReader()
    val enums = reader.getEnumsMetadataMap()
    logger().info("enums: {}", enums)

    Assertions.assertTrue(enums.isNotEmpty())
  }

}
