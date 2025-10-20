package tutorials.vault.config

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import javax.sql.DataSource

@Configuration
class DataSourceConfig {

  @Bean
  @DependsOn("vaultTemplate")
  fun dataSource(dataSourceProperties: DataSourceProperties): DataSource {
    return dataSourceProperties.initializeDataSourceBuilder().build()
  }
}
