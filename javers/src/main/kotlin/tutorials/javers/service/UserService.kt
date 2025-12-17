package tutorials.javers.service

import jakarta.transaction.Transactional
import org.javers.spring.annotation.JaversAuditable
import org.springframework.stereotype.Service
import tutorials.javers.domain.User
import tutorials.javers.repository.UserJaversRepository
import tutorials.javers.repository.UserRepository

@Service
class UserService(
  val userRepository: UserRepository,
) {

  @Transactional
  fun createUser(user: User): User {
    return userRepository.save(user)
  }

  @Transactional
  fun createUser(name: String, password: String): User {
    return userRepository.save(User(name = name, password = password))
  }

  @Transactional
  fun updateUser(user: User): User {
    return userRepository.save(user)
  }

  @Transactional
  fun updateUser(id: String, password: String, email: String? = null) {
    val user = findById(id)
    user.password = password
    email?.let { user.email = it }
    //userRepository.save(user)
  }

  @Transactional
  fun deleteUser(user: User) {
    userRepository.delete(user)
  }

  @Transactional
  fun deleteUser(id: String) {
    userRepository.deleteById(id)
  }

  fun findById(id: String): User {
    return userRepository.findById(id).orElseThrow {
      IllegalArgumentException("User not found: $id")
    }
  }

}
