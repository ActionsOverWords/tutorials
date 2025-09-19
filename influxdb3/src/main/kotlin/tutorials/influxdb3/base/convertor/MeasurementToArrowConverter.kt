package tutorials.influxdb3.base.convertor

import org.apache.arrow.memory.BufferAllocator
import org.apache.arrow.vector.BigIntVector
import org.apache.arrow.vector.Float8Vector
import org.apache.arrow.vector.IntVector
import org.apache.arrow.vector.TimeStampMicroTZVector
import org.apache.arrow.vector.VarCharVector
import org.apache.arrow.vector.VectorSchemaRoot
import org.apache.arrow.vector.types.pojo.Field
import org.apache.arrow.vector.types.pojo.Schema
import org.apache.arrow.vector.util.Text
import java.time.Instant
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

object MeasurementToArrowConverter {

  fun <T : Any> toVectorSchemaRoot(
    data: List<T>,
    kClass: KClass<T>,
    allocator: BufferAllocator,
  ): VectorSchemaRoot {
    if (data.isEmpty()) {
      throw IllegalArgumentException("Cannot convert an empty list.")
    }

    val fields = kClass.memberProperties.map { prop ->
      when (prop.returnType.classifier) {
        String::class -> Field.nullable(prop.name, org.apache.arrow.vector.types.Types.MinorType.VARCHAR.type)
        Double::class -> Field.nullable(prop.name, org.apache.arrow.vector.types.Types.MinorType.FLOAT8.type)
        Long::class -> Field.nullable(prop.name, org.apache.arrow.vector.types.Types.MinorType.BIGINT.type)
        Int::class -> Field.nullable(prop.name, org.apache.arrow.vector.types.Types.MinorType.INT.type)
        Instant::class -> Field.notNullable(
          prop.name,
          org.apache.arrow.vector.types.Types.MinorType.TIMESTAMPMICROTZ.type
        )

        else -> throw IllegalArgumentException("Unsupported type: ${prop.returnType}")
      }
    }

    val schema = Schema(fields)
    val root = VectorSchemaRoot.create(schema, allocator)
    root.allocateNew()

    data.forEachIndexed { rowIndex, item ->
      kClass.memberProperties.forEach { prop ->
        val vector = root.getVector(prop.name)
        val value = prop.get(item)

        when (vector) {
          is VarCharVector -> vector.setSafe(rowIndex, value?.let { Text(it as String) })
          is Float8Vector -> vector.setSafe(rowIndex, value as Double)
          is BigIntVector -> vector.setSafe(rowIndex, value as Long)
          is IntVector -> vector.setSafe(rowIndex, value as Int)
          is TimeStampMicroTZVector -> {
            val instantValue = value as Instant
            val micros = instantValue.epochSecond * 1_000_000 + instantValue.nano / 1_000
            vector.setSafe(rowIndex, micros)
          }
        }
      }
    }

    root.rowCount = data.size
    return root
  }

}
