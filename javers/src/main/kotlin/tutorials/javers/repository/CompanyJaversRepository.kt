package tutorials.javers.repository

import org.javers.spring.annotation.JaversSpringDataAuditable
import org.springframework.data.jpa.repository.JpaRepository
import tutorials.javers.domain.Company

@JaversSpringDataAuditable
interface CompanyJaversRepository: JpaRepository<Company, String> {
}
