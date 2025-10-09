package tutorials.openfeign.querydsl.user.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tutorials.openfeign.querydsl.config.AbstractContainerTest
import tutorials.openfeign.querydsl.user.model.Name
import tutorials.openfeign.querydsl.user.model.Role
import tutorials.openfeign.querydsl.user.model.User

class UserRepositoryTest(
  var userRepository: UserRepository,
) : AbstractContainerTest() {

  @Test
  fun save() {
    val user = User(
      name = Name("User"),
      role = Role.USER
    )

    val savedUser = userRepository.save(user)
    log.debug("user: {}", savedUser)

    assertThat(savedUser).isNotNull()
    assertThat(savedUser.id).isNotNull()
  }

  @Test
  fun find() {
    val userDto = userRepository.find("user-01")
    assertThat(userDto).isNull()
  }

  @Test
  fun findUsersByRole() {
    val users = userRepository.findUsersByRole(Role.ADMIN)
    assertThat(users).size().isEqualTo(0)
  }

}
