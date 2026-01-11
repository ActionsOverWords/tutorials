package tutorials.proxysql.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "notices")
class Notice(
  @Id
  @UuidGenerator
  val id: UUID? = null,

  @Column(nullable = false, length = 200)
  var title: String,

  @Column(nullable = false, length = 10000)
  var content: String,

  @Column(nullable = false)
  val createdAt: Instant = Instant.now(),

  var publishFrom: Instant? = null,

  var publishTo: Instant? = null,

  @Column(nullable = false)
  var enabled: Boolean = true,
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Notice) return false
    return id != null && id == other.id
  }

  override fun hashCode(): Int = id?.hashCode() ?: 0

  override fun toString(): String {
    return "Notice(id=$id, title='$title', createdAt=$createdAt, enabled=$enabled)"
  }
}
