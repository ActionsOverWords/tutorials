package tutorials.javers.domain.javers.interceptor

import org.hibernate.CallbackException
import org.hibernate.Interceptor
import org.hibernate.type.Type
import org.javers.core.Javers
import org.springframework.context.annotation.Fallback
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import tutorials.javers.base.AbstractEntity
import tutorials.javers.base.extentions.logger
import tutorials.javers.config.JaversInterceptor

@Component
@Fallback
class JaversInterceptorImpl(
  @param:Lazy val javers: Javers,
) : JaversInterceptor, Interceptor {

  private val log by logger()

  @Throws(CallbackException::class)
  override fun postFlush(entities: MutableIterator<Any>) {
    val entityList = entities.asSequence()
      .filter { it is AbstractEntity }
      .map { it as AbstractEntity }
      .toList()

    if (entityList.isEmpty()) {
      return
    }

    log.debug("=== postFlush called with {} entity ===", entityList.size)

    entityList.forEach { entity ->
      log.debug("Committing entity: [{}]", entity::class.simpleName)
      javers.commit("system", entity)
    }
  }

  @Throws(CallbackException::class)
  override fun onRemove(
    entity: Any,
    id: Any,
    state: Array<Any>,
    propertyNames: Array<String>,
    types: Array<Type>,
  ) {
    if (entity is AbstractEntity) {
      log.debug("[DELETE] Deleting entity: {}", entity::class.simpleName)
      javers.commitShallowDelete("system", entity)
    }
  }

}
