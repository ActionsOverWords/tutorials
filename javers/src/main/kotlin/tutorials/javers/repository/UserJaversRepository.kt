package tutorials.javers.repository

import org.javers.spring.annotation.JaversSpringDataAuditable
import org.springframework.data.jpa.repository.JpaRepository
import tutorials.javers.domain.User

@JaversSpringDataAuditable
interface UserJaversRepository: JpaRepository<User, String> {
}
