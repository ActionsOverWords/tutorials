package tutorials.jpa.config

import org.hibernate.resource.jdbc.spi.StatementInspector
import tutorials.jpa.base.constants.ColumnEncryptionConstants.INSPECTOR_SECRET_KEY
import tutorials.jpa.base.extentions.logger

class EncryptionStatementInspector(
  private val secretKey: String,
) : StatementInspector {

  override fun inspect(sql: String): String {
    if (sql.contains(INSPECTOR_SECRET_KEY)) {
      return sql.replace(
        INSPECTOR_SECRET_KEY,
        "'" + escapeSql(secretKey) + "'"
      ).also {
        logger().debug("replaced sql: $it")
      }
    }

    return sql
  }

  private fun escapeSql(value: String): String {
    // SQL injection 방지: 작은따옴표를 두 개로 이스케이프
    return value.replace("'", "''")
  }
}
