package tutorials.thread.basic

import org.apache.commons.logging.LogFactory
import org.junit.jupiter.api.Test
import org.springframework.util.StopWatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.StructuredTaskScope
import java.util.concurrent.locks.ReentrantLock

class VirtualThreadTest {

  val log = LogFactory.getLog(javaClass)

  @Test
  fun start() {
    val thread = Thread.ofVirtual().start {
      log.info("run..")
    }

    log.info("thread: $thread")
  }

  @Test
  fun threadPerTaskExecutor() {
    val executor = Executors.newVirtualThreadPerTaskExecutor()

    executor.use { executor ->
      (0 until 10).forEach { _ ->
        executor.execute {
          log.info("run")
        }
      }
    }
  }

  interface PinningService {
    fun process()
  }

  inner class PinningServiceImpl(
    val name: String,
  ) : PinningService {
    @Synchronized
    override fun process() {
      val thread = Thread.currentThread()
      log.info("$thread")

      log.info("[$name] process start..")
      Thread.sleep(1000)
      log.info("[$name] process end..")
    }
  }

  inner class NonPinningServiceImpl(
    val name: String,
  ) : PinningService {
    private val lock = ReentrantLock()

    override fun process() {
      lock.lock()
      try {
        val thread = Thread.currentThread()
        log.info("$thread")

        log.info("[$name] process start..")
        Thread.sleep(1000)
        log.info("[$name] process end..")
      } finally {
        lock.unlock()
      }
    }
  }

  @Test
  fun pinningTest() {
    pinningServiceExecMillis(
      PinningServiceImpl("first"),
      PinningServiceImpl("second"),
    )

    pinningServiceExecMillis(
      NonPinningServiceImpl("first"),
      NonPinningServiceImpl("second"),
    )
  }

  private fun pinningServiceExecMillis(
    vararg pinningServices: PinningService,
  ) {
    val executor = Executors.newVirtualThreadPerTaskExecutor()

    val stopWatch = StopWatch()
    stopWatch.start()

    pinningServices.forEach { s ->
      executor.execute { s.process() }
    }

    shutdown(executor)
    stopWatch.stop()

    log.info("실행 시간: ${stopWatch.totalTimeMillis}ms\n")
  }

  private fun shutdown(executor: ExecutorService) {
    executor.shutdown()

    while (!executor.isTerminated) {
      Thread.sleep(100)
    }
  }

  // ===== Structured Concurrency Test =====
  data class ApiResult(val name: String, val data: String)

  private fun fetchData(name: String, delayMs: Long, shouldFail: Boolean = false): ApiResult {
    log.info("[$name] 요청 시작")
    Thread.sleep(delayMs)
    if (shouldFail) {
      log.error("[$name] 요청 실패!")
      throw RuntimeException("$name API 호출 실패")
    }
    log.info("[$name] 요청 완료")
    return ApiResult(name, "$name-data")
  }

  @Test
  fun structuredConcurrencyTest() {
    log.info("\n=== 1. ExecutorService ===")
    executorServicePattern()

    Thread.sleep(500)

    log.info("\n=== 2. StructuredTaskScope.ShutdownOnFailure ===")
    structuredTaskScopeFailurePattern()

    Thread.sleep(500)

    log.info("\n=== 3. StructuredTaskScope.ShutdownOnSuccess ===")
    structuredTaskScopeSuccessPattern()
  }

  /**
   * 안티패턴: ExecutorService 사용
   * - 한 작업 실패 시 다른 작업 수동 취소 필요
   * - 복잡한 에러 처리
   * - 스레드 누수 위험
   */
  private fun executorServicePattern() {
    val executor = Executors.newVirtualThreadPerTaskExecutor()
    val futures = mutableListOf<Future<ApiResult>>()

    val stopWatch = StopWatch()
    stopWatch.start()

    try {
      futures.add(executor.submit<ApiResult> { fetchData("User", 500) })
      futures.add(executor.submit<ApiResult> { fetchData("Order", 1000, true) })
      futures.add(executor.submit<ApiResult> { fetchData("Stats", 1500) })

      val results = futures.map { it.get() }
      log.info("모든 작업 성공: $results")
    } catch (e: Exception) {
      log.error("작업 실패 감지: ${e.message}")

      futures.forEach { future ->
        if (!future.isDone) {
          log.warn("미완료 작업 취소 중...")
          future.cancel(true)
        }
      }
    } finally {
      shutdown(executor)
      stopWatch.stop()
      log.info("ExecutorService 실행 시간: ${stopWatch.totalTimeMillis}ms")
    }
  }

  /**
   * 권장: StructuredTaskScope.ShutdownOnFailure
   * - 한 작업 실패 시 나머지 자동 취소
   * - 간단한 에러 처리
   */
  private fun structuredTaskScopeFailurePattern() {
    val stopWatch = StopWatch()
    stopWatch.start()

    try {
      StructuredTaskScope.ShutdownOnFailure().use { scope ->
        val userTask = scope.fork { fetchData("User", 500) }
        val orderTask = scope.fork { fetchData("Order", 1000, shouldFail = true) }
        val statsTask = scope.fork { fetchData("Stats", 1500) }

        scope.join()
        scope.throwIfFailed()

        log.info("모든 작업 성공: ${userTask.get()}, ${orderTask.get()}, ${statsTask.get()}")
      }
    } catch (e: ExecutionException) {
      log.error("작업 실패: ${e.cause?.message}")
      log.info("장점: 나머지 작업 자동 취소됨!")
    } finally {
      stopWatch.stop()
      log.info("StructuredTaskScope 실행 시간: ${stopWatch.totalTimeMillis}ms")
    }
  }

  /**
   * ShutdownOnSuccess: 첫 번째 성공 결과만 필요
   * - 가장 빠른 응답 사용 (Racing)
   * - Fallback 패턴
   */
  private fun structuredTaskScopeSuccessPattern() {
    val stopWatch = StopWatch()
    stopWatch.start()

    try {
      StructuredTaskScope.ShutdownOnSuccess<ApiResult>().use { scope ->
        scope.fork { fetchData("Primary-DB", 1000) }
        scope.fork { fetchData("Cache", 300) }
        scope.fork { fetchData("Backup-DB", 800) }

        scope.join()
        val result = scope.result()

        log.info("가장 빠른 결과: $result")
      }
    } catch (e: ExecutionException) {
      log.error("모든 작업 실패: ${e.message}")
    } finally {
      stopWatch.stop()
      log.info("ShutdownOnSuccess 실행 시간: ${stopWatch.totalTimeMillis}ms")
    }
  }

  // ===== ScopedValue Test =====
  data class User(val id: String, val name: String)

  companion object {
    private val CURRENT_USER: ScopedValue<User> = ScopedValue.newInstance()
  }

  @Test
  fun scopedValueTest() {
    log.info("\n=== 1. ThreadLocal (안티패턴) ===")
    threadLocalPattern()

    Thread.sleep(500)

    log.info("\n=== 2. ScopedValue (권장) ===")
    scopedValuePattern()

    Thread.sleep(500)

    log.info("\n=== 3. ScopedValue + Virtual Thread 조합 ===")
    scopedValueWithVirtualThreads()
  }

  /**
   * ThreadLocal의 문제점 시뮬레이션
   * - 명시적 remove 필요
   * - 가변성으로 인한 예측 어려움
   */
  private fun threadLocalPattern() {
    val threadLocal = ThreadLocal<User>()

    try {
      threadLocal.set(User("1", "Alice"))
      log.info("ThreadLocal 설정: ${threadLocal.get()}")

      // Virtual Thread에서 ThreadLocal 접근 시 전파 안됨
      Thread.startVirtualThread {
        log.info("Virtual Thread에서 ThreadLocal get(): ${threadLocal.get()}") // null
      }.join()

    } finally {
      threadLocal.remove() // 명시적 제거 필요 (안하면 메모리 누수)
      log.info("ThreadLocal 제거 후: ${threadLocal.get()}")
    }
  }

  /**
   * ScopedValue의 장점
   * - 불변
   * - 자동 정리
   * - Virtual Thread에 자동 전파
   */
  private fun scopedValuePattern() {
    val user = User("1", "Alice")

    ScopedValue.runWhere(CURRENT_USER, user) {
      log.info("ScopedValue 설정: ${CURRENT_USER.get()}")

      // StructuredTaskScope 사용하여 ScopedValue 자동 전파 보장
      StructuredTaskScope.ShutdownOnFailure().use { scope ->
        scope.fork {
          log.info("Virtual Thread에서 ScopedValue: ${CURRENT_USER.get()}")
          null
        }
        scope.join()
        scope.throwIfFailed()
      }

      // 범위 내에서 처리
      processRequest()
    }

    // 범위 밖에서는 접근 불가
    try {
      val value = CURRENT_USER.get()
      log.error("예외가 발생해야 하는데 값이 나옴: $value")
    } catch (e: NoSuchElementException) {
      log.info("범위 밖에서 접근 시 예외 발생 (예상된 동작): ${e.message}")
    }
  }

  private fun processRequest() {
    val user = CURRENT_USER.get()
    log.info("processRequest - 현재 사용자: $user")
  }

  /**
   * ScopedValue + Virtual Thread 실전 예제
   * - 여러 Virtual Thread에서 각각 다른 ScopedValue 사용
   */
  private fun scopedValueWithVirtualThreads() {
    val users = listOf(
      User("1", "Alice"),
      User("2", "Bob"),
      User("3", "Charlie")
    )

    Executors.newVirtualThreadPerTaskExecutor().use { executor ->
      users.forEach { user ->
        executor.submit {
          ScopedValue.runWhere(CURRENT_USER, user) {
            log.info("[${Thread.currentThread()}] 요청 처리 시작 - 사용자: ${CURRENT_USER.get()}")

            Thread.sleep(100)

            nestedCall()

            log.info("[${Thread.currentThread()}] 요청 처리 완료 - 사용자: ${CURRENT_USER.get()}")
          }
        }
      }
    }
  }

  private fun nestedCall() {
    val user = CURRENT_USER.get()
    log.info("  nestedCall - 현재 사용자: ${user.name}")
  }
}
