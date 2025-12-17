package tutorials.javers.service

import jakarta.transaction.Transactional
import org.javers.spring.annotation.JaversAuditable
import org.javers.spring.annotation.JaversAuditableDelete
import org.springframework.stereotype.Service
import tutorials.javers.domain.User
import tutorials.javers.repository.UserJaversRepository
import tutorials.javers.repository.UserRepository

@Service
class UserJaversService(
  val userRepository: UserRepository,
  val userJaversRepository: UserJaversRepository,
) {

  @Transactional
  @JaversAuditable
  fun createUser(user: User): User {
    return userRepository.save(user)
  }

  @Transactional
  @JaversAuditable
  fun updateUser(user: User): User {
    return userRepository.save(user)
  }

  @Transactional
  @JaversAuditableDelete
  fun deleteUser(user: User) {
    userRepository.delete(user)
  }

  @Transactional
  @JaversAuditableDelete(entity = User::class)
  fun deleteUser(id: String) {
    userRepository.deleteById(id)
  }

  fun findById(id: String): User {
    return userJaversRepository.findById(id).orElseThrow {
      IllegalArgumentException("User not found: $id")
    }
  }

}
