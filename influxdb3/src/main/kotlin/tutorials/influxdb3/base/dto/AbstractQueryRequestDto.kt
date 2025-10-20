package tutorials.influxdb3.base.dto

import tutorials.influxdb3.base.enums.Order

abstract class AbstractQueryRequestDto (
  open val page: Int? = 1,
  open val pageSize: Int? = 20,
  open val order: Order? = Order.DESC,
) {
  abstract fun toWhereClause(): String
}
