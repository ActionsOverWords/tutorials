package tutorials.jpa.user.repository

import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.jpa.repository.JpaRepository
import tutorials.jpa.user.dto.QUserDto
import tutorials.jpa.user.dto.UserDto
import tutorials.jpa.user.model.QUser.user
import tutorials.jpa.user.model.User

interface UserRepository : JpaRepository<User, String>, UserRepositoryCustom

interface UserRepositoryCustom {
  fun findByName(name: String): MutableList<UserDto>
}

class UserRepositoryCustomImpl(
  private val queryFactory: JPAQueryFactory,
) : UserRepositoryCustom {

  override fun findByName(name: String): MutableList<UserDto> {
    return queryFactory.select(
      QUserDto(user)
    )
      .from(user)
      .where(user.name.contains(name))
      .fetch()
  }

}
