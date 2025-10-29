package tutorials.jpa.user.repository

import org.junit.jupiter.api.Test
import tutorials.jpa.config.AbstractContainerTest
import tutorials.jpa.user.model.User

class UserRepositoryTest(
  var userRepository: UserRepository,
) : AbstractContainerTest() {

  @Test
  fun findByName() {
    userRepository.save(User(username = "user-01", name = "full-name-01", nickname = "nickname"))
    userRepository.save(User(username = "user-02", name = "full-name-02", nickname = "tutorials"))
    userRepository.save(User(username = "user-03", name = "full-name-03", nickname = "tutorials"))

    flushAndClear()

    val users = userRepository.findByName("name")
    users.forEach { log.info("user: {}", it) }

    assert(users.isNotEmpty())
  }

}
