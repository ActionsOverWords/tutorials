package tutorials.javers.domain.javers

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.io.Serializable

/**
 * JaVers Commit Property 테이블 Entity (복합키)
 */
@Entity
@Table(
  name = "jv_commit_property",
  indexes = [
    Index(name = "jv_commit_property_commit_fk", columnList = "commit_fk"),
  ]
)
@IdClass(JvCommitPropertyId::class)
class JvCommitProperty(
  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "commit_fk", foreignKey = ForeignKey(name = "jv_commit_property_commit_fk"))
  val commit: JvCommit,

  @Id
  @Column(name = "property_name", length = 191)
  val propertyName: String,

  @Column(name = "property_value", length = 600)
  val propertyValue: String? = null,
)

/**
 * JvCommitProperty 복합키 클래스
 */
data class JvCommitPropertyId(
  val commit: Long = 0L,
  val propertyName: String = "",
) : Serializable
