package tutorials.springdocs.domain

import java.time.Instant
import java.util.UUID.randomUUID

class User(
  val username: String,
  val password: String,
) {
  var id: String? = null

  var nickname: String? = null
  var birthDate: Instant? = null
  var gender: Gender? = Gender.UNKNOWN

  companion object {

    fun of(username: String, password: String): User {
      val user = User(username, password)
      user.id = "tutorials-${generateUUID()}"
      return user
    }

    private fun generateUUID() =
      randomUUID().toString().replace("-", "")

  }

  override fun toString(): String {
    return "User(id=$id, username='$username', password='***', nickname=$nickname, birthDate=$birthDate, gender=$gender)"
  }
}
