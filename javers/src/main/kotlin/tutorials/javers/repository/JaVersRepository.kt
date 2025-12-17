package tutorials.javers.repository

import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import tutorials.javers.domain.javers.QJvCommit.jvCommit
import tutorials.javers.domain.javers.QJvSnapshot.jvSnapshot
import tutorials.javers.dto.SearchCondition
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
class JaVersRepository(
  private val queryFactory: JPAQueryFactory
) {

  fun findCommitIdsByCondition(
    condition: SearchCondition,
    pageable: Pageable
  ): List<BigDecimal> {
    return queryFactory
      .select(jvCommit.commitId)
      .from(jvCommit)
      .where(
        authorEq(condition.author),
        commitDateGoe(condition.fromDate),
        commitDateLoe(condition.toDate),
        domainTypeContains(condition.domainType)
      )
      .orderBy(jvCommit.commitDate.desc(), jvCommit.commitId.desc())
      .offset(pageable.offset)
      .limit(pageable.pageSize.toLong())
      .distinct()
      .fetch()
      .filterNotNull()
  }

  fun countByCondition(condition: SearchCondition): Long {
    return queryFactory
      .select(jvCommit.commitPk.countDistinct())
      .from(jvCommit)
      .where(
        authorEq(condition.author),
        commitDateGoe(condition.fromDate),
        commitDateLoe(condition.toDate),
        domainTypeContains(condition.domainType)
      )
      .fetchOne() ?: 0L
  }

  private fun authorEq(author: String?): BooleanExpression? {
    return author?.let { jvCommit.author.eq(it) }
  }

  private fun commitDateGoe(fromDate: LocalDateTime?): BooleanExpression? {
    return fromDate?.let { jvCommit.commitDate.goe(it) }
  }

  private fun commitDateLoe(toDate: LocalDateTime?): BooleanExpression? {
    return toDate?.let { jvCommit.commitDate.loe(it) }
  }

  private fun domainTypeContains(domainType: String?): BooleanExpression? {
    return domainType?.let {
      queryFactory
        .selectOne()
        .from(jvSnapshot)
        .where(
          jvSnapshot.commit.commitPk.eq(jvCommit.commitPk),
          jvSnapshot.managedType.contains(it)
        )
        .exists()
    }
  }

  fun existsCommitById(commitId: BigDecimal): Boolean {
    return queryFactory
      .selectOne()
      .from(jvCommit)
      .where(jvCommit.commitId.eq(commitId))
      .fetchFirst() != null
  }
}
