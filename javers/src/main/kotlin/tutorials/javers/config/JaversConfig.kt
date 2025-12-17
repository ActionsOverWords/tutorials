package tutorials.javers.config

import org.javers.core.JaversBuilderPlugin
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tutorials.javers.base.extentions.logger
import tutorials.javers.domain.javers.interceptor.JaversInterceptorImpl
import kotlin.getValue

@Configuration
class JaversConfig {

  private val log by logger()

  @Bean
  fun customCommitIdGenerator(): JaversBuilderPlugin {
    return JaversBuilderPlugin { builder ->
      log.info("commitIdGenerator : Custom..")
      builder.withCustomCommitIdGenerator(CustomCommitIdGenerator())
    }
  }

  @Bean
  fun hibernatePropertiesCustomizer(javersInterceptor: JaversInterceptor): HibernatePropertiesCustomizer {
    return HibernatePropertiesCustomizer { hibernateProperties ->
      log.info("HibernatePropertiesCustomizer..")
      hibernateProperties["hibernate.session_factory.interceptor"] = javersInterceptor
    }
  }

}
