package tutorials.proxysql.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import tutorials.proxysql.domain.Notice
import java.util.UUID

@Repository
interface NoticeRepository : JpaRepository<Notice, UUID> {
  fun findByEnabledTrueOrderByCreatedAtDesc(): List<Notice>
}
