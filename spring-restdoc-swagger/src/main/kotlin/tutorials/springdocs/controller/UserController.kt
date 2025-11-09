package tutorials.springdocs.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import tutorials.springdocs.dto.UserDetailResponse
import tutorials.springdocs.dto.UserRegisterRequest
import tutorials.springdocs.dto.UserRegisterResponse
import tutorials.springdocs.dto.UserUpdateRequest
import tutorials.springdocs.service.UserService

@RestController
class UserController(
  val userService: UserService,
) {

  @PostMapping("/users")
  fun register(@RequestBody request: UserRegisterRequest): UserRegisterResponse {
    val user = userService.register(request)
    return UserRegisterResponse.of(user)
  }

  @GetMapping("/users/{id}")
  fun find(@PathVariable id: String): UserDetailResponse {
    val user = userService.find(id)
    return UserDetailResponse.of(user)
  }

  @PutMapping("/users/{id}")
  fun update(@PathVariable id: String, @RequestBody request: UserUpdateRequest): ResponseEntity<Unit> {
    userService.update(id, request)
    return ResponseEntity.ok().build()
  }

}
