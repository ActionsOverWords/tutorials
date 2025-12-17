package tutorials.javers.config

import org.javers.core.commit.CommitId
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import tutorials.javers.base.extentions.logger
import java.util.function.Supplier

class CustomCommitIdGenerator : Supplier<CommitId> {

  private val log by logger()

  companion object {
    private val transactionMajorId = ThreadLocal<Long>()
    private val transactionMinorSeq = ThreadLocal<Int>()
  }

  override fun get(): CommitId {
    if (!TransactionSynchronizationManager.isActualTransactionActive()) {
      // 트랜잭션이 없으면 표준 방식으로 생성 (major만 증가, minor=0)
      val commitId = CommitId(generateNewMajorId(), 0)
      log.debug("[No Transaction] Generated commitId: {}", commitId)
      return commitId
    }

    return try {
      // 트랜잭션 내: 동일한 major, minor만 증가
      val major = getOrCreateTransactionMajorId()
      val minor = transactionMinorSeq.get() ?: 0
      transactionMinorSeq.set(minor + 1)

      val commitId = CommitId(major, minor)
      log.trace("[Transaction] Generated commitId: {} (major={}, minor={})", commitId, major, minor)

      commitId
    } catch (e: Exception) {
      // 예외 발생 시 ThreadLocal 정리하여 메모리 누수 방지
      log.error("[Transaction] Error generating commitId, cleaning up ThreadLocal", e)
      cleanupThreadLocal()
      throw e
    }
  }

  private fun getOrCreateTransactionMajorId(): Long {
    return transactionMajorId.get() ?: run {
      val newMajor = generateNewMajorId()
      transactionMajorId.set(newMajor)
      transactionMinorSeq.set(0)

      log.debug("[Transaction] First commit in transaction - created new majorId: {}", newMajor)

      try {
        transactionSynchronization(newMajor)
      } catch (e: IllegalStateException) {
        log.error("[Transaction] Failed to register TransactionSynchronization, cleaning up immediately", e)
        cleanupThreadLocal()
        throw e
      }

      newMajor
    }
  }

  private fun generateNewMajorId(): Long {
    return System.currentTimeMillis()
  }

  private fun transactionSynchronization(majorId: Long) {
    TransactionSynchronizationManager.registerSynchronization(
      createTransactionCleanupSynchronization(majorId)
    )
  }

  private fun createTransactionCleanupSynchronization(majorId: Long) =
    object : TransactionSynchronization {
      override fun afterCompletion(status: Int) {
        log.debug(
          "[Transaction] Completed - clearing majorId: {} (status={})",
          majorId, getTransactionStatusName(status)
        )

        cleanupThreadLocal()
      }
    }

  private fun getTransactionStatusName(status: Int): String = when (status) {
    TransactionSynchronization.STATUS_COMMITTED -> "COMMITTED"
    TransactionSynchronization.STATUS_ROLLED_BACK -> "ROLLED_BACK"
    else -> "UNKNOWN($status)"
  }

  private fun cleanupThreadLocal() {
    transactionMajorId.remove()
    transactionMinorSeq.remove()
  }

}
