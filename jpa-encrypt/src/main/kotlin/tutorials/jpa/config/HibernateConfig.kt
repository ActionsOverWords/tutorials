package tutorials.jpa.config

import org.hibernate.cfg.AvailableSettings
import org.hibernate.jpa.boot.spi.IntegratorProvider
import org.hibernate.jpa.boot.spi.JpaSettings
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tutorials.jpa.base.extentions.logger

@Configuration
class HibernateConfig(
  private val kmsProperties: KmsProperties,
) {

  @Bean
  fun hibernatePropertiesCustomizer(): HibernatePropertiesCustomizer {
    return HibernatePropertiesCustomizer { hibernateProperties: MutableMap<String, Any> ->
      val secretKey = kmsProperties.db.secretKey

      val inspector = EncryptionStatementInspector(secretKey)
      val integrator = EncryptionIntegrator(secretKey)

      /*
        * Inspector 또는 Integrator 방식 중 하나만 사용
       */
      hibernateProperties[AvailableSettings.STATEMENT_INSPECTOR] = inspector
      hibernateProperties[JpaSettings.INTEGRATOR_PROVIDER] = IntegratorProvider { listOf(integrator) }

      logger().info("Registered EncryptionStatementInspector ans EncryptionIntegrator..")
    }
  }

}

