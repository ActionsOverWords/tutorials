package tutorials.lgtm.user.repository

import org.springframework.data.jpa.repository.JpaRepository
import tutorials.lgtm.user.entity.User

interface UserRepository: JpaRepository<User, String>
