package tutorials.javers.service

import jakarta.transaction.Transactional
import org.javers.spring.annotation.JaversAuditable
import org.javers.spring.annotation.JaversAuditableDelete
import org.springframework.stereotype.Service
import tutorials.javers.domain.Company
import tutorials.javers.repository.CompanyJaversRepository
import tutorials.javers.repository.CompanyRepository

@Service
class CompanyJaversService(
  val companyRepository: CompanyRepository,
  val companyJaversRepository: CompanyJaversRepository,
) {
  
  @Transactional
  @JaversAuditable
  fun createCompany(company: Company): Company {
    return companyRepository.save(company)
  }

  @Transactional
  @JaversAuditable
  @Deprecated("JaVers 지원 안함")
  fun createCompany(name: String, ceoName: String): Company {
    return companyRepository.save(Company.of(name = name, ceoName = ceoName))
  }

  @Transactional
  @JaversAuditable
  @Deprecated("JaVers 지원 안함")
  fun createCompanyByJavers(name: String, ceoName: String): Company {
    return companyJaversRepository.save(Company.of(name = name, ceoName = ceoName))
  }

  @Transactional
  @JaversAuditable
  fun updateCompany(company: Company): Company {
    return companyRepository.save(company)
  }

  @Transactional
  @JaversAuditable
  @Deprecated("JaVers 지원 안함")
  fun updateCompany(id: String, name: String, ceoName: String) {
    val company = findById(id)
    company.name = name
    company.ceoName = ceoName
    companyRepository.save(company)
  }

  @Transactional
  @JaversAuditableDelete
  fun deleteCompany(company: Company) {
    companyRepository.delete(company)
  }

  @Transactional
  @JaversAuditableDelete(entity = Company::class)
  fun deleteCompany(id: String) {
    companyRepository.deleteById(id)
  }

  fun findById(id: String): Company {
    return companyRepository.findById(id).orElseThrow {
      IllegalArgumentException("Company not found: $id")
    }
  }

}
