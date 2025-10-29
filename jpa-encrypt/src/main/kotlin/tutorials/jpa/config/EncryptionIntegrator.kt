package tutorials.jpa.config

import org.hibernate.boot.Metadata
import org.hibernate.boot.spi.BootstrapContext
import org.hibernate.engine.spi.SessionFactoryImplementor
import org.hibernate.integrator.spi.Integrator
import org.hibernate.mapping.Column
import org.hibernate.service.spi.SessionFactoryServiceRegistry
import tutorials.jpa.base.constants.ColumnEncryptionConstants.INSPECTOR_SECRET_KEY

class EncryptionIntegrator(
  val secretKey: String,
) : Integrator {

  override fun integrate(
    metadata: Metadata,
    bootstrapContext: BootstrapContext,
    sessionFactory: SessionFactoryImplementor,
  ) {
    metadata.entityBindings.forEach { entity ->
      entity.referenceableProperties.forEach { property ->
        property.columns.forEach { column ->
          customRead(column)
          customWrite(column)
        }
      }
    }
  }

  private fun customWrite(column: Column) {
    val customWrite = column.customWrite
    if (customWrite != null && customWrite.contains(INSPECTOR_SECRET_KEY)) {
      column.customWrite = customWrite.replace(
        INSPECTOR_SECRET_KEY,
        "'" + escapeSql(secretKey) + "'"
      )
    }
  }

  private fun customRead(column: Column) {
    val customRead = column.customRead
    if (customRead != null && customRead.contains(INSPECTOR_SECRET_KEY)) {
      column.customRead = customRead.replace(
        INSPECTOR_SECRET_KEY,
        "'" + escapeSql(secretKey) + "'"
      )
    }
  }

  private fun escapeSql(value: String): String {
    // SQL injection 방지: 작은따옴표를 두 개로 이스케이프
    return value.replace("'", "''")
  }

  override fun disintegrate(
    sessionFactory: SessionFactoryImplementor,
    serviceRegistry: SessionFactoryServiceRegistry,
  ) {
    // no-op
  }

}
