package tutorials.javers.base

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.Date

abstract class AbstractEntity {
  abstract override fun equals(other: Any?): Boolean
  abstract override fun hashCode(): Int
  abstract override fun toString(): String
}

@MappedSuperclass
abstract class AbstractDateEntity : AbstractEntity() {
  @Temporal(TemporalType.TIMESTAMP)
  @Column(nullable = false, updatable = false)
  @CreatedDate
  var createdAt: Date = Date()

  @Column(nullable = false)
  @LastModifiedDate
  var updatedAt: Instant = Instant.now()
}

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class AbstractTraceEntity : AbstractDateEntity() {
  @Column(updatable = false, length = ColumnSize.UUID)
  @CreatedBy
  var createdBy: String? = null

  @Column(length = ColumnSize.UUID)
  @LastModifiedBy
  var updatedBy: String? = null
}
