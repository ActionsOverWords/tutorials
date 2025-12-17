package tutorials.javers.config

import org.hibernate.type.Type
import org.javers.core.CommitIdGenerator
import org.javers.core.JaversBuilderPlugin
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import tutorials.javers.base.extentions.logger

@TestConfiguration(proxyBeanMethods = false)
class JaversTestConfig {

  private val log by logger()

  @Bean
  fun commitIdGenerator(): JaversBuilderPlugin {
    return JaversBuilderPlugin { builder ->
      log.info("commitIdGenerator : Sequence..")
      builder.withCommitIdGenerator(CommitIdGenerator.SYNCHRONIZED_SEQUENCE)
    }
  }

  @Bean
  fun emptyHibernatePropertiesCustomizer(): HibernatePropertiesCustomizer {
    return HibernatePropertiesCustomizer { _ ->
      log.info("Empty HibernatePropertiesCustomizer..")
    }
  }

  @Bean
  fun javersInterceptor(): JaversInterceptor {
    return object : JaversInterceptor {
      override fun postFlush(entities: MutableIterator<Any>) {
        log.debug("=== postFlush called ===")
      }

      override fun onRemove(
        entity: Any,
        id: Any,
        state: Array<Any>,
        propertyNames: Array<String>,
        types: Array<Type>,
      ) {
        log.info("onRemove called: ${entity::class.simpleName}")
      }
    }
  }

}
