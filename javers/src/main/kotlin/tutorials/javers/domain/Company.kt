package tutorials.javers.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import tutorials.javers.base.AbstractTraceEntity
import tutorials.javers.base.ColumnSize

@Entity
@Table(name = "company")
class Company private constructor(
  @Column(nullable = false)
  var name: String,

  @Column(nullable = false)
  var ceoName: String,

  @Column(nullable = false)
  var enabled: Boolean = true,
) : AbstractTraceEntity() {
  @Id
  @UuidGenerator
  @Column(length = ColumnSize.UUID)
  val id: String? = null

  @OneToOne(mappedBy = "company", cascade = [CascadeType.ALL], orphanRemoval = true, optional = false)
  @PrimaryKeyJoinColumn
  lateinit var companyOption: CompanyOption

  @OneToMany(mappedBy = "company", cascade = [CascadeType.ALL], orphanRemoval = true)
  val users: MutableList<User> = mutableListOf()

  fun addUser(user: User) {
    users.add(user)
    user.company = this
  }

  fun removeUser(user: User) {
    users.remove(user)
    user.company = null
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Company

    return id == other.id
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }

  override fun toString(): String {
    return """
      Company(
        id=$id, name=$name, ceoName=$ceoName, enabled=$enabled,
        companyOption=$companyOption, employees=${users.size}
      )"
    """.trimIndent()
  }

  companion object {
    fun of(
      name: String,
      ceoName: String,
      enabled: Boolean = true,
    ): Company {
      val company = Company(name = name, ceoName = ceoName, enabled = enabled)

      val companyOption = CompanyOption(company = company)
      company.companyOption = companyOption

      return company
    }
  }

}
