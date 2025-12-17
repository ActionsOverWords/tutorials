package tutorials.javers.domain.javers

import jakarta.persistence.*
import java.math.BigDecimal

/**
 * JaVers Snapshot 테이블 Entity
 */
@Entity
@Table(
  name = "jv_snapshot",
  indexes = [
    Index(name = "jv_snapshot_global_id_fk_idx", columnList = "global_id_fk"),
    Index(name = "jv_snapshot_commit_fk_idx", columnList = "commit_fk")
  ]
)
class JvSnapshot(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "snapshot_pk", columnDefinition = "BIGINT")
  val snapshotPk: Long? = null,

  @Column(name = "type", length = 200)
  val type: String? = null,

  @Column(name = "version")
  val version: Long? = null,

  @Lob
  @Column(name = "state", columnDefinition = "TEXT")
  val state: String? = null,

  @Lob
  @Column(name = "changed_properties", columnDefinition = "TEXT")
  val changedProperties: String? = null,

  @Column(name = "managed_type", length = 200)
  val managedType: String? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "global_id_fk", foreignKey = ForeignKey(name = "jv_snapshot_global_id_fk"))
  val globalId: JvGlobalId? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "commit_fk", foreignKey = ForeignKey(name = "jv_snapshot_commit_fk"))
  val commit: JvCommit? = null
)
