package tutorials.multitenancy.config

import org.flywaydb.core.Flyway
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tutorials.multitenancy.base.extensions.logger
import javax.sql.DataSource

@Configuration
class FlywayConfig {

  private val logger by logger()

  @Bean
  fun flywayMigrationStrategy(): FlywayMigrationStrategy {
    return FlywayMigrationStrategy { }
  }

  @Bean
  fun flywayMigration(tenantDataSources: Map<String, DataSource>): String {
    tenantDataSources.forEach { (tenantId, dataSource) ->
      migrateTenant(tenantId, dataSource)
    }

    return "Flyway migrations completed for all tenants"
  }

  private fun migrateTenant(tenantId: String, dataSource: DataSource) {
    logger.info("Running Flyway migration for $tenantId")

    Flyway.configure()
      .dataSource(dataSource)
      .locations("classpath:db/migration")
      .baselineOnMigrate(true)
      .load()
      .migrate()

    logger.info("Completed Flyway migration for $tenantId")
  }
}
