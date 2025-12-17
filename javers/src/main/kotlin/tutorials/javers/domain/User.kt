package tutorials.javers.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.UuidGenerator
import org.javers.core.metamodel.annotation.DiffIgnore
import tutorials.javers.base.AbstractTraceEntity
import tutorials.javers.base.ColumnSize

@Entity
@Table(name = "user")
class User(
  @Id
  @UuidGenerator
  @Column(length = ColumnSize.UUID)
  val id: String? = null,

  @Column(nullable = false)
  var name: String,

  @Column(nullable = false)
  @DiffIgnore
  var password: String,

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  var role: Role = Role.USER,

  @ColumnDefault("1")
  @Column(nullable = false)
  var enabled: Boolean = true,
) : AbstractTraceEntity() {

  @Column
  var email: String? = null

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn
  var company: Company? = null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as User

    return id == other.id
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }

  override fun toString(): String {
    return """
      User(id=$id, name=$name, email=$email, role=$role)", enabled=$enabled)", company=${company?.id})"
    """.trimIndent()
  }

}
