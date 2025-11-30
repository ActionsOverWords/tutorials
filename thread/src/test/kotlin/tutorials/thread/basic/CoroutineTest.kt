package tutorials.thread.basic

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.apache.commons.logging.LogFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineTest {

  val log = LogFactory.getLog(javaClass)

  // ===== Suspend Function 기본 =====
  @Test
  fun suspendFunctionTest() = runBlocking {
    log.info("=== 1. Suspend Function 기본 동작 ===")
    suspendFunctionBasic()

    delay(100)

    log.info("=== 2. 순차 실행 ===")
    suspendSequentialExecution()
  }

  /**
   * Suspend Function 기본 동작
   * - suspend 키워드로 선언
   * - Non-blocking delay
   */
  private suspend fun suspendFunctionBasic() {
    log.info("suspend 함수는 일시 중단 가능")

    val startThread = Thread.currentThread().name
    log.info("  시작: $startThread")

    delay(100) // Non-blocking delay

    val endThread = Thread.currentThread().name
    log.info("  종료: $endThread")
    log.info("  delay는 Thread를 블로킹하지 않음")
  }

  /**
   * 순차 실행
   * - suspend 함수는 순차적으로 실행됨
   */
  private suspend fun suspendSequentialExecution() {
    log.info("suspend 함수 내에서는 순차 실행")

    val time = measureTimeMillis {
      val result1 = fetchData(1, 100)
      log.info("  result1: $result1")

      val result2 = fetchData(2, 100)
      log.info("  result2: $result2")
    }

    log.info("  총 소요 시간: ${time}ms (약 200ms)")
    assertTrue(time >= 200, "순차 실행이므로 200ms 이상 소요")
  }

  private suspend fun fetchData(id: Int, delayMs: Long): String {
    delay(delayMs)
    return "Data-$id"
  }

  // ===== Coroutine Builders =====
  @Test
  fun coroutineBuildersTest() = runBlocking {
    log.info("=== 1. launch - Fire and Forget ===")
    launchBuilder()

    delay(300)

    log.info("=== 2. async - 결과 반환 ===")
    asyncBuilder()

    delay(300)

    log.info("=== 3. async 병렬 실행 ===")
    asyncParallel()
  }

  /**
   * launch - Fire and Forget
   * - 반환값 없음 (Job 반환)
   */
  private suspend fun launchBuilder() {
    log.info("launch는 반환값 없이 백그라운드 실행")

    val job = CoroutineScope(Dispatchers.IO).launch {
      log.info("  launch 실행: ${Thread.currentThread().name}")
      delay(100)
      log.info("  launch 완료")
    }

    job.join() // 완료 대기
    log.info("  Job 완료 확인")
  }

  /**
   * async - 결과 반환
   * - Deferred<T> 반환
   * - await()로 결과 획득
   */
  private suspend fun asyncBuilder() {
    log.info("async는 결과를 Deferred로 반환")

    val deferred = CoroutineScope(Dispatchers.IO).async {
      log.info("  async 실행: ${Thread.currentThread().name}")
      delay(100)
      "Result"
    }

    val result = deferred.await()
    log.info("  결과: $result")
    assertEquals("Result", result)
  }

  /**
   * async 병렬 실행
   * - 여러 async를 동시에 실행
   */
  private suspend fun asyncParallel() {
    log.info("async로 병렬 실행")

    val time = measureTimeMillis {
      coroutineScope {
        val deferred1 = async { fetchData(1, 100) }
        val deferred2 = async { fetchData(2, 100) }
        val deferred3 = async { fetchData(3, 100) }

        val result1 = deferred1.await()
        val result2 = deferred2.await()
        val result3 = deferred3.await()

        log.info("  결과: $result1, $result2, $result3")
      }
    }

    log.info("  총 소요 시간: ${time}ms (약 100ms)")
    assertTrue(time < 200, "병렬 실행이므로 200ms 미만 소요")
  }

  // ===== Dispatcher =====
  @Test
  fun dispatcherTest() = runBlocking {
    log.info("=== 1. Dispatchers.Default - CPU 작업 ===")
    dispatcherDefault()

    delay(100)

    log.info("=== 2. Dispatchers.IO - I/O 작업 ===")
    dispatcherIO()

    delay(100)

    log.info("=== 3. withContext - Dispatcher 전환 ===")
    dispatcherSwitch()
  }

  /**
   * Dispatchers.Default - CPU 집약적 작업
   */
  private suspend fun dispatcherDefault() {
    log.info("Dispatchers.Default는 CPU 집약적 작업용")

    withContext(Dispatchers.Default) {
      log.info("  실행 Thread: ${Thread.currentThread().name}")
      val result = (1..1000).sum()
      log.info("  계산 결과: $result")
    }
  }

  /**
   * Dispatchers.IO - I/O 작업
   */
  private suspend fun dispatcherIO() {
    log.info("Dispatchers.IO는 I/O 작업용")

    withContext(Dispatchers.IO) {
      log.info("  실행 Thread: ${Thread.currentThread().name}")
      delay(50) // I/O 시뮬레이션
      log.info("  I/O 작업 완료")
    }
  }

  /**
   * withContext - Dispatcher 전환
   */
  private suspend fun dispatcherSwitch() {
    log.info("withContext로 Dispatcher 전환 가능")

    withContext(Dispatchers.IO) {
      log.info("  IO Thread: ${Thread.currentThread().name}")
      val data = "data"

      withContext(Dispatchers.Default) {
        log.info("  Default Thread: ${Thread.currentThread().name}")
        data.uppercase()
      }

      log.info("  다시 IO Thread: ${Thread.currentThread().name}")
    }
  }

  // ===== Structured Concurrency =====
  @Test
  fun structuredConcurrencyTest() = runBlocking {
    log.info("=== 1. coroutineScope - 모두 성공해야 함 ===")
    coroutineScopeTest()

    delay(100)

    log.info("=== 2. supervisorScope - 독립적 실행 ===")
    supervisorScopeTest()
  }

  /**
   * coroutineScope
   * - 하나라도 실패하면 모두 취소
   */
  private suspend fun coroutineScopeTest() {
    log.info("coroutineScope는 하나 실패 시 모두 취소")

    try {
      coroutineScope {
        launch {
          delay(50)
          log.info("  작업 1 완료")
        }

        launch {
          delay(100)
          log.info("  작업 2 실행 중...")
          throw RuntimeException("Error")
        }

        launch {
          delay(150)
          log.info("  작업 3 완료 (실행 안 될 것)")
        }
      }
    } catch (e: Exception) {
      log.info("  예외 발생: ${e.message}")
      log.info("  모든 자식 코루틴이 취소됨")
    }
  }

  /**
   * supervisorScope
   * - 자식 실패가 다른 자식에 영향 없음
   */
  private suspend fun supervisorScopeTest() {
    log.info("supervisorScope는 자식 실패가 독립적")

    supervisorScope {
      launch {
        delay(50)
        log.info("  작업 1 완료")
      }

      launch {
        try {
          delay(100)
          throw RuntimeException("Error")
        } catch (e: Exception) {
          log.info("  작업 2 실패: ${e.message}")
        }
      }

      launch {
        delay(150)
        log.info("  작업 3 완료 (계속 실행됨)")
      }
    }

    log.info("  supervisorScope 완료")
  }

  // ===== 코루틴 취소 =====
  @Test
  fun cancellationTest() = runBlocking {
    log.info("=== 1. 코루틴 취소 기본 ===")
    cancellationBasic()

    delay(100)

    log.info("=== 2. isActive 체크 ===")
    cancellationWithIsActive()

    delay(100)

    log.info("=== 3. timeout ===")
    timeoutTest()
  }

  /**
   * 코루틴 취소 기본
   */
  private suspend fun cancellationBasic() {
    log.info("Job.cancel()로 코루틴 취소")

    val job = CoroutineScope(Dispatchers.Default).launch {
      repeat(5) { i ->
        log.info("  작업 중... $i")
        delay(100)
      }
    }

    delay(250)
    log.info("  코루틴 취소 요청")
    job.cancelAndJoin()
    log.info("  코루틴 취소 완료")
  }

  /**
   * isActive 체크
   * - CPU 집약적 작업에서 취소 체크
   */
  private suspend fun cancellationWithIsActive() {
    log.info("isActive로 취소 가능한 코루틴 작성")

    val job = CoroutineScope(Dispatchers.Default).launch {
      var count = 0
      while (isActive) {
        count++
        if (count % 1000000 == 0) {
          log.info("  작업 중... ${count / 1000000}M")
        }
        if (count >= 3000000) break
      }
      log.info("  작업 완료: $count")
    }

    delay(50)
    job.cancelAndJoin()
    log.info("  isActive 체크로 취소됨")
  }

  /**
   * timeout
   */
  private suspend fun timeoutTest() {
    log.info("withTimeout으로 타임아웃 설정")

    try {
      withTimeout(100) {
        log.info("  작업 시작")
        delay(200)
        log.info("  작업 완료 (실행 안 됨)")
      }
    } catch (e: TimeoutCancellationException) {
      log.info("  타임아웃 발생: ${e.message}")
    }

    val result = withTimeoutOrNull(100) {
      delay(200)
      "result"
    }
    log.info("  withTimeoutOrNull 결과: $result")
    assertNull(result)
  }

  // ===== Exception Handling =====
  @Test
  fun exceptionHandlingTest() = runBlocking {
    log.info("=== 1. try-catch로 예외 처리 ===")
    tryCatchException()

    delay(100)

    log.info("=== 2. CoroutineExceptionHandler ===")
    exceptionHandlerTest()
  }

  /**
   * try-catch로 예외 처리
   */
  private suspend fun tryCatchException() {
    log.info("launch는 try-catch로 예외 처리")

    coroutineScope {
      launch {
        try {
          delay(50)
          throw RuntimeException("Error")
        } catch (e: Exception) {
          log.info("  예외 처리: ${e.message}")
        }
      }
    }

    log.info("  예외 처리 완료")
  }

  /**
   * CoroutineExceptionHandler
   */
  private suspend fun exceptionHandlerTest() {
    log.info("CoroutineExceptionHandler로 예외 처리")

    val handler = CoroutineExceptionHandler { _, exception ->
      log.info("  예외 핸들러: ${exception.message}")
    }

    val scope = CoroutineScope(Dispatchers.Default + handler)
    scope.launch {
      delay(50)
      throw RuntimeException("Error")
    }

    delay(150)
    log.info("  예외 핸들러로 처리됨")
  }

  // ===== Flow 기본 =====
  @Test
  fun flowBasicTest() = runBlocking {
    log.info("=== 1. Flow 생성과 수집 ===")
    flowBasic()

    delay(100)

    log.info("=== 2. Flow는 Cold Stream ===")
    flowColdStream()
  }

  /**
   * Flow 생성과 수집
   */
  private suspend fun flowBasic() {
    log.info("Flow는 비동기 데이터 스트림")

    val flow = flow {
      for (i in 1..3) {
        delay(50)
        log.info("  emit: $i")
        emit(i)
      }
    }

    flow.collect { value ->
      log.info("  collect: $value")
    }

    log.info("  Flow 수집 완료")
  }

  /**
   * Flow는 Cold Stream
   * - 구독할 때마다 새로 시작
   */
  private suspend fun flowColdStream() {
    log.info("Flow는 Cold Stream (구독마다 새로 시작)")

    val executionCount = AtomicInteger(0)
    val flow = flow {
      executionCount.incrementAndGet()
      emit("data")
    }

    log.info("  첫 번째 수집")
    flow.collect { log.info("  구독자1: $it") }

    log.info("  두 번째 수집")
    flow.collect { log.info("  구독자2: $it") }

    log.info("  실행 횟수: ${executionCount.get()}")
    assertEquals(2, executionCount.get())
  }

  // ===== Flow 연산자 =====
  @Test
  fun flowOperatorsTest() = runBlocking {
    log.info("=== 1. map - 변환 ===")
    flowMap()

    delay(100)

    log.info("=== 2. filter - 필터링 ===")
    flowFilter()

    delay(100)

    log.info("=== 3. transform - 복잡한 변환 ===")
    flowTransform()
  }

  /**
   * map - 변환
   */
  private suspend fun flowMap() {
    log.info("map으로 데이터 변환")

    flowOf(1, 2, 3)
      .map { it * 2 }
      .collect { value ->
        log.info("  $value")
      }
  }

  /**
   * filter - 필터링
   */
  private suspend fun flowFilter() {
    log.info("filter로 데이터 필터링")

    flowOf(1, 2, 3, 4, 5)
      .filter { it % 2 == 0 }
      .collect { value ->
        log.info("  $value")
      }
  }

  /**
   * transform - 복잡한 변환
   */
  private suspend fun flowTransform() {
    log.info("transform으로 복잡한 변환")

    flowOf(1, 2, 3)
      .transform { value ->
        emit(value)
        emit(value * 2)
      }
      .collect { value ->
        log.info("  $value")
      }
  }

  // ===== Flow 결합 =====
  @Test
  fun flowCombineTest() = runBlocking {
    log.info("=== 1. zip - 쌍으로 결합 ===")
    flowZip()

    delay(100)

    log.info("=== 2. combine - 최신 값 결합 ===")
    flowCombine()
  }

  /**
   * zip - 쌍으로 결합
   */
  private suspend fun flowZip() {
    log.info("zip은 쌍으로 결합")

    val flow1 = flowOf(1, 2, 3)
    val flow2 = flowOf("A", "B", "C")

    flow1.zip(flow2) { num, letter ->
      "$num$letter"
    }.collect { value ->
      log.info("  $value")
    }
  }

  /**
   * combine - 최신 값 결합
   */
  private suspend fun flowCombine() {
    log.info("combine은 최신 값 결합")

    val numbers = flowOf(1, 2, 3).onEach { delay(50) }
    val letters = flowOf("A", "B", "C").onEach { delay(70) }

    numbers.combine(letters) { num, letter ->
      "$num$letter"
    }.collect { value ->
      log.info("  $value")
    }
  }

  // ===== Flow Context =====
  @Test
  fun flowContextTest() = runBlocking {
    log.info("=== flowOn - Dispatcher 변경 ===")
    flowOnTest()
  }

  /**
   * flowOn - Upstream Dispatcher 변경
   */
  private suspend fun flowOnTest() {
    log.info("flowOn으로 Flow의 Dispatcher 변경")

    flow {
      log.info("  Flow 실행: ${Thread.currentThread().name}")
      emit(1)
    }
      .flowOn(Dispatchers.IO)
      .map {
        log.info("  map 실행: ${Thread.currentThread().name}")
        it * 2
      }
      .collect {
        log.info("  collect 실행: ${Thread.currentThread().name}")
        log.info("  결과: $it")
      }
  }

  // ===== Flow 예외 처리 =====
  @Test
  fun flowExceptionTest() = runBlocking {
    log.info("=== 1. catch - 예외 처리 ===")
    flowCatch()

    delay(100)

    log.info("=== 2. onCompletion - 완료 처리 ===")
    flowOnCompletion()
  }

  /**
   * catch - 예외 처리
   */
  private suspend fun flowCatch() {
    log.info("catch로 예외 처리")

    flow {
      emit(1)
      throw RuntimeException("Error")
    }
      .catch { e ->
        log.info("  예외 발생: ${e.message}")
        emit(-1)
      }
      .collect { value ->
        log.info("  $value")
      }
  }

  /**
   * onCompletion - 완료 처리
   */
  private suspend fun flowOnCompletion() {
    log.info("onCompletion으로 완료 처리")

    flow {
      emit(1)
      emit(2)
    }
      .onCompletion { cause ->
        if (cause != null) {
          log.info("  에러로 완료: $cause")
        } else {
          log.info("  정상 완료")
        }
      }
      .collect { value ->
        log.info("  $value")
      }
  }

  // ===== StateFlow와 SharedFlow =====
  @Test
  fun stateFlowTest() = runBlocking {
    log.info("=== StateFlow - 상태 관리 ===")
    stateFlowBasic()
  }

  /**
   * StateFlow - 상태 관리
   */
  private suspend fun stateFlowBasic() = coroutineScope {
    log.info("StateFlow는 상태 관리용")

    val stateFlow = MutableStateFlow("Initial")

    val job = launch {
      stateFlow.collect { state ->
        log.info("  상태: $state")
      }
    }

    delay(50)
    log.info("  상태 변경: Updated")
    stateFlow.value = "Updated"

    delay(50)
    log.info("  상태 변경: Final")
    stateFlow.value = "Final"

    delay(50)
    job.cancel()
  }

  @Test
  fun sharedFlowTest() = runBlocking {
    log.info("=== SharedFlow - 이벤트 브로드캐스팅 ===")
    sharedFlowBasic()
  }

  /**
   * SharedFlow - 이벤트 브로드캐스팅
   */
  private suspend fun sharedFlowBasic() = coroutineScope {
    log.info("SharedFlow는 이벤트 브로드캐스팅용")

    val sharedFlow = MutableSharedFlow<String>()

    val job1 = launch {
      sharedFlow.collect { event ->
        log.info("  구독자1: $event")
      }
    }

    val job2 = launch {
      sharedFlow.collect { event ->
        log.info("  구독자2: $event")
      }
    }

    delay(50)
    log.info("  이벤트 발행: Click")
    sharedFlow.emit("Click")

    delay(50)
    log.info("  이벤트 발행: Submit")
    sharedFlow.emit("Submit")

    delay(50)
    job1.cancel()
    job2.cancel()
  }

  // ===== Channel =====
  @Test
  fun channelTest() = runBlocking {
    log.info("=== 1. Channel 기본 ===")
    channelBasic()

    delay(100)

    log.info("=== 2. produce - Channel 생성 ===")
    channelProduce()
  }

  /**
   * Channel 기본
   * - 코루틴 간 통신
   */
  private suspend fun channelBasic() = coroutineScope {
    log.info("Channel은 코루틴 간 통신 수단")

    val channel = Channel<Int>()

    launch {
      for (x in 1..5) {
        log.info("  송신: $x")
        channel.send(x)
      }
      channel.close()
    }

    launch {
      for (y in channel) {
        log.info("  수신: $y")
      }
    }

    delay(200)
  }

  /**
   * produce - Channel 생성
   */
  private suspend fun channelProduce() = coroutineScope {
    log.info("produce로 Channel 생성")

    val numbers = produce {
      var x = 1
      while (x <= 5) {
        send(x++)
        delay(50)
      }
    }

    repeat(5) {
      val value = numbers.receive()
      log.info("  수신: $value")
    }

    numbers.cancel()
  }

  // ===== 실무 패턴 =====
  @Test
  fun practicalPatternsTest() = runBlocking {
    log.info("=== 1. 병렬 API 호출 ===")
    parallelApiCalls()

    delay(100)

    log.info("=== 2. 재시도 로직 ===")
    retryPattern()

    delay(100)

    log.info("=== 3. 타임아웃과 폴백 ===")
    timeoutWithFallback()
  }

  /**
   * 병렬 API 호출
   */
  private suspend fun parallelApiCalls() {
    log.info("async로 병렬 API 호출")

    val time = measureTimeMillis {
      coroutineScope {
        val user = async { fetchData(1, 100) }
        val orders = async { fetchData(2, 100) }
        val payments = async { fetchData(3, 100) }

        log.info("  user: ${user.await()}")
        log.info("  orders: ${orders.await()}")
        log.info("  payments: ${payments.await()}")
      }
    }

    log.info("  총 소요 시간: ${time}ms")
  }

  /**
   * 재시도 로직
   */
  private suspend fun retryPattern() {
    log.info("재시도 로직 구현")

    val attemptCount = AtomicInteger(0)

    suspend fun <T> retry(
      times: Int = 3,
      initialDelay: Long = 100,
      block: suspend () -> T
    ): T {
      repeat(times - 1) {
        try {
          return block()
        } catch (e: Exception) {
          delay(initialDelay)
        }
      }
      return block()
    }

    val result = retry {
      val attempt = attemptCount.incrementAndGet()
      log.info("  시도: $attempt")
      if (attempt < 3) {
        throw RuntimeException("Failed")
      }
      "Success"
    }

    log.info("  결과: $result")
    assertEquals("Success", result)
  }

  /**
   * 타임아웃과 폴백
   */
  private suspend fun timeoutWithFallback() {
    log.info("타임아웃 시 폴백 값 반환")

    val result = withTimeoutOrNull(50) {
      delay(100)
      "data"
    } ?: "fallback"

    log.info("  결과: $result")
    assertEquals("fallback", result)
  }

  // ===== 테스트 (runTest) =====
  @Test
  fun virtualTimeTest() = runTest {
    log.info("=== runTest로 가상 시간 테스트 ===")

    val startTime = currentTime
    delay(1000)
    val endTime = currentTime

    log.info("  시작: $startTime, 종료: $endTime")
    log.info("  가상 시간 경과: ${endTime - startTime}ms")
    assertEquals(1000, endTime - startTime)
  }

  @Test
  fun advanceTimeByTest() = runTest {
    log.info("=== advanceTimeBy로 시간 진행 ===")

    val values = mutableListOf<Int>()

    val job = launch {
      delay(1000)
      values.add(1)
      delay(1000)
      values.add(2)
    }

    advanceTimeBy(1000)
    runCurrent() // 대기 중인 코루틴 실행
    assertEquals(listOf(1), values)
    log.info("  1초 경과 후: $values")

    advanceTimeBy(1000)
    runCurrent() // 대기 중인 코루틴 실행
    assertEquals(listOf(1, 2), values)
    log.info("  2초 경과 후: $values")

    job.cancel()
  }

  // ===== 성능 비교 =====
  @Test
  fun performanceTest() = runBlocking {
    log.info("=== 코루틴 성능 테스트 ===")

    val time = measureTimeMillis {
      val jobs = List(10_000) {
        launch(Dispatchers.Default) {
          delay(100)
        }
      }
      jobs.forEach { it.join() }
    }

    log.info("  10,000개 코루틴 실행 완료: ${time}ms")
    log.info("  Thread보다 훨씬 가벼움")
  }
}
