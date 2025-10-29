package tutorials.jpa.user.dto

import com.querydsl.core.annotations.QueryProjection
import tutorials.jpa.user.model.User

data class UserDto(
  val id: String,
  val username: String,
  val name: String,
  val nickname: String? = null,
) {

  @QueryProjection
  constructor(user: User) : this(
    id = user.id!!,
    username = user.username,
    name = user.name,
    nickname = user.nickname
  )

}
