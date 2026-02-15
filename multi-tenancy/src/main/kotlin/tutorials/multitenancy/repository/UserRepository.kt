package tutorials.multitenancy.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import tutorials.multitenancy.domain.User

@Repository
interface UserRepository : JpaRepository<User, String> {
  fun findByUsername(username: String): User?
  fun findByUsernameAndEnabledTrue(username: String): User?
}
