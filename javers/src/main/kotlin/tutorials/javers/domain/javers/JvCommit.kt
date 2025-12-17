package tutorials.javers.domain.javers

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * JaVers Commit 테이블 Entity
 */
@Entity
@Table(
  name = "jv_commit",
  indexes = [
    Index(name = "jv_commit_commit_id_idx", columnList = "commit_id")
  ]
)
class JvCommit(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "commit_pk", columnDefinition = "BIGINT")
  val commitPk: Long? = null,

  @Column(name = "author", length = 200)
  val author: String? = null,

  @Column(name = "commit_date")
  val commitDate: LocalDateTime? = null,

  @Column(name = "commit_date_instant", length = 30)
  val commitDateInstant: String? = null,

  @Column(name = "commit_id", precision = 22, scale = 2)
  val commitId: BigDecimal? = null
)
