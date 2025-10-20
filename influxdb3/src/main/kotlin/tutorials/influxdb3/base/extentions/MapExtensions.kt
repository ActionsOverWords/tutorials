package tutorials.influxdb3.base.extentions

fun <K, V> Map<K, V>.toWhereClause(): String {
  if (this.isEmpty()) {
    return ""
  }

  val clause = this.entries
    .filter { (_, value) ->
      value != null && (value !is String || value.isNotBlank())
    }
    .joinToString(" AND ") { (key, value) ->
      when (value) {
        is Number, is Boolean -> "$key = $value"
        is String -> "$key = '$value'"
        else -> "$key = '$value'"
      }
    }

  return if (clause.isEmpty()) {
    ""
  } else {
    " WHERE $clause"
  }
}
