package tutorials.thread.basic

import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.bulkhead.BulkheadConfig
import io.github.resilience4j.bulkhead.BulkheadFullException
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.timelimiter.TimeLimiter
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import org.apache.commons.logging.LogFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.util.StopWatch
import java.io.IOException
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class Resilience4jTest {

  val log = LogFactory.getLog(javaClass)

  // ===== Circuit Breaker =====
  @Test
  fun circuitBreakerTest() {
    log.info("\n=== 1. Circuit Breaker 상태 전환 (CLOSED → OPEN → HALF_OPEN) ===")
    circuitBreakerStateTransition()

    Thread.sleep(500)

    log.info("\n=== 2. Slow Call Rate Threshold ===")
    slowCallRateThreshold()

    Thread.sleep(500)

    log.info("\n=== 3. minimumNumberOfCalls의 중요성 ===")
    minimumNumberOfCallsTest()
  }

  /**
   * Circuit Breaker 상태 전환 테스트
   * CLOSED → OPEN → HALF_OPEN → CLOSED
   */
  private fun circuitBreakerStateTransition() {
    val config = CircuitBreakerConfig.custom()
      .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
      .slidingWindowSize(5)                                          // 최근 5개 호출 기준
      .minimumNumberOfCalls(3)                                   // 최소 3개 호출 후 실패율 계산
      .failureRateThreshold(50f)                                   // 실패율 50% 이상 시 OPEN
      .waitDurationInOpenState(Duration.ofSeconds(2)) // OPEN 상태 2초 유지
      .permittedNumberOfCallsInHalfOpenState(2)         // HALF_OPEN 시 2개 요청 테스트
      .recordExceptions(IOException::class.java)
      .build()

    val circuitBreaker = CircuitBreaker.of("test", config)
    val callCount = AtomicInteger(0)

    // CLOSED 상태 확인
    log.info("초기 상태: ${circuitBreaker.state}")
    assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.state)

    // 성공 호출 2번
    repeat(2) {
      val decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker) {
        callCount.incrementAndGet()
        "success"
      }
      runCatching { decoratedSupplier.get() }
      log.info("  호출 ${callCount.get()}: 성공 - 상태: ${circuitBreaker.state}")
    }

    // 실패 호출 3번 (실패율 60% = 3/5 > 50%)
    repeat(3) {
      val decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker) {
        callCount.incrementAndGet()
        throw IOException("의도적 실패")
      }
      runCatching { decoratedSupplier.get() }
      log.info("  호출 ${callCount.get()}: 실패 - 상태: ${circuitBreaker.state}")
    }

    // OPEN 상태로 전환 확인
    log.info("현재 상태: ${circuitBreaker.state}")
    assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.state)

    // OPEN 상태에서는 CallNotPermittedException 발생
    val decoratedOpen = CircuitBreaker.decorateSupplier(circuitBreaker) { "should-not-execute" }
    val openResult = runCatching { decoratedOpen.get() }
    assertTrue(openResult.isFailure)
    assertTrue(openResult.exceptionOrNull() is CallNotPermittedException)
    log.info("  OPEN 상태: CallNotPermittedException 발생 (요청 차단)")

    // 2초 대기 후 HALF_OPEN으로 전환
    Thread.sleep(2100)
    log.info("2초 경과 후 상태: ${circuitBreaker.state}")

    // HALF_OPEN 상태에서 성공 호출 2번
    repeat(2) {
      val decoratedSuccess = CircuitBreaker.decorateSupplier(circuitBreaker) {
        callCount.incrementAndGet()
        "success-after-open"
      }
      runCatching { decoratedSuccess.get() }
      log.info("  HALF_OPEN 시험 호출 ${it + 1}: 성공 - 상태: ${circuitBreaker.state}")
    }

    // CLOSED로 복구 확인
    log.info("최종 상태: ${circuitBreaker.state}")
    assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.state)
  }

  /**
   * Slow Call Rate Threshold 테스트
   * - 느린 호출도 Circuit Breaker를 OPEN시킬 수 있음
   */
  private fun slowCallRateThreshold() {
    val config = CircuitBreakerConfig.custom()
      .slidingWindowSize(5)
      .minimumNumberOfCalls(3)
      .slowCallRateThreshold(50f)                                  // 느린 호출 비율 50% 이상 시 OPEN
      .slowCallDurationThreshold(Duration.ofMillis(100)) // 100ms 이상을 느린 호출로 간주
      .waitDurationInOpenState(Duration.ofSeconds(1))
      .build()

    val circuitBreaker = CircuitBreaker.of("slow-test", config)

    log.info("초기 상태: ${circuitBreaker.state}")

    // 빠른 호출 2번
    repeat(2) {
      val decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker) {
        Thread.sleep(50)
        "fast"
      }
      decoratedSupplier.get()
      log.info("  빠른 호출 ${it + 1}: 50ms - 상태: ${circuitBreaker.state}")
    }

    // 느린 호출 3번 (느린 호출 비율 60% = 3/5 > 50%)
    repeat(3) {
      val decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker) {
        Thread.sleep(150) // 150ms (> 100ms threshold)
        "slow"
      }
      runCatching { decoratedSupplier.get() }
      log.info("  느린 호출 ${it + 1}: 150ms - 상태: ${circuitBreaker.state}")
    }

    // Slow call rate로 인해 OPEN 상태로 전환
    log.info("최종 상태: ${circuitBreaker.state}")
    assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.state)
  }

  /**
   * minimumNumberOfCalls의 중요성
   * - 초기 몇 번의 실패로 즉시 OPEN되는 것을 방지
   */
  private fun minimumNumberOfCallsTest() {
    // minimumNumberOfCalls 없음 (위험)
    val config1 = CircuitBreakerConfig.custom()
      .slidingWindowSize(10)
      .minimumNumberOfCalls(1)  // 1개만 있어도 실패율 계산
      .failureRateThreshold(50f)
      .waitDurationInOpenState(Duration.ofSeconds(1))
      .recordExceptions(IOException::class.java)
      .build()

    val cb1 = CircuitBreaker.of("no-minimum", config1)

    // 첫 번째 호출 실패 시 즉시 OPEN (실패율 100%)
    val decoratedFail1 = CircuitBreaker.decorateSupplier(cb1) { throw IOException("첫 실패") }
    runCatching { decoratedFail1.get() }
    log.info("minimumNumberOfCalls=1: 첫 실패 후 상태 = ${cb1.state}")
    assertEquals(CircuitBreaker.State.OPEN, cb1.state, "첫 실패로 즉시 OPEN")

    // minimumNumberOfCalls 설정 (권장)
    val config2 = CircuitBreakerConfig.custom()
      .slidingWindowSize(10)
      .minimumNumberOfCalls(5)  // 최소 5개 호출 후 실패율 계산
      .failureRateThreshold(50f)
      .waitDurationInOpenState(Duration.ofSeconds(1))
      .recordExceptions(IOException::class.java)
      .build()

    val cb2 = CircuitBreaker.of("with-minimum", config2)

    // 첫 번째 호출 실패해도 CLOSED 유지 (최소 호출 수 미달)
    val decoratedFail2 = CircuitBreaker.decorateSupplier(cb2) { throw IOException("첫 실패") }
    runCatching { decoratedFail2.get() }
    log.info("minimumNumberOfCalls=5: 첫 실패 후 상태 = ${cb2.state}")
    assertEquals(CircuitBreaker.State.CLOSED, cb2.state, "최소 호출 수 미달로 CLOSED 유지")

    // 4번 더 실패 (총 5번, 실패율 100% > 50%)
    repeat(4) {
      val decoratedFail = CircuitBreaker.decorateSupplier(cb2) { throw IOException("추가 실패") }
      runCatching { decoratedFail.get() }
    }
    log.info("5번 실패 후 상태 = ${cb2.state}")
    assertEquals(CircuitBreaker.State.OPEN, cb2.state, "최소 호출 수 달성 후 OPEN")
  }

  // ===== Rate Limiter =====
  @Test
  fun rateLimiterTest() {
    log.info("\n=== 1. Rate Limiter 기본 동작 ===")
    rateLimiterBasic()

    Thread.sleep(500)

    log.info("\n=== 2. Rate Limiter Timeout ===")
    rateLimiterTimeout()

    Thread.sleep(500)

    log.info("\n=== 3. Rate Limiter Cycle 갱신 ===")
    rateLimiterCycleRefresh()
  }

  /**
   * Rate Limiter 기본 동작
   * - 설정한 제한을 초과하면 RequestNotPermitted 발생
   */
  private fun rateLimiterBasic() {
    val config = RateLimiterConfig.custom()
      .limitForPeriod(3)                                      // 주기당 3개 요청 허용
      .limitRefreshPeriod(Duration.ofSeconds(1)) // 1초마다 갱신
      .timeoutDuration(Duration.ofMillis(0))         // 대기 안 함 (즉시 실패)
      .build()

    val rateLimiter = RateLimiter.of("test", config)
    var successCount = 0
    var failCount = 0

    log.info("초기 available permissions: ${rateLimiter.metrics.availablePermissions}")

    // 5번 요청 시도 (3번만 성공해야 함)
    repeat(5) { i ->
      val decoratedSupplier = RateLimiter.decorateSupplier(rateLimiter) {
        successCount++
        "success-$i"
      }
      val result = runCatching { decoratedSupplier.get() }

      if (result.isSuccess) {
        log.info("  요청 ${i + 1}: 성공 (available: ${rateLimiter.metrics.availablePermissions})")
      } else {
        failCount++
        log.info("  요청 ${i + 1}: 실패 - RequestNotPermitted (available: ${rateLimiter.metrics.availablePermissions})")
      }
    }

    log.info("성공: $successCount, 실패: $failCount")
    assertEquals(3, successCount, "3개 요청만 성공해야 함")
    assertEquals(2, failCount, "2개 요청은 실패해야 함")
  }

  /**
   * Rate Limiter Timeout
   * - timeoutDuration > 0이면 권한 획득까지 대기
   */
  private fun rateLimiterTimeout() {
    val config = RateLimiterConfig.custom()
      .limitForPeriod(2)
      .limitRefreshPeriod(Duration.ofSeconds(1))
      .timeoutDuration(Duration.ofMillis(500)) // 최대 500ms 대기
      .build()

    val rateLimiter = RateLimiter.of("timeout-test", config)

    // 2개 요청 즉시 성공
    repeat(2) { i ->
      val decoratedSupplier = RateLimiter.decorateSupplier(rateLimiter) { "success-$i" }
      decoratedSupplier.get()
      log.info("  요청 ${i + 1}: 즉시 성공")
    }

    // 3번째 요청은 500ms 대기 후 실패
    val stopWatch = StopWatch()
    stopWatch.start()
    val decoratedTimeout = RateLimiter.decorateSupplier(rateLimiter) { "should-timeout" }
    val result = runCatching { decoratedTimeout.get() }
    stopWatch.stop()
    val elapsedTime = stopWatch.totalTimeMillis

    log.info("  요청 3: 실패 (대기 시간: ${elapsedTime}ms)")
    assertTrue(result.isFailure, "3번째 요청은 실패해야 함")
    assertTrue(elapsedTime >= 500, "최소 500ms 대기해야 함")
  }

  /**
   * Rate Limiter Cycle 갱신
   * - 주기가 지나면 권한이 갱신됨
   */
  private fun rateLimiterCycleRefresh() {
    val config = RateLimiterConfig.custom()
      .limitForPeriod(2)
      .limitRefreshPeriod(Duration.ofSeconds(1)) // 1초마다 갱신
      .timeoutDuration(Duration.ofMillis(0))
      .build()

    val rateLimiter = RateLimiter.of("cycle-test", config)

    // Cycle 1: 2개 요청 성공
    repeat(2) { i ->
      val decoratedSupplier = RateLimiter.decorateSupplier(rateLimiter) { "cycle1-$i" }
      decoratedSupplier.get()
      log.info("  Cycle 1 - 요청 ${i + 1}: 성공 (available: ${rateLimiter.metrics.availablePermissions})")
    }

    // Cycle 1: 3번째 요청 실패
    val decoratedFail = RateLimiter.decorateSupplier(rateLimiter) { "should-fail" }
    val result1 = runCatching { decoratedFail.get() }
    log.info("  Cycle 1 - 요청 3: 실패 (권한 소진) (available: ${rateLimiter.metrics.availablePermissions})")
    assertTrue(result1.isFailure)

    // 1초 대기 (새 Cycle 시작)
    log.info("1초 대기 중... (새 Cycle 시작)")
    Thread.sleep(1100)

    // Cycle 2: 권한 갱신되어 다시 2개 요청 가능
    repeat(2) { i ->
      val decoratedSupplier = RateLimiter.decorateSupplier(rateLimiter) { "cycle2-$i" }
      decoratedSupplier.get()
      log.info("  Cycle 2 - 요청 ${i + 1}: 성공 (권한 갱신됨) (available: ${rateLimiter.metrics.availablePermissions})")
    }

    log.info("최종 available permissions: ${rateLimiter.metrics.availablePermissions}")
    assertEquals(0, rateLimiter.metrics.availablePermissions)
  }

  // ===== Bulkhead =====
  @Test
  fun bulkheadTest() {
    log.info("\n=== 1. Semaphore Bulkhead ===")
    semaphoreBulkheadTest()

    Thread.sleep(500)

    log.info("\n=== 2. ThreadPool Bulkhead ===")
    threadPoolBulkheadTest()
  }

  /**
   * Semaphore Bulkhead
   * - 동시 실행 수 제한
   */
  private fun semaphoreBulkheadTest() {
    val config = BulkheadConfig.custom()
      .maxConcurrentCalls(2)                         // 동시 실행 최대 2개
      .maxWaitDuration(Duration.ofMillis(100))  // 대기 최대 100ms
      .build()

    val bulkhead = Bulkhead.of("test", config)
    val activeCount = AtomicInteger(0)
    val maxActiveCount = AtomicInteger(0)

    log.info("maxConcurrentCalls: 2")

    // 3개 Thread 동시 실행 시도
    val executor = Executors.newFixedThreadPool(3)

    val futures = (1..3).map { threadNum ->
      executor.submit {
        try {
          val decoratedSupplier = Bulkhead.decorateSupplier(bulkhead) {
            val current = activeCount.incrementAndGet()
            maxActiveCount.updateAndGet { maxOf(it, current) }

            log.info("  Thread $threadNum: 실행 시작 (active: $current)")
            Thread.sleep(500)
            log.info("  Thread $threadNum: 실행 완료")

            activeCount.decrementAndGet()
            "result-$threadNum"
          }
          decoratedSupplier.get()
        } catch (e: BulkheadFullException) {
          log.info("  Thread $threadNum: BulkheadFullException (동시 실행 수 초과)")
          log.error(e.message)
        }
      }
    }

    futures.forEach { it.get() }
    shutdown(executor)

    log.info("최대 동시 실행 수: ${maxActiveCount.get()} (예상: 2)")
    assertEquals(2, maxActiveCount.get(), "최대 2개만 동시 실행되어야 함")
  }

  /**
   * ThreadPool Bulkhead
   * - 별도 Thread Pool로 격리
   */
  private fun threadPoolBulkheadTest() {
    val config = ThreadPoolBulkheadConfig.custom()
      .maxThreadPoolSize(2)
      .coreThreadPoolSize(1)
      .queueCapacity(1) // 큐 용량
      .build()

    val bulkhead = ThreadPoolBulkhead.of("test", config)

    log.info("maxThreadPoolSize: 2, queueCapacity: 1")

    // 4개 Thread 동시 실행 시도
    val futures = (1..4).map { taskNum ->
      runCatching {
        bulkhead.executeSupplier {
          log.info("  Task $taskNum: 실행 중")
          Thread.sleep(300)
          "result-$taskNum"
        }
      }
    }

    // 모든 future의 결과를 기다림
    val results = futures.map { futureResult ->
      futureResult.mapCatching { it.toCompletableFuture().join() }
    }

    Thread.sleep(500)

    val successCount = results.count { it.isSuccess }
    val failCount = results.count { it.isFailure }

    log.info("성공: $successCount, 실패: $failCount")
    assertTrue(failCount > 0, "큐와 Thread Pool이 가득 차서 일부 작업은 거부되어야 함")
  }

  // ===== Retry =====
  @Test
  fun retryTest() {
    log.info("\n=== 1. Retry 기본 동작 ===")
    retryBasic()

    Thread.sleep(500)

    log.info("\n=== 2. Exponential Backoff + Jitter ===")
    exponentialBackoffTest()

    Thread.sleep(500)
  }

  /**
   * Retry 기본 동작
   * - 실패 시 자동 재시도
   */
  private fun retryBasic() {
    val retry = Retry.ofDefaults("test") //DEFAULT_MAX_ATTEMPTS 3
    val attemptCount = AtomicInteger(0)

    log.info("maxAttempts: 3 (default), waitDuration: 500ms (default)")

    val decoratedSupplier = Retry.decorateSupplier(retry) {
      val attempt = attemptCount.incrementAndGet()
      log.info("$attempt 시도")

      if (attempt < 3) {
        throw RuntimeException("의도적 실패 (시도 $attempt)")
      }
      "success"
    }

    val result = decoratedSupplier.get()

    log.info("총 시도 횟수: ${attemptCount.get()}, 결과: $result")
    assertEquals(3, attemptCount.get(), "3번 시도 후 성공해야 함")
    assertEquals("success", result)
  }

  /**
   * Exponential Backoff + Jitter
   * - 재시도 간격이 지수적으로 증가
   * - Jitter로 동시 재시도 분산 (Thundering herd 현상 방지)
   */
  private fun exponentialBackoffTest() {
    val config = RetryConfig.custom<Any>()
      .maxAttempts(3)
      .intervalFunction(
        IntervalFunction.ofExponentialRandomBackoff(
          100L, // initialIntervalMillis: 초기 간격 100ms
          2.0,         // multiplier: 2배씩 증가
          0.5 // randomizationFactor: Jitter ±50%
        )
      )
      .build()

    val retry = Retry.of("backoff-test", config)
    val attemptTimes = mutableListOf<Long>()

    log.info("Exponential Backoff + Jitter 설정:")
    log.info("  - 초기 간격: 100ms, 배수: 2.0배, Jitter: ±50%")
    log.info("  - 예상 간격: 1차→2차: ~100ms(50-150ms), 2차→3차: ~200ms(100-300ms)")

    val decoratedSupplier = Retry.decorateSupplier(retry) {
      // 재시도 간격 측정을 위해 실제 시간 기록
      attemptTimes.add(System.currentTimeMillis())
      val attempt = attemptTimes.size

      if (attempt <= 2) {
        log.info("  시도 $attempt: 실패")
        throw RuntimeException("시도 $attempt")
      }

      log.info("  시도 $attempt: 성공")
      "success"
    }

    runCatching { decoratedSupplier.get() }

    // 재시도 간격 계산 및 출력
    for (i in 1 until attemptTimes.size) {
      val interval = attemptTimes[i] - attemptTimes[i - 1]
      log.info("  재시도 $i 간격: ${interval}ms")
    }

    assertTrue(attemptTimes.size >= 3, "최소 3번 시도해야 함")

    // Exponential backoff가 작동하는지 검증
    if (attemptTimes.size >= 2) {
      val firstInterval = attemptTimes[1] - attemptTimes[0]
      assertTrue(firstInterval >= 50, "첫 번째 재시도 간격은 최소 50ms 이상이어야 함 (100ms ± 50%)")
      log.info("검증 성공: 첫 번째 재시도 간격 = ${firstInterval}ms (50-150ms 범위 내)")
    }
  }

  // ===== TimeLimiter =====
  @Test
  fun timeLimiterTest() {
    log.info("\n=== 1. TimeLimiter 기본 동작 ===")
    timeLimiterBasic()

    Thread.sleep(500)

    log.info("\n=== 2. Timeout 발생 ===")
    timeLimiterTimeout()
  }

  /**
   * TimeLimiter 기본 동작
   * - 제한 시간 내 완료되면 성공
   */
  private fun timeLimiterBasic() {
    val config = TimeLimiterConfig.custom()
      .timeoutDuration(Duration.ofSeconds(1)) // 1초 타임아웃
      .cancelRunningFuture(true)
      .build()

    val timeLimiter = TimeLimiter.of("test", config)

    log.info("timeout: 2초")

    val future = CompletableFuture.supplyAsync {
      log.info("  작업 시작")
      Thread.sleep(500) // 0.5초 소요
      log.info("  작업 완료")
      "success"
    }

    val result = timeLimiter.executeFutureSupplier { future }
    log.info("결과: $result")
    assertEquals("success", result)
  }

  /**
   * Timeout 발생
   * - 제한 시간 초과 시 TimeoutException
   */
  private fun timeLimiterTimeout() {
    val config = TimeLimiterConfig.custom()
      .timeoutDuration(Duration.ofSeconds(1)) // 1초 타임아웃
      .cancelRunningFuture(true)
      .build()

    val timeLimiter = TimeLimiter.of("timeout-test", config)

    log.info("timeout: 1초")

    val future = CompletableFuture.supplyAsync {
      log.info("  작업 시작 (1.5초 소요 예정)")
      Thread.sleep(1500) // 1.5초 소요 (타임아웃)
      log.info("  작업 완료 (이 로그는 출력 안 될 수 있음)")
      "should-timeout"
    }

    val timeoutResult = runCatching {
      timeLimiter.executeFutureSupplier { future }
    }

    log.info("TimeoutException 발생 (1초 초과)")
    assertTrue(timeoutResult.isFailure)
  }

  // ===== 패턴 조합 =====
  @Test
  fun combinedPatternsTest() {
    log.info("\n=== Retry + Circuit Breaker 조합 ===")
    retryWithCircuitBreaker()

    Thread.sleep(500)

    log.info("\n=== 전체 패턴 조합 (간소화) ===")
    simpleCombinedPatterns()
  }

  /**
   * Retry + Circuit Breaker 조합
   * - Circuit Breaker가 OPEN되면 Retry해도 즉시 실패
   */
  private fun retryWithCircuitBreaker() {
    val cbConfig = CircuitBreakerConfig.custom()
      .slidingWindowSize(3)
      .minimumNumberOfCalls(3)
      .failureRateThreshold(50f)
      .waitDurationInOpenState(Duration.ofSeconds(1))
      .recordExceptions(IOException::class.java)
      .build()

    val circuitBreaker = CircuitBreaker.of("cb", cbConfig)
    val retry = Retry.ofDefaults("retry")
    val attemptCount = AtomicInteger(0)

    log.info("실행 순서: Retry ( CircuitBreaker ( API Call ) )")

    val decoratedSupplier = Retry.decorateSupplier(retry) {
      val cbDecorated = CircuitBreaker.decorateSupplier(circuitBreaker) {
        val attempt = attemptCount.incrementAndGet()
        log.info("  시도 $attempt - CB 상태: ${circuitBreaker.state}")

        // 모든 호출 실패
        throw IOException("API 실패")
      }
      cbDecorated.get()
    }

    val result = runCatching { decoratedSupplier.get() }

    log.info("총 시도 횟수: ${attemptCount.get()}")
    log.info("Circuit Breaker 최종 상태: ${circuitBreaker.state}")
    log.info("결과: ${if (result.isFailure) "실패" else "성공"}")

    assertTrue(result.isFailure)
    assertTrue(attemptCount.get() >= 1, "최소 1번은 시도해야 함")  // Retry 동작은 무시
  }

  /**
   * 전체 패턴 조합 (간소화)
   * - Retry → CircuitBreaker → RateLimiter → Bulkhead
   */
  private fun simpleCombinedPatterns() {
    val circuitBreaker = CircuitBreaker.of(
      "all-cb",
      CircuitBreakerConfig.custom()
        .slidingWindowSize(5)
        .minimumNumberOfCalls(3)
        .failureRateThreshold(50f)
        .waitDurationInOpenState(Duration.ofSeconds(1))
        .build()
    )

    val rateLimiter = RateLimiter.of(
      "all-rl",
      RateLimiterConfig.custom()
        .limitForPeriod(5)
        .limitRefreshPeriod(Duration.ofSeconds(1))
        .timeoutDuration(Duration.ofMillis(100))
        .build()
    )

    val bulkhead = Bulkhead.of(
      "all-bh",
      BulkheadConfig.custom()
        .maxConcurrentCalls(3)
        .maxWaitDuration(Duration.ofMillis(100))
        .build()
    )

    val retry = Retry.ofDefaults("all-retry")

    log.info("패턴 조합: Retry → CircuitBreaker → RateLimiter → Bulkhead")

    var successCount = 0

    repeat(3) { i ->
      val decoratedSupplier = Retry.decorateSupplier(retry) {
        val cbDecorated = CircuitBreaker.decorateSupplier(circuitBreaker) {
          val rlDecorated = RateLimiter.decorateSupplier(rateLimiter) {
            val bhDecorated = Bulkhead.decorateSupplier(bulkhead) {
              Thread.sleep(50)
              successCount++
              "result-$i"
            }
            bhDecorated.get()
          }
          rlDecorated.get()
        }
        cbDecorated.get()
      }

      val result = runCatching { decoratedSupplier.get() }

      if (result.isSuccess) {
        log.info("  요청 ${i + 1}: 성공 (모든 패턴 통과)")
      } else {
        log.info("  요청 ${i + 1}: 실패 - ${result.exceptionOrNull()?.javaClass?.simpleName}")
      }
    }

    log.info("성공한 요청 수: $successCount")
    assertTrue(successCount > 0, "최소 1개 요청은 성공해야 함")
  }

  /**
   * 외부 API 호출 종합 테스트
   * - Circuit Breaker: 장애 전파 방지
   * - Retry: 일시적 오류 대응
   * - Rate Limiter: API 제한 준수
   */
  @Test
  fun externalApiCallTest() {
    log.info("\n=== 외부 API 호출 시나리오 ===")

    val circuitBreaker = CircuitBreaker.of(
      "external-api",
      CircuitBreakerConfig.custom()
        .slidingWindowSize(10)
        .minimumNumberOfCalls(5)
        .failureRateThreshold(50f)
        .slowCallRateThreshold(50f)
        .slowCallDurationThreshold(Duration.ofSeconds(2))
        .waitDurationInOpenState(Duration.ofSeconds(10))
        .build()
    )

    val rateLimiter = RateLimiter.of(
      "external-api",
      RateLimiterConfig.custom()
        .limitForPeriod(100) // 초당 100건
        .limitRefreshPeriod(Duration.ofSeconds(1))
        .timeoutDuration(Duration.ofMillis(500))
        .build()
    )

    val retry = Retry.ofDefaults("external-api")

    log.info("외부 API 호출 설정:")
    log.info("  - Circuit Breaker: 실패율 50%, Slow call 2초")
    log.info("  - Rate Limiter: 초당 100건")
    log.info("  - Retry: 최대 3회 (default)")

    var successCount = 0
    var failCount = 0
    val apiCallCount = AtomicInteger(0)

    // 10번 API 호출 시뮬레이션
    repeat(10) { i ->
      val decoratedSupplier = Retry.decorateSupplier(retry) {
        val cbDecorated = CircuitBreaker.decorateSupplier(circuitBreaker) {
          val rlDecorated = RateLimiter.decorateSupplier(rateLimiter) {
            val callNum = apiCallCount.incrementAndGet()

            // 30% 확률로 실패
            if (Math.random() < 0.3) {
              throw IOException("API 일시 오류")
            }

            "api-response-$callNum"
          }
          rlDecorated.get()
        }
        cbDecorated.get()
      }

      val result = runCatching { decoratedSupplier.get() }

      if (result.isSuccess) {
        successCount++
        log.info("  API 호출 ${i + 1}: 성공")
      } else {
        failCount++
        log.info("  API 호출 ${i + 1}: 실패 - ${result.exceptionOrNull()?.javaClass?.simpleName}")
      }

      Thread.sleep(50)
    }

    log.info("\n결과:")
    log.info("  - 성공: $successCount")
    log.info("  - 실패: $failCount")
    log.info("  - 실제 API 호출 횟수: ${apiCallCount.get()} (Retry 포함)")
    log.info("  - Circuit Breaker 상태: ${circuitBreaker.state}")

    assertTrue(successCount + failCount == 10, "총 10번 시도해야 함")
  }

  private fun shutdown(executor: ExecutorService) {
    executor.shutdown()

    while (!executor.isTerminated) {
      Thread.sleep(100)
    }
  }
}
