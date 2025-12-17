package tutorials.javers.service

import org.javers.core.Javers
import org.javers.core.commit.CommitId
import org.javers.repository.jql.QueryBuilder
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.support.PageableExecutionUtils
import org.springframework.stereotype.Service
import tutorials.javers.dto.AuditDetailDto
import tutorials.javers.dto.AuditDto
import tutorials.javers.dto.SearchCondition
import tutorials.javers.repository.JaVersRepository
import java.math.BigDecimal

@Service
class JaVersService(
  private val javers: Javers,
  private val jaVersRepository: JaVersRepository,
) {

  fun searchAuditHistory(condition: SearchCondition): Page<AuditDto> {
    val pageable = PageRequest.of(condition.pageNumber, condition.pageSize)

    val commitIds = jaVersRepository.findCommitIdsByCondition(condition, pageable)

    if (commitIds.isEmpty()) {
      return PageImpl(emptyList(), pageable, 0)
    }

    val auditDtos = javers.findSnapshots(
      QueryBuilder.anyDomainObject()
        .withCommitIds(commitIds)
        .build()
    )
      .groupBy { it.commitMetadata.id }
      .map { (commitId, snapshots) ->
        AuditDto.from(commitId, snapshots)
      }
      .sortedByDescending { it.commitDate }

    return PageableExecutionUtils.getPage(auditDtos, pageable) {
      jaVersRepository.countByCondition(condition)
    }
  }

  fun getAuditDetail(commitId: BigDecimal): AuditDetailDto {
    require(jaVersRepository.existsCommitById(commitId)) {
      "CommitId[$commitId] not found"
    }

    val changes = javers.findChanges(
      QueryBuilder.anyDomainObject()
        .withCommitId(CommitId.valueOf(commitId))
        .build()
    )

    require(changes.isNotEmpty()) {
      "No changes found for commit id $commitId"
    }

    return AuditDetailDto.from(changes)
  }

}
