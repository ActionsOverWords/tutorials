package tutorials.openfeign.querydsl.user.repository

import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.jpa.repository.JpaRepository
import tutorials.openfeign.querydsl.user.dto.QUserDto
import tutorials.openfeign.querydsl.user.dto.UserDto
import tutorials.openfeign.querydsl.user.model.QUser.Companion.user
import tutorials.openfeign.querydsl.user.model.Role
import tutorials.openfeign.querydsl.user.model.User

interface UserRepository : JpaRepository<User, String>, UserRepositoryCustom

interface UserRepositoryCustom {
  fun find(id: String): UserDto?
  fun findUsersByRole(role: Role): MutableList<User>
}

class UserRepositoryCustomImpl(
  private val queryFactory: JPAQueryFactory,
) : UserRepositoryCustom {
  override fun find(id: String): UserDto? {
    return queryFactory.select(
      QUserDto(user.id, user.name)
    )
      .from(user)
      .where(user.id.eq(id))
      .fetchOne()
  }

  override fun findUsersByRole(role: Role): MutableList<User> {
    return queryFactory.selectFrom(user)
      .where(user.role.eq(role))
      .fetch()
  }

}
