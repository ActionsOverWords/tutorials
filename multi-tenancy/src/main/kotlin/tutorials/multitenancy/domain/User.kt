package tutorials.multitenancy.domain

import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import java.time.Instant

@Entity
@Table(name = "users")
class User(
  @Id
  @UuidGenerator
  @Column(length = 36)
  val id: String? = null,

  @Column(nullable = false, unique = true, length = 50)
  val username: String,

  @Column(nullable = false, length = 100)
  var password: String,

  @Column(nullable = false, length = 50)
  var tenantId: String,

  @Column(nullable = false)
  val createdAt: Instant = Instant.now(),

  @Column(nullable = false)
  var enabled: Boolean = true
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is User) return false
    return id != null && id == other.id
  }

  override fun hashCode(): Int = id?.hashCode() ?: 0

  override fun toString(): String {
    return "User(id=$id, username='$username', tenantId='$tenantId', enabled=$enabled)"
  }
}
