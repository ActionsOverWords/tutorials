package tutorials.javers.config

import org.hibernate.Interceptor
import org.hibernate.type.Type

interface JaversInterceptor: Interceptor {

  override fun postFlush(entities: MutableIterator<Any>)

  override fun onRemove(
    entity: Any,
    id: Any,
    state: Array<Any>,
    propertyNames: Array<String>,
    types: Array<Type>,
  )

}
