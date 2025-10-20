package tutorials.openfeign.querydsl.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import tutorials.openfeign.querydsl.user.dto.UserDto
import tutorials.openfeign.querydsl.user.dto.UserRegisterRequest
import tutorials.openfeign.querydsl.user.model.Name
import tutorials.openfeign.querydsl.user.model.User
import tutorials.openfeign.querydsl.user.repository.UserRepository

@RestController
class UserController(
  val userRepository: UserRepository
) {

  @PostMapping("/users")
  fun register(@RequestBody request: UserRegisterRequest): UserDto {
    val user = userRepository.save(User(
      name = Name(request.name),
      role = request.role
    ))

    return UserDto(id = user.id!!, name = user.name)
  }

  @GetMapping("/users/{id}")
  fun find(@PathVariable id: String): UserDto {
    return userRepository.find(id)
      ?: throw IllegalArgumentException("User not found")
  }

}
