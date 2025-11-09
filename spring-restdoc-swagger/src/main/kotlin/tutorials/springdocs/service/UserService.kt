package tutorials.springdocs.service

import org.springframework.stereotype.Service
import tutorials.springdocs.base.extentions.logger
import tutorials.springdocs.domain.User
import tutorials.springdocs.dto.UserRegisterRequest
import tutorials.springdocs.dto.UserUpdateRequest

@Service
class UserService {

  val log = logger()

  val users = mutableMapOf<String, User>()

  fun register(request: UserRegisterRequest): User {
    val user = User.of(
      username = request.username,
      password = request.password,
    )
    log.debug("registering user {}", user)

    users[user.id!!] = user
    return user
  }

  fun find(id: String): User {
    if (id.lowercase().startsWith("tutorials")) {
      return users[id] ?: throw IllegalArgumentException("User not found")
    } else {
      throw IllegalArgumentException("Invalid user id")
    }
  }

  fun update(id: String, request: UserUpdateRequest) {
    val user = find(id)

    request.nickname?.let { user.nickname = it }
    request.birthDate?.let { user.birthDate = request.toInstantByBirthDate() }
    request.gender?.let { user.gender = it }

    log.debug("updating user {}", user)
  }

}
