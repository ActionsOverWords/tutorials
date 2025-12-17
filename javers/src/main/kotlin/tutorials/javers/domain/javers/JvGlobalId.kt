package tutorials.javers.domain.javers

import jakarta.persistence.*
import java.math.BigDecimal

/**
 * JaVers Global ID 테이블 Entity
 */
@Entity
@Table(
  name = "jv_global_id",
  indexes = [
    Index(name = "jv_global_id_local_id_idx", columnList = "local_id"),
    Index(name = "jv_global_id_owner_id_fk_idx", columnList = "owner_id_fk")
  ]
)
class JvGlobalId(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "global_id_pk", columnDefinition = "BIGINT")
  val globalIdPk: Long? = null,

  @Column(name = "local_id", length = 191)
  val localId: String? = null,

  @Column(name = "fragment", length = 200)
  val fragment: String? = null,

  @Column(name = "type_name", length = 200)
  val typeName: String? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id_fk", foreignKey = ForeignKey(name = "jv_global_id_owner_id_fk"))
  val owner: JvGlobalId? = null
)
