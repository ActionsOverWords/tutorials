package tutorials.multitenancy.config

import com.zaxxer.hikari.HikariDataSource
import jakarta.persistence.EntityManagerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import tutorials.multitenancy.base.extensions.logger
import java.util.*
import javax.sql.DataSource

@Configuration
@EnableTransactionManagement
@EnableConfigurationProperties(TenantDataSourceProperties::class)
class DataSourceConfig(
  private val tenantProperties: TenantDataSourceProperties
) {

  private val logger by logger()

  @Bean
  fun tenantDataSources(): Map<String, DataSource> {
    return tenantProperties.tenants.mapValues { (tenantId, config) ->
      logger.info("Creating DataSource for tenant: $tenantId")

      DataSourceBuilder.create()
        .type(HikariDataSource::class.java)
        .url(config.url)
        .username(config.username)
        .password(config.password)
        .driverClassName(config.driverClassName)
        .build()
    }
  }

  @Bean
  @Primary
  fun routingDataSource(tenantDataSources: Map<String, DataSource>): DataSource {
    require(tenantDataSources.isNotEmpty()) {
      "At least one tenant DataSource must be configured"
    }

    val routingDataSource = object : AbstractRoutingDataSource() {
      override fun determineCurrentLookupKey(): Any? {
        val tenant = TenantContext.getTenant()
          ?: throw IllegalStateException("Tenant context not set")

        if (!tenantDataSources.containsKey(tenant)) {
          throw IllegalStateException("Unknown tenant: $tenant")
        }

        logger.debug("Routing to datasource: $tenant")
        return tenant
      }
    }

    routingDataSource.setTargetDataSources(HashMap<Any, Any>(tenantDataSources))
    routingDataSource.setLenientFallback(false)
    routingDataSource.afterPropertiesSet()

    return routingDataSource
  }

  @Bean
  fun entityManagerFactory(routingDataSource: DataSource): LocalContainerEntityManagerFactoryBean {
    val em = LocalContainerEntityManagerFactoryBean()
    em.dataSource = routingDataSource
    em.setPackagesToScan("tutorials.multitenancy.domain")
    em.jpaVendorAdapter = HibernateJpaVendorAdapter()

    val properties = Properties()
    properties.setProperty("hibernate.hbm2ddl.auto", "none")
    properties.setProperty("hibernate.dialect", "org.hibernate.dialect.MariaDBDialect")
    properties.setProperty("hibernate.format_sql", "true")
    properties.setProperty("hibernate.boot.allow_jdbc_metadata_access", "false")
    properties.setProperty("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy")
    em.setJpaProperties(properties)

    return em
  }

  @Bean
  fun transactionManager(entityManagerFactory: EntityManagerFactory): PlatformTransactionManager {
    return JpaTransactionManager(entityManagerFactory)
  }
}
