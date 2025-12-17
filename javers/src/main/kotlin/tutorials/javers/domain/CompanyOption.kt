package tutorials.javers.domain

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import tutorials.javers.base.AbstractTraceEntity
import tutorials.javers.base.ColumnSize

@Entity
@Table(name = "company_option")
class CompanyOption internal constructor(
  @OneToOne(optional = false)
  @MapsId
  val company: Company,
) : AbstractTraceEntity() {
  @Id
  @Column(length = ColumnSize.UUID)
  val id: String? = null

  @Column(nullable = false)
  var timeZone: String = "Asia/Seoul"

  @Embedded
  val securityOption: SecurityOption = SecurityOption()

  @ElementCollection
  @CollectionTable(name = "company_allowed_ip", joinColumns = [JoinColumn(name = "company_id")])
  @Column(name = "ip_address")
  var allowedIpAddresses: MutableList<String> = mutableListOf()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as CompanyOption

    return id == other.id
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }

  override fun toString(): String {
    return """
      CompanyOption(
        id=$id, timeZone=$timeZone, securityOption=$securityOption, allowedIpAddresses=$allowedIpAddresses
      )"
    """.trimIndent()
  }

}
