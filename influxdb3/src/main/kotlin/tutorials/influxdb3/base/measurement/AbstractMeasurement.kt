package tutorials.influxdb3.base.measurement

import java.time.Instant

abstract class AbstractMeasurement(
  val measurement: String,
  time: Instant = Instant.now(),
) {

  private val tags = mutableMapOf<String, String>()
  private val fields = mutableMapOf<String, Any>()
  private var timestamp: Long = time.epochSecond * 1_000_000_000 + time.nano

  fun addTag(key: String, value: String): AbstractMeasurement {
    tags[key] = value
    return this
  }

  protected fun addField(key: String, value: Any): AbstractMeasurement {
    fields[key] = value
    return this
  }

  fun toLineProtocol(): String {
    if (fields.isEmpty())
      throw IllegalStateException("Table(Measurement) must have at least one field.")

    val tagsPart = if (tags.isEmpty()) "" else "," + tags.entries
      .joinToString(",") { "${escape(it.key)}=${escape(it.value)}" }

    val fieldsPart = fields.entries
      .joinToString(",") { "${escape(it.key)}=${formatFieldValue(it.value)}" }

    val timestampPart = timestamp.let { " $it" }

    return "$measurement$tagsPart $fieldsPart$timestampPart"
  }

  private fun formatFieldValue(value: Any): String {
    return when (value) {
      is String -> "\"${escape(value, isString = true)}\""
      is Long -> "${value}i"
      else -> value.toString()
    }
  }

  private fun escape(value: String, isString: Boolean = false): String {
    val result = value.replace(",", "\\,").replace("=", "\\=").replace(" ", "\\ ")
    return if (isString) result.replace("\"", "\\\"") else result
  }

}
