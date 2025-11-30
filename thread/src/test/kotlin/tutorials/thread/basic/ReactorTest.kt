package tutorials.thread.basic

import org.apache.commons.logging.LogFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import reactor.core.publisher.BufferOverflowStrategy
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.test.StepVerifier
import reactor.util.context.Context
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

class ReactorTest {

  val log = LogFactory.getLog(javaClass)

  // ===== Mono와 Flux 기본 =====
  @Test
  fun monoFluxBasicTest() {
    log.info("=== 1. Mono 기본 동작 ===")
    monoBasic()

    Thread.sleep(300)

    log.info("=== 2. Flux 기본 동작 ===")
    fluxBasic()

    Thread.sleep(300)

    log.info("=== 3. 구독 없이는 실행 안 됨 ===")
    noSubscribeNoExecution()
  }

  /**
   * Mono 기본 동작
   * - 0 또는 1개의 아이템
   */
  private fun monoBasic() {
    log.info("Mono는 0 또는 1개의 아이템을 emit")

    // 1개 아이템
    val mono = Mono.just("Hello")
    StepVerifier.create(mono)
      .expectNext("Hello")
      .verifyComplete()
    log.info("  Mono.just('Hello'): 성공")

    // 빈 Mono
    val empty = Mono.empty<String>()
    StepVerifier.create(empty)
      .verifyComplete()
    log.info("  Mono.empty(): 성공 (아이템 없음)")

    // 에러 Mono
    val error = Mono.error<String>(RuntimeException("Error"))
    StepVerifier.create(error)
      .expectError(RuntimeException::class.java)
      .verify()
    log.info("  Mono.error(): 성공 (에러 emit)")
  }

  /**
   * Flux 기본 동작
   * - 0~N개의 아이템
   */
  private fun fluxBasic() {
    log.info("Flux는 0~N개의 아이템을 emit")

    // 여러 아이템
    val flux = Flux.just("A", "B", "C")
    StepVerifier.create(flux)
      .expectNext("A")
      .expectNext("B")
      .expectNext("C")
      .verifyComplete()
    log.info("  Flux.just('A', 'B', 'C'): 성공")

    // range
    val range = Flux.range(1, 5)
    StepVerifier.create(range)
      .expectNext(1, 2, 3, 4, 5)
      .verifyComplete()
    log.info("  Flux.range(1, 5): 성공 (1~5 emit)")

    // fromIterable
    val list = Flux.fromIterable(listOf("A", "B", "C"))
    StepVerifier.create(list)
      .expectNext("A", "B", "C")
      .verifyComplete()
    log.info("  Flux.fromIterable(): 성공")
  }

  /**
   * 구독 없이는 실행 안 됨
   * - Publisher는 구독되기 전까지 아무것도 하지 않음
   */
  private fun noSubscribeNoExecution() {
    val executionCount = AtomicInteger(0)

    log.info("구독하지 않으면 실행되지 않음")

    // 구독 안 함
    val mono = Mono.fromCallable {
      executionCount.incrementAndGet()
      "result"
    }
    Thread.sleep(100)
    log.info("  구독 전: executionCount = ${executionCount.get()}")
    assertEquals(0, executionCount.get(), "구독하지 않으면 실행 안 됨")

    // 구독함
    mono.subscribe()
    Thread.sleep(100)
    log.info("  구독 후: executionCount = ${executionCount.get()}")
    assertEquals(1, executionCount.get(), "구독하면 실행됨")
  }

  // ===== 주요 연산자 =====
  @Test
  fun operatorsTest() {
    log.info("=== 1. map - 동기 변환 ===")
    mapOperator()

    Thread.sleep(300)

    log.info("=== 2. flatMap - 비동기 변환 ===")
    flatMapOperator()

    Thread.sleep(300)

    log.info("=== 3. filter - 필터링 ===")
    filterOperator()
  }

  /**
   * map - 동기 변환
   * - T -> U
   */
  private fun mapOperator() {
    log.info("map은 동기 변환 (T -> U)")

    val mono = Mono.just("hello")
      .map(String::uppercase)
      .map { s -> "$s WORLD" }

    StepVerifier.create(mono)
      .expectNext("HELLO WORLD")
      .verifyComplete()

    log.info("  Mono.just('hello').map(uppercase).map(concat): 'HELLO WORLD'")
  }

  /**
   * flatMap - 비동기 변환
   * - T -> Mono<U> or Flux<U>
   */
  private fun flatMapOperator() {
    log.info("flatMap은 비동기 변환 (T -> Mono<U>)")

    val mono = Mono.just(1L)
      .flatMap { id ->
        // 비동기 작업 시뮬레이션
        Mono.just("User-$id")
          .delayElement(Duration.ofMillis(100))
      }

    StepVerifier.create(mono)
      .expectNext("User-1")
      .verifyComplete()

    log.info("  flatMap으로 비동기 변환 성공")

    // Flux의 flatMap
    val flux = Flux.just(1, 2, 3)
      .flatMap { num ->
        Flux.just("$num-A", "$num-B")
      }

    StepVerifier.create(flux)
      .expectNextCount(6) // 1-A, 1-B, 2-A, 2-B, 3-A, 3-B
      .verifyComplete()

    log.info("  Flux.flatMap으로 1:N 변환 성공")
  }

  /**
   * filter - 필터링
   */
  private fun filterOperator() {
    log.info("filter는 조건에 맞는 아이템만 통과")

    val flux = Flux.range(1, 10)
      .filter { n -> n % 2 == 0 }

    StepVerifier.create(flux)
      .expectNext(2, 4, 6, 8, 10)
      .verifyComplete()

    log.info("  Flux.range(1, 10).filter(짝수): 2, 4, 6, 8, 10")
  }

  // ===== 결합 연산자 =====
  @Test
  fun combineOperatorsTest() {
    log.info("=== 1. zip - 결합 ===")
    zipOperator()

    Thread.sleep(300)

    log.info("=== 2. merge - 병합 (순서 보장 안 됨) ===")
    mergeOperator()

    Thread.sleep(300)

    log.info("=== 3. concat - 순차 연결 (순서 보장) ===")
    concatOperator()
  }

  /**
   * zip - 여러 Publisher를 결합
   * - 모든 Publisher의 아이템이 준비될 때까지 대기
   */
  private fun zipOperator() {
    log.info("zip은 여러 Publisher를 결합")

    val mono1 = Mono.just("John")
    val mono2 = Mono.just("Doe")

    val result = Mono.zip(mono1, mono2)
      .map { tuple -> "${tuple.t1} ${tuple.t2}" }

    StepVerifier.create(result)
      .expectNext("John Doe")
      .verifyComplete()

    log.info("  Mono.zip(firstName, lastName): 'John Doe'")

    // 3개 결합
    val mono3 = Mono.just(30)
    val combined = Mono.zip(mono1, mono2, mono3)
      .map { tuple -> "${tuple.t1} ${tuple.t2}, ${tuple.t3}" }

    StepVerifier.create(combined)
      .expectNext("John Doe, 30")
      .verifyComplete()

    log.info("  3개 Mono 결합: 'John Doe, 30'")
  }

  /**
   * merge - 여러 Publisher를 병합
   * - 순서 보장 안 됨 (먼저 emit되는 대로)
   */
  private fun mergeOperator() {
    log.info("merge는 병합 (순서 보장 안 됨)")

    val flux1 = Flux.just("A", "B").delayElements(Duration.ofMillis(100))
    val flux2 = Flux.just("C", "D").delayElements(Duration.ofMillis(50))

    val merged = Flux.merge(flux1, flux2)

    // 순서는 보장 안 되지만 모든 아이템은 emit됨
    StepVerifier.create(merged)
      .expectNextCount(4)
      .verifyComplete()

    log.info("  Flux.merge: 4개 아이템 emit (순서는 delayElements에 따라 다름)")
  }

  /**
   * concat - 여러 Publisher를 순차 연결
   * - 순서 보장됨 (첫 번째가 complete된 후 두 번째 시작)
   */
  private fun concatOperator() {
    log.info("concat은 순차 연결 (순서 보장)")

    val flux1 = Flux.just("A", "B")
    val flux2 = Flux.just("C", "D")

    val concatenated = Flux.concat(flux1, flux2)

    StepVerifier.create(concatenated)
      .expectNext("A", "B", "C", "D")
      .verifyComplete()

    log.info("  Flux.concat: A, B, C, D (순서 보장)")
  }

  // ===== 부수 효과 연산자 =====
  @Test
  fun sideEffectOperatorsTest() {
    log.info("=== 1. doOnNext, doOnError, doOnComplete ===")
    doOnOperators()

    Thread.sleep(300)

    log.info("=== 2. doOnSubscribe, doFinally ===")
    doOnLifecycleOperators()
  }

  /**
   * doOnNext, doOnError, doOnComplete
   * - 부수 효과 (로깅, 모니터링 등)
   */
  private fun doOnOperators() {
    val events = mutableListOf<String>()

    log.info("doOnXxx는 부수 효과를 위한 연산자")

    val mono = Mono.just("Hello")
      .doOnNext { value ->
        events.add("onNext: $value")
        log.info("  doOnNext: $value")
      }
      .doOnSuccess { value ->
        events.add("onSuccess: $value")
        log.info("  doOnSuccess: $value")
      }
      .doOnError { error ->
        events.add("onError: ${error.message}")
        log.info("  doOnError: ${error.message}")
      }

    StepVerifier.create(mono)
      .expectNext("Hello")
      .verifyComplete()

    assertTrue(events.contains("onNext: Hello"))
    assertTrue(events.contains("onSuccess: Hello"))
    log.info("  부수 효과 연산자 동작 확인")
  }

  /**
   * doOnSubscribe, doFinally
   * - 구독 시작과 종료 시점 처리
   */
  private fun doOnLifecycleOperators() {
    val events = mutableListOf<String>()

    log.info("doOnSubscribe, doFinally는 lifecycle 이벤트 처리")

    val mono = Mono.just("Data")
      .doOnSubscribe {
        events.add("subscribe")
        log.info("  doOnSubscribe: 구독 시작")
      }
      .doFinally { signalType ->
        events.add("finally: $signalType")
        log.info("  doFinally: $signalType")
      }

    StepVerifier.create(mono)
      .expectNext("Data")
      .verifyComplete()

    assertTrue(events.contains("subscribe"))
    assertTrue(events[events.size - 1].startsWith("finally:"))
    log.info("  Lifecycle 이벤트 처리 확인 완료")
  }

  // ===== 에러 처리 및 재시도 =====
  @Test
  fun errorHandlingTest() {
    log.info("=== 1. onErrorReturn - 기본값 반환 ===")
    onErrorReturnTest()

    Thread.sleep(300)

    log.info("=== 2. onErrorReturn - 특정 예외 타입 처리 ===")
    onErrorReturnWithExceptionTypeTest()

    Thread.sleep(300)

    log.info("=== 3. onErrorReturn - Eager Evaluation 문제 ===")
    onErrorReturnEagerEvaluationTest()

    Thread.sleep(300)

    log.info("=== 4. onErrorResume - 대체 Publisher ===")
    onErrorResumeTest()

    Thread.sleep(300)

    log.info("=== 5. retry - 재시도 ===")
    retryTest()

    Thread.sleep(300)

    log.info("=== 6. timeout - 타임아웃 ===")
    timeoutTest()
  }

  /**
   * onErrorReturn - 에러 발생 시 기본값 반환
   */
  private fun onErrorReturnTest() {
    log.info("onErrorReturn은 에러 시 기본값 반환")

    val mono = Mono.error<String>(RuntimeException("Error"))
      .onErrorReturn("Fallback value")

    StepVerifier.create(mono)
      .expectNext("Fallback value")
      .verifyComplete()

    log.info("  에러 발생 -> 'Fallback value' 반환")
  }

  /**
   * onErrorReturn - 특정 예외 타입에만 fallback 적용
   * - Exception 클래스를 지정하여 특정 에러만 처리 가능
   */
  private fun onErrorReturnWithExceptionTypeTest() {
    log.info("특정 예외 타입에만 fallback 적용")

    val mono = Mono.error<String>(IllegalArgumentException("Invalid"))
      .onErrorReturn(IllegalArgumentException::class.java, "IllegalArgument Fallback")
      .onErrorReturn(RuntimeException::class.java, "Runtime Fallback")

    StepVerifier.create(mono)
      .expectNext("IllegalArgument Fallback")
      .verifyComplete()

    log.info("  첫 번째 매칭되는 예외 타입의 fallback 반환")

    log.info("매칭되지 않는 예외는 그대로 전파")

    val mono2 = Mono.error<String>(IllegalStateException("Invalid State"))
      .onErrorReturn(IllegalArgumentException::class.java, "IllegalArgument Fallback")

    StepVerifier.create(mono2)
      .expectError(IllegalStateException::class.java)
      .verify()

    log.info("  IllegalStateException은 매칭 안 되어 에러 전파")
  }

  /**
   * onErrorReturn - Eager Evaluation 문제
   * - 파라미터로 전달된 값은 에러 발생 여부와 관계없이 즉시 평가됨
   */
  private fun onErrorReturnEagerEvaluationTest() {
    log.info("onErrorReturn은 파라미터를 즉시 평가함 (Eager Evaluation)")

    log.info("  정상 케이스에서도 fallback 생성 코드가 실행됨")

    val executionCount = AtomicInteger(0)
    val successMono = Mono.just("Success")
      .onErrorReturn(
        run {
          val count = executionCount.incrementAndGet()
          log.info("    fallback 값 생성 실행됨 (executionCount: $count)")
          "Fallback-$count"
        }
      )

    StepVerifier.create(successMono)
      .expectNext("Success")
      .verifyComplete()

    log.info("  결과: 정상 케이스인데도 fallback 생성 코드가 실행됨 (executionCount: ${executionCount.get()})")
    assertEquals(1, executionCount.get(), "에러가 없어도 onErrorReturn의 파라미터는 평가됨")

    log.info("  [해결책] onErrorResume으로 lazy evaluation")
  }

  /**
   * onErrorResume - 에러 발생 시 대체 Publisher
   */
  private fun onErrorResumeTest() {
    log.info("onErrorResume은 에러 시 대체 Publisher 반환")

    // 1. 기본 사용 - 모든 에러를 대체 Publisher로 변환
    val mono1 = Mono.error<String>(RuntimeException("Error"))
      .onErrorResume { error ->
        log.info("  에러 발생: ${error.message}, 대체 Mono 반환")
        Mono.just("Alternative value")
      }

    StepVerifier.create(mono1)
      .expectNext("Alternative value")
      .verifyComplete()

    log.info("  대체 Publisher로 복구 성공")

    // 2. 예외 타입별 다른 처리
    log.info("  예외 타입별 다른 처리")

    val mono2 = Mono.error<String>(IllegalStateException("Invalid State"))
      .onErrorResume { error ->
        when (error) {
          is IllegalArgumentException -> {
            log.info("  IllegalArgumentException")
            Mono.just("From Argument")
          }

          is IllegalStateException -> {
            log.info("  IllegalStateException")
            Mono.just("From State")
          }

          else -> {
            log.info("  기타 예외 -> 기본값")
            Mono.just("Default")
          }
        }
      }

    StepVerifier.create(mono2)
      .expectNext("From State")
      .verifyComplete()

    log.info("  예외 타입에 따라 다른 복구 전략 적용")
  }

  /**
   * retry - 재시도
   */
  private fun retryTest() {
    val attemptCount = AtomicInteger(0)

    log.info("retry는 에러 시 자동 재시도")

    val mono = Mono.fromCallable {
      val attempt = attemptCount.incrementAndGet()
      log.info("  시도 $attempt")
      if (attempt < 3) {
        throw RuntimeException("Attempt $attempt failed")
      }
      "Success"
    }.retry(2) // 최대 2번 재시도 (총 3번 시도)

    StepVerifier.create(mono)
      .expectNext("Success")
      .verifyComplete()

    log.info("  총 ${attemptCount.get()}번 시도 후 성공")
    assertEquals(3, attemptCount.get())
  }

  /**
   * timeout - 타임아웃
   */
  private fun timeoutTest() {
    log.info("timeout은 시간 제한")

    // 타임아웃 발생
    val slow = Mono.delay(Duration.ofSeconds(2))
      .timeout(Duration.ofMillis(500))
      .onErrorReturn(-1L)

    StepVerifier.create(slow)
      .expectNext(-1L)
      .verifyComplete()

    log.info("  2초 작업에 500ms 타임아웃 -> fallback 값 반환")
  }

  // ===== Scheduler =====
  @Test
  fun schedulerTest() {
    log.info("=== 1. subscribeOn - 구독 Thread 지정 ===")
    subscribeOnTest()

    Thread.sleep(500)

    log.info("=== 2. publishOn - 이후 연산자 Thread 지정 ===")
    publishOnTest()

    Thread.sleep(500)

    log.info("=== 3. Blocking 코드 격리 ===")
    blockingCodeIsolationTest()
  }

  /**
   * subscribeOn - 구독 실행 Thread 지정
   */
  private fun subscribeOnTest() {
    val threads = mutableListOf<String>()

    log.info("subscribeOn은 구독 실행 Thread 지정")

    val mono = Mono.fromCallable {
      val threadName = Thread.currentThread().name
      threads.add("source: $threadName")
      log.info("  구독 실행: $threadName")
      "data"
    }
      .subscribeOn(Schedulers.boundedElastic())
      .map { value ->
        val threadName = Thread.currentThread().name
        threads.add("map: $threadName")
        log.info("  map 실행: $threadName")
        value.uppercase()
      }

    mono.block() // 완료 대기

    assertTrue(threads.size >= 2, "threads 크기: ${threads.size}")
    // JDK 21에서는 boundedElastic이 Virtual Thread를 사용하여 loomBoundedElastic으로 표시됨
    assertTrue(
      threads[0].contains("boundedElastic") || threads[0].contains("loomBoundedElastic"),
      "threads[0]: ${threads[0]}"
    )
    assertTrue(
      threads[1].contains("boundedElastic") || threads[1].contains("loomBoundedElastic"),
      "threads[1]: ${threads[1]}"
    )
    log.info("  소스와 map 모두 boundedElastic에서 실행 확인")
  }

  /**
   * publishOn - 이후 연산자 실행 Thread 지정
   */
  private fun publishOnTest() {
    val threads = mutableListOf<String>()

    log.info("publishOn은 이후 연산자 Thread 지정")

    val mono = Mono.just("data")
      .map { value ->
        val threadName = Thread.currentThread().name
        threads.add("map1: $threadName")
        log.info("  map1 실행: $threadName")
        value
      }
      .publishOn(Schedulers.parallel())
      .map { value ->
        val threadName = Thread.currentThread().name
        threads.add("map2: $threadName")
        log.info("  map2 실행 (publishOn 이후): $threadName")
        value
      }

    mono.block() // 완료 대기

    assertTrue(threads.size >= 2, "threads 크기: ${threads.size}")
    assertTrue(threads[1].contains("parallel"), "threads[1]: ${threads[1]}")
    log.info("  publishOn 이후의 연산자는 parallel에서 실행 확인")
  }

  /**
   * Blocking 코드 격리
   * - boundedElastic에서 실행해야 함
   */
  private fun blockingCodeIsolationTest() {
    log.info("Blocking 코드는 boundedElastic에서 격리")

    val mono = Mono.fromCallable {
      log.info("  Blocking 작업 시작 (${Thread.currentThread().name})")
      Thread.sleep(100) // Blocking 작업
      log.info("  Blocking 작업 완료")
      "result"
    }.subscribeOn(Schedulers.boundedElastic())

    StepVerifier.create(mono)
      .expectNext("result")
      .verifyComplete()

    log.info("  boundedElastic에서 Blocking 코드 실행 완료")
  }

  // ===== Cold vs Hot Stream =====
  @Test
  fun coldVsHotStreamTest() {
    log.info("=== 1. Cold Stream - 구독마다 새로 시작 ===")
    coldStreamTest()

    Thread.sleep(500)

    log.info("=== 2. Hot Stream - autoConnect ===")
    hotStreamTest()

    Thread.sleep(500)

    log.info("=== 3. Hot Stream - connect (수동 시작) ===")
    hotStreamConnectTest()

    Thread.sleep(500)

    log.info("=== 4. Hot Stream - refCount (자동 중단) ===")
    hotStreamRefCountTest()
  }

  /**
   * Cold Stream
   * - 구독할 때마다 처음부터 새로 시작
   */
  private fun coldStreamTest() {
    log.info("Cold Stream은 구독마다 독립적으로 실행")

    val cold = Flux.range(1, 3)

    val subscriber1Results = mutableListOf<Int>()
    val subscriber2Results = mutableListOf<Int>()

    // 첫 번째 구독자
    cold.subscribe { value ->
      subscriber1Results.add(value)
      log.info("  구독자1: $value")
    }

    Thread.sleep(100)

    // 두 번째 구독자 (처음부터 다시 시작)
    cold.subscribe { value ->
      subscriber2Results.add(value)
      log.info("  구독자2: $value")
    }

    Thread.sleep(100)

    assertEquals(listOf(1, 2, 3), subscriber1Results)
    assertEquals(listOf(1, 2, 3), subscriber2Results)
    log.info("  두 구독자 모두 독립적으로 1, 2, 3 수신")
  }

  /**
   * Hot Stream
   * - 모든 구독자가 같은 스트림 공유
   */
  private fun hotStreamTest() {
    log.info("Hot Stream은 모든 구독자가 공유")

    val hot = Flux.interval(Duration.ofMillis(100))
      .take(10)
      .publish() // ConnectableFlux로 변환
      .autoConnect(1) // 첫 번째 구독자가 연결되면 자동 시작

    val subscriber1Results = mutableListOf<Long>()
    val subscriber2Results = mutableListOf<Long>()

    // 첫 번째 구독자 (즉시 시작)
    hot.subscribe { value ->
      subscriber1Results.add(value)
      log.info("  구독자1: $value")
    }

    // 300ms 후 두 번째 구독자 (중간부터 수신)
    Thread.sleep(350)

    hot.subscribe { value ->
      subscriber2Results.add(value)
      log.info("  구독자2: $value (중간부터)")
    }

    Thread.sleep(800)

    log.info("  구독자1: ${subscriber1Results.size}개, 구독자2: ${subscriber2Results.size}개")
    assertTrue(
      subscriber1Results.size > subscriber2Results.size,
      "구독자1(${subscriber1Results.size})이 구독자2(${subscriber2Results.size})보다 많이 수신해야 함"
    )
    log.info("  구독자2는 나중에 구독했으므로 적게 수신")
  }

  /**
   * Hot Stream - connect() 수동 시작
   * - 모든 구독자를 먼저 등록한 후 수동으로 시작
   */
  private fun hotStreamConnectTest() {
    log.info("connect()는 모든 구독자를 먼저 등록 후 수동 시작")

    val hot = Flux.interval(Duration.ofMillis(100))
      .take(5)
      .publish() // ConnectableFlux (autoConnect 없음!)

    val subscriber1Results = mutableListOf<Long>()
    val subscriber2Results = mutableListOf<Long>()
    val subscriber3Results = mutableListOf<Long>()

    // 모든 구독자를 먼저 등록 (아직 시작 안 됨)
    hot.subscribe { value ->
      subscriber1Results.add(value)
      log.info("  구독자1: $value")
    }

    hot.subscribe { value ->
      subscriber2Results.add(value)
      log.info("  구독자2: $value")
    }

    hot.subscribe { value ->
      subscriber3Results.add(value)
      log.info("  구독자3: $value")
    }

    log.info("  모든 구독자 등록 완료, 아직 데이터 발행 안 됨")
    Thread.sleep(200) // 대기해도 데이터 없음

    assertEquals(0, subscriber1Results.size, "connect() 전에는 데이터 없음")
    assertEquals(0, subscriber2Results.size, "connect() 전에는 데이터 없음")
    assertEquals(0, subscriber3Results.size, "connect() 전에는 데이터 없음")

    log.info("  이제 connect() 호출 - 모든 구독자가 동시에 수신 시작")
    hot.connect()

    Thread.sleep(600)

    log.info("  구독자1: ${subscriber1Results.size}개")
    log.info("  구독자2: ${subscriber2Results.size}개")
    log.info("  구독자3: ${subscriber3Results.size}개")

    assertEquals(subscriber1Results, subscriber2Results, "모든 구독자가 동일한 데이터 수신")
    assertEquals(subscriber2Results, subscriber3Results, "모든 구독자가 동일한 데이터 수신")
    log.info("  모든 구독자가 공정하게 동시 시작")
  }

  /**
   * Hot Stream - refCount() 자동 관리
   * - 구독자 수에 따라 자동 시작/중단
   */
  private fun hotStreamRefCountTest() {
    log.info("refCount()는 구독자 수에 따라 자동 시작/중단")

    val hot = Flux.interval(Duration.ofMillis(100))
      .take(10)
      .doOnSubscribe { log.info("  [upstream] 시작됨!") }
      .doOnCancel { log.info("  [upstream] 중단됨!") }
      .publish()
      .refCount(1) // 1명 이상이면 시작, 0명이면 중단

    val subscriber1Results = mutableListOf<Long>()
    val subscriber2Results = mutableListOf<Long>()

    log.info("첫 번째 구독자 등록 -> upstream 시작")
    val disposable1 = hot.subscribe { value ->
      subscriber1Results.add(value)
      log.info("  구독자1: $value")
    }

    Thread.sleep(300)

    log.info("두 번째 구독자 등록 (중간부터 수신)")
    val disposable2 = hot.subscribe { value ->
      subscriber2Results.add(value)
      log.info("  구독자2: $value")
    }

    Thread.sleep(300)

    log.info("첫 번째 구독자 해제")
    disposable1.dispose()
    log.info("  구독자2만 남았으므로 계속 실행")

    Thread.sleep(200)

    log.info("두 번째 구독자도 해제 -> upstream 자동 중단")
    disposable2.dispose()

    Thread.sleep(200)

    log.info("  구독자1: ${subscriber1Results.size}개 수신")
    log.info("  구독자2: ${subscriber2Results.size}개 수신")
    assertTrue(
      subscriber1Results.size > subscriber2Results.size,
      "구독자1이 더 많이 수신 (먼저 구독)"
    )

    log.info("다시 구독 -> 처음부터 재시작")
    val subscriber3Results = mutableListOf<Long>()

    hot.subscribe { value ->
      subscriber3Results.add(value)
      log.info("  구독자3: $value (새로 시작)")
    }

    Thread.sleep(400)

    log.info("  구독자3: ${subscriber3Results.size}개 수신 (처음부터 재시작됨)")
    assertEquals(0L, subscriber3Results.firstOrNull(), "refCount는 재시작 시 0부터 시작")
  }

  // ===== Context 전파 =====
  @Test
  fun contextPropagationTest() {
    log.info("=== Context 전파 (ThreadLocal 대체) ===")
    contextTest()
  }

  /**
   * Context 전파
   * - ThreadLocal 대신 Context 사용
   */
  private fun contextTest() {
    log.info("Reactive에서는 Context로 데이터 전파")

    val mono = Mono.deferContextual { ctx ->
      val userId = ctx.get<String>("userId")
      log.info("  Context에서 userId 읽기: $userId")
      Mono.just("User data for $userId")
    }.contextWrite(Context.of("userId", "user123"))

    StepVerifier.create(mono)
      .expectNext("User data for user123")
      .verifyComplete()

    log.info("  Context 전파 성공")

    // Context는 불변이므로 chain에서 수정 가능
    val modified = Mono.deferContextual { ctx ->
      val userId = ctx.get<String>("userId")
      val role = ctx.get<String>("role")
      log.info("  userId: $userId, role: $role")
      Mono.just("$userId-$role")
    }
      .contextWrite(Context.of("role", "admin"))
      .contextWrite(Context.of("userId", "user456"))

    StepVerifier.create(modified)
      .expectNext("user456-admin")
      .verifyComplete()

    log.info("  여러 Context 값 전파 성공")
  }

  // ===== Backpressure =====
  @Test
  fun backpressureTest() {
    log.info("=== 1. onBackpressureDrop - 드롭 ===")
    backpressureDropTest()

    Thread.sleep(300)

    log.info("=== 2. onBackpressureBuffer - 버퍼 전략 ===")
    backpressureBufferTest()

    Thread.sleep(300)

    log.info("=== 3. onBackpressureLatest - 최신 유지 ===")
    backpressureLatestTest()
  }

  /**
   * onBackpressureDrop
   * - 처리 못하는 아이템 드롭
   */
  private fun backpressureDropTest() {
    val dropped = mutableListOf<Int>()

    log.info("onBackpressureDrop은 처리 못하는 아이템 드롭")

    val flux = Flux.range(1, 10)
      .onBackpressureDrop { value ->
        dropped.add(value)
        log.info("  드롭: $value")
      }
      .delayElements(Duration.ofMillis(10))

    val received = mutableListOf<Int>()
    flux.take(10).subscribe { value ->
      received.add(value)
    }

    Thread.sleep(200)

    log.info("  수신: ${received.size}개, 드롭: ${dropped.size}개")
  }

  /**
   * onBackpressureBuffer - 버퍼 오버플로우 전략
   * - ERROR (기본), DROP_OLDEST, DROP_LATEST
   */
  private fun backpressureBufferTest() {
    // 1. 충분한 버퍼 크기 - 정상 동작
    log.info("1) 충분한 버퍼 크기 - 모든 아이템 버퍼링")

    val results = Flux.range(1, 10)
      .onBackpressureBuffer(15) // 충분한 버퍼 크기
      .delayElements(Duration.ofMillis(50))
      .doOnNext { log.info("  수신: $it") }
      .collectList()
      .block()

    log.info("  수신 완료: ${results?.size}개")
    assertEquals(10, results?.size)
    log.info("  모든 아이템 버퍼링 후 수신 완료")

    Thread.sleep(100)

    // 2. ERROR 전략 (기본) - 버퍼 부족 시 OverflowException
    log.info("2) ERROR 전략 (기본) - 버퍼 부족 시 OverflowException")

    val flux = Flux.range(1, 100)
      .onBackpressureBuffer(10)
      .delayElements(Duration.ofMillis(100))
      .doOnNext { log.info("  수신: $it") }

    StepVerifier.create(flux)
      .expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
      .expectErrorMatches { error ->
        // @see reactor.core.IllegalStateException
        // new OverflowException("The receiver is overrun by more signals than expected (bounded queue...)")
        error.message?.contains("overrun") == true
      }
      .verify()

    log.info("  11번째 아이템부터 OverflowException 발생 (데이터 손실 불가)")

    Thread.sleep(100)

    // 3. DROP_OLDEST 전략 - 가장 오래된 아이템 버림
    log.info("3) DROP_OLDEST 전략 - 가장 오래된 아이템 버림")

    val droppedOldest = mutableListOf<Int>()

    // 빠른 발행 속도 (interval) + 느린 소비 속도 (delayElements)
    val fluxOldest = Flux.interval(Duration.ofMillis(10))
      .take(50)
      .map { it.toInt() + 1 } // 1부터 시작
      .onBackpressureBuffer(
        5,
        { value ->
          droppedOldest.add(value)
          log.info("  버림(오래됨): $value")
        },
        BufferOverflowStrategy.DROP_OLDEST
      )
      .delayElements(Duration.ofMillis(100))
      .doOnNext { value -> log.info("  수신: $value") }

    val receivedOldest = mutableListOf<Int>()
    fluxOldest.take(10).subscribe { receivedOldest.add(it) }

    Thread.sleep(1500)

    log.info("  드롭된 아이템: ${droppedOldest.size}개")
    if (droppedOldest.isNotEmpty()) {
      log.info("    드롭된 값 (처음 10개): ${droppedOldest.take(10)}")
      log.info("    수신된 값 (처음 5개): ${receivedOldest.take(5)}")
    }
    log.info("  최신 데이터가 중요한 경우 사용 (실시간 시세, 센서)")

    Thread.sleep(100)

    // 4. DROP_LATEST 전략 - 최신 아이템 버림
    log.info("4) DROP_LATEST 전략 - 최신 아이템 버림")

    val droppedLatest = mutableListOf<Int>()

    // 빠른 발행 속도 (interval) + 느린 소비 속도 (delayElements)
    val fluxLatest = Flux.interval(Duration.ofMillis(10))
      .take(50)
      .map { it.toInt() + 1 } // 1부터 시작
      .onBackpressureBuffer(
        5, // 작은 버퍼
        { value ->
          droppedLatest.add(value)
          log.info("  버림(최신): $value")
        },
        BufferOverflowStrategy.DROP_LATEST
      )
      .delayElements(Duration.ofMillis(100))
      .doOnNext { value -> log.info("  수신: $value") }

    val receivedLatest = mutableListOf<Int>()
    fluxLatest.take(10).subscribe { receivedLatest.add(it) }

    Thread.sleep(1500)

    log.info("  드롭된 아이템: ${droppedLatest.size}개")
    if (droppedLatest.isNotEmpty()) {
      log.info("    드롭된 값 (처음 10개): ${droppedLatest.take(10)}")
      log.info("    수신된 값 (처음 5개): ${receivedLatest.take(5)}")
    }
    log.info("  오래된 데이터가 중요한 경우 사용 (로그, 히스토리)")
  }

  /**
   * onBackpressureLatest
   * - 최신 아이템만 유지
   */
  private fun backpressureLatestTest() {
    log.info("onBackpressureLatest는 최신 아이템만 유지")

    val flux = Flux.range(1, 10)
      .onBackpressureLatest()
      .delayElements(Duration.ofMillis(50))

    val results = mutableListOf<Int>()
    flux.subscribe { value ->
      results.add(value)
      log.info("  수신: $value")
    }

    Thread.sleep(600)

    log.info("  수신 완료: ${results.size}개")
  }

}
