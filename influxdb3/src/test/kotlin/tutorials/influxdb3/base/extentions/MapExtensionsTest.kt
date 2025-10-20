package tutorials.influxdb3.base.extentions

import org.junit.jupiter.api.Test

class MapExtensionsTest {

  val log = logger()

  @Test
  fun toWhereClause() {
    val map = mapOf(
      "a" to 1,
      "b" to "2",
      "c" to true
    )

    val whereClause = map.toWhereClause()
    log.error("{}", whereClause)

    assert(whereClause == " WHERE a = 1 AND b = '2' AND c = true")
  }

  @Test
  fun toWhereClauseByNullValue() {
    val map = mapOf(
      "a" to 1,
      "b" to null,
      "c" to true
    )

    val whereClause = map.toWhereClause()
    log.error("{}", whereClause)

    assert(whereClause == " WHERE a = 1 AND c = true")
  }

  @Test
  fun toWhereClauseByBlankValue() {
    val map = mapOf(
      "a" to 1,
      "b" to " ",
      "c" to true
    )

    val whereClause = map.toWhereClause()
    log.error("{}", whereClause)

    assert(whereClause == " WHERE a = 1 AND c = true")
  }

}
