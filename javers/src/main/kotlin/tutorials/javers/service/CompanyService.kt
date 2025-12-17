package tutorials.javers.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import tutorials.javers.domain.Company
import tutorials.javers.domain.User
import tutorials.javers.repository.CompanyRepository
import tutorials.javers.repository.UserRepository

@Service
class CompanyService(
  val companyRepository: CompanyRepository,
  val userRepository: UserRepository,
) {

  @Transactional
  fun createCompany(company: Company): Company {
    return companyRepository.save(company)
  }

  @Transactional
  fun createCompany(name: String, ceoName: String): Company {
    return companyRepository.save(Company.of(name = name, ceoName = ceoName))
  }

  @Transactional
  fun createCompanyAndUser(name: String, ceoName: String, username: String, userPassword: String): Company {
    val company = companyRepository.save(Company.of(name = name, ceoName = ceoName))

    userRepository.save(User(name = username, password = userPassword).apply {
      this.company = company
    })

    return company
  }

  @Transactional
  fun updateCompany(company: Company): Company {
    return companyRepository.save(company)
  }

  @Transactional
  fun updateCompany(id: String, name: String, ceoName: String) {
    val company = findById(id)
    company.name = name
    company.ceoName = ceoName
    companyRepository.save(company)
  }

  @Transactional
  fun deleteCompany(company: Company) {
    companyRepository.delete(company)
  }

  @Transactional
  fun deleteCompany(id: String) {
    companyRepository.deleteById(id)
  }

  fun findById(id: String): Company {
    return companyRepository.findById(id).orElseThrow {
      IllegalArgumentException("Company not found: $id")
    }
  }

}
