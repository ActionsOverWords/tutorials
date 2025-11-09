package tutorials.springdocs.dto

import tutorials.springdocs.domain.Gender
import tutorials.springdocs.domain.User
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE

data class UserRegisterRequest(
  val username: String,
  val password: String,
)

data class UserRegisterResponse(
  val id: String,
  val username: String,
) {
  companion object {
    fun of(user: User): UserRegisterResponse {
      return UserRegisterResponse(
        id = user.id!!,
        username = user.username
      )
    }
  }
}

data class UserDetailResponse(
  val id: String,
  val username: String,
  val nickname: String?,
  var birthDate: Instant?,
  var gender: Gender?,
) {
  companion object {
    fun of(user: User): UserDetailResponse {
      return UserDetailResponse(
        id = user.id!!,
        username = user.username,
        nickname = user.nickname,
        birthDate = user.birthDate,
        gender = user.gender
      )
    }
  }
}

data class UserUpdateRequest(
  val nickname: String? = null,
  var birthDate: String? = null,
  var gender: Gender? = null,
) {
  fun toInstantByBirthDate(): Instant? {
    return birthDate?.let {
      LocalDate.parse(it, ISO_LOCAL_DATE)
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
    }
  }

}
