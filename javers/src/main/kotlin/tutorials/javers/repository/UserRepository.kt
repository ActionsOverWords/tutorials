package tutorials.javers.repository

import org.springframework.data.jpa.repository.JpaRepository
import tutorials.javers.domain.User

interface UserRepository: JpaRepository<User, String> {
}
