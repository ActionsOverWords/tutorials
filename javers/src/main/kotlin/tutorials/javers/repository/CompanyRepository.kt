package tutorials.javers.repository

import org.springframework.data.jpa.repository.JpaRepository
import tutorials.javers.domain.Company

interface CompanyRepository: JpaRepository<Company, String> {
}
