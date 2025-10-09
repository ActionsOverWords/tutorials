package tutorials.openfeign.querydsl.user.dto

import com.querydsl.core.annotations.QueryProjection
import tutorials.openfeign.querydsl.user.model.Name
import tutorials.openfeign.querydsl.user.model.Role

data class UserRegisterRequest(
  val name: String,
  val role: Role,
)

@QueryProjection
data class UserDto(
  val id: String,
  val name: Name,
)

@QueryProjection
data class NestedUserDto(
  val id: String,
  val dto: UserDto,
)
