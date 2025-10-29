package tutorials.jpa.config

import org.junit.jupiter.api.Test
import tutorials.jpa.base.extentions.logger

class EncryptedStringConverterTest {

  val converter = EncryptedStringConverter("tutorials")

  @Test
  fun enc() {
    val convertToDatabaseColumn = converter.convertToDatabaseColumn("test")
    logger().warn("convertToDatabaseColumn: {}", convertToDatabaseColumn)
  }

  @Test
  fun dec() {
    val convertToDatabaseColumn = converter.convertToDatabaseColumn("test")
    val convertToEntityAttribute = converter.convertToEntityAttribute(convertToDatabaseColumn)
    logger().warn("convertToEntityAttribute: {}", convertToEntityAttribute)
  }

}
