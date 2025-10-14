package tutorials.lgtm.user.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import tutorials.lgtm.user.entity.Name
import tutorials.lgtm.user.entity.Role
import tutorials.lgtm.user.entity.User
import tutorials.lgtm.user.repository.UserRepository

@RestController
class UserController(
  val userRepository: UserRepository,
) {

  @PostMapping("/users")
  fun register(username: String, role: String): UserDto {
    val user = userRepository.save(
      User(
        name = Name(username),
        role = Role.valueOf(role.uppercase())
      )
    )

    return UserDto.from(user)
  }

  @GetMapping("/users")
  fun list(): List<UserDto> {
    return userRepository.findAll()
      .map { UserDto.from(it) }
  }

  @GetMapping("/users/{id}")
  fun get(@PathVariable id: String): UserDto? {
    return userRepository.findById(id)
      .map { UserDto.from(it) }
      .orElseThrow { IllegalArgumentException("User not found") }
  }

}

data class UserDto(
  val id: String,
  val name: String,
  val role: Role,
) {
  companion object {
    fun from(user: User) = UserDto(
      user.id!!,
      user.name.name,
      user.role
    )
  }
}
