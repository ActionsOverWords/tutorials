# Kotlin Coroutines

## 기본 개념

### 정의
- **경량 Thread**: Thread보다 훨씬 가벼운 비동기 실행 단위
- **Non-blocking**: Thread를 블로킹하지 않고 중단(suspend) 가능
- **Structured Concurrency**: 코루틴의 생명주기를 구조적으로 관리

### Thread vs Virtual Thread vs Coroutine

| 구분                | Platform Thread | Virtual Thread | Coroutine     |
|-------------------|-----------------|----------------|---------------|
| 생성 비용             | 높음 (~1MB)       | 낮음 (~10KB)     | 낮음 (~KB)      |
| Context Switching | 비쌈 (OS 레벨)      | 저렴 (JVM 레벨)    | 저렴 (언어 레벨)    |
| 동시 실행 가능 수        | 제한적 (~수천)       | 많음 (~수백만)      | 거의 무제한 (~수백만) |
| 취소                | 어려움             | 보통             | 쉬움            |
| 제어                | OS가 스케줄링        | JVM이 스케줄링      | 프로그래머가 제어     |
| 플랫폼               | JVM 1.0+        | JVM 21+        | Kotlin        |
| 블로킹 처리            | Thread 블로킹      | 자동 언마운트        | suspend 함수    |

### 의존성
```kotlin
dependencies {
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.7.3")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
```

## Suspend Function

### 정의
- `suspend` 키워드로 선언
- 코루틴을 일시 중단하고 재개할 수 있는 함수
- 다른 suspend 함수에서만 호출 가능

```kotlin
suspend fun fetchUser(id: Long): User {
  delay(1000) // 1초 대기 (Non-blocking)
  return userRepository.findById(id)
}

// 일반 함수에서는 호출 불가
fun normalFunction() {
  // fetchUser(1L) // 컴파일 에러!
}

// suspend 함수에서만 호출 가능
suspend fun loadUserProfile(id: Long) {
  val user = fetchUser(id) // OK
  println(user)
}
```

### 특징
- **Non-blocking**: delay()는 Thread를 블로킹하지 않음
- **일시 중단**: 코루틴만 중단, Thread는 다른 작업 수행 가능
- **순차 실행**: 코루틴 내에서는 순차적으로 실행

```kotlin
suspend fun example() {
  println("Start: ${Thread.currentThread().name}")
  delay(1000) // Thread 블로킹 없이 1초 대기
  println("End: ${Thread.currentThread().name}")
}
```

## Coroutine Builders

### 1. launch - Fire and Forget
```kotlin
val job: Job = CoroutineScope(Dispatchers.IO).launch {
  // 백그라운드 작업
  val data = fetchData()
  println(data)
}

// Job 제어
job.cancel() // 취소
job.join() // 완료 대기
```

**특징:**
- 반환값 없음 (Job 반환)
- 예외 발생 시 부모로 전파
- Fire-and-forget 작업에 적합

### 2. async - 결과 반환
```kotlin
val deferred: Deferred<User> = CoroutineScope(Dispatchers.IO).async {
  fetchUser(1L)
}

val user: User = deferred.await() // 결과 대기
```

**특징:**
- 결과를 Deferred로 반환
- await()로 결과 획득 (일시 중단)
- 병렬 작업에 적합

### 3. runBlocking - 블로킹 (테스트/main 용)
```kotlin
fun main() = runBlocking {
  val user = fetchUser(1L)
  println(user)
}

@Test
fun test() = runBlocking {
  val result = suspendFunction()
  assertEquals(expected, result)
}
```

**특징:**
- 코루틴 완료까지 Thread 블로킹
- main 함수, 테스트에서만 사용
- 프로덕션 코드에서는 사용 금지

### 비교

```kotlin
suspend fun example() {
  // launch: 결과 없음
  CoroutineScope(Dispatchers.IO).launch {
    println("Launch: ${Thread.currentThread().name}")
  }

  // async: 결과 있음
  val deferred = CoroutineScope(Dispatchers.IO).async {
    "Async result"
  }
  val result = deferred.await()

  // runBlocking: Thread 블로킹
  runBlocking {
    delay(1000)
    println("RunBlocking")
  }
}
```

## CoroutineScope와 CoroutineContext

### CoroutineScope
- 코루틴의 생명주기 관리
- Structured Concurrency 제공

```kotlin
// GlobalScope (사용 금지 - 생명주기 제어 불가)
GlobalScope.launch {
  // 앱 종료 시까지 실행
}

// CoroutineScope (권장)
val scope = CoroutineScope(Dispatchers.Default)
scope.launch {
  // scope 취소 시 함께 취소됨
}
scope.cancel() // 모든 자식 코루틴 취소
```

### CoroutineContext
- 코루틴의 실행 환경 정의
- Dispatcher, Job, CoroutineName 등 포함

```kotlin
val context: CoroutineContext =
  Dispatchers.IO + CoroutineName("MyCoroutine")

CoroutineScope(context).launch {
  println(coroutineContext[CoroutineName]?.name)
}
```

## Dispatchers - Thread 관리

### 종류

```kotlin
// 1. Dispatchers.Default - CPU 집약적 작업
CoroutineScope(Dispatchers.Default).launch {
  val result = complexCalculation()
}

// 2. Dispatchers.IO - I/O 작업
CoroutineScope(Dispatchers.IO).launch {
  val data = apiCall()
  val file = readFile()
}

// 3. Dispatchers.Main - UI 작업 (Android/JavaFX)
CoroutineScope(Dispatchers.Main).launch {
  updateUI()
}

// 4. Dispatchers.Unconfined - 테스트용 (사용 권장 안 함)
CoroutineScope(Dispatchers.Unconfined).launch {
  // 호출한 Thread에서 실행
}
```

### 선택 가이드

| Dispatcher | Thread Pool 크기 | 용도                    |
|------------|----------------|-----------------------|
| Default    | CPU 코어 수       | 계산, 정렬, 파싱            |
| IO         | 64 (제한 있음)     | Network, DB, File I/O |
| Main       | 1 (메인 Thread)  | UI 업데이트               |
| Unconfined | -              | 테스트 (실무 사용 금지)        |

### Dispatcher 전환

```kotlin
suspend fun example() {
  // IO Thread에서 시작
  withContext(Dispatchers.IO) {
    val data = fetchData()

    // CPU 작업은 Default로 전환
    withContext(Dispatchers.Default) {
      processData(data)
    }

    // 다시 IO로 전환
    saveData(data)
  }
}
```

## Structured Concurrency

### 개념
- 부모 코루틴이 취소되면 모든 자식 코루틴도 취소
- 자식 코루틴이 예외를 던지면 부모로 전파
- 코루틴의 생명주기를 명확하게 관리

```kotlin
suspend fun example() {
  coroutineScope {
    // 자식 1
    launch {
      delay(1000)
      println("Child 1")
    }

    // 자식 2
    launch {
      delay(500)
      throw RuntimeException("Error")
    }

    // 자식 2가 예외를 던지면 자식 1도 취소됨
  }
}
```

### coroutineScope vs supervisorScope

```kotlin
// coroutineScope: 하나의 자식 실패 시 모두 취소
suspend fun example1() {
  coroutineScope {
    launch { task1() } // 성공
    launch { throw RuntimeException() } // 실패
    launch { task3() } // 취소됨
  }
  // 여기 도달 안 함 (예외 전파)
}

// supervisorScope: 자식 실패가 다른 자식에 영향 안 줌
suspend fun example2() {
  supervisorScope {
    launch { task1() } // 성공
    launch { throw RuntimeException() } // 실패
    launch { task3() } // 계속 실행
  }
  // 여기 도달 (예외 전파 안 됨)
}
```

### 실무 사용

```kotlin
// 모든 작업이 성공해야 하는 경우
suspend fun loadAllData() = coroutineScope {
  val user = async { fetchUser() }
  val orders = async { fetchOrders() }
  val payments = async { fetchPayments() }

  Triple(user.await(), orders.await(), payments.await())
  // 하나라도 실패하면 모두 취소
}

// 독립적인 작업 (실패해도 다른 작업 계속)
suspend fun loadDashboard() = supervisorScope {
  launch { loadRecentOrders() }
  launch { loadRecommendations() }
  launch { loadNotifications() }
  // 하나 실패해도 다른 작업은 계속
}
```

## 코루틴 취소

### 취소 기본

```kotlin
val job = CoroutineScope(Dispatchers.IO).launch {
  repeat(1000) { i ->
    println("Job: $i")
    delay(500)
  }
}

delay(2000)
job.cancel() // 취소
job.join() // 취소 완료 대기

// 또는
job.cancelAndJoin() // cancel + join
```

### 취소 가능한 코루틴 작성

```kotlin
// 안티패턴: 취소 불가능
suspend fun nonCancellable() {
  repeat(1000) {
    println("Working $it")
    Thread.sleep(100) // delay 대신 sleep 사용 시 취소 안 됨!
  }
}

// 권장 1: delay 사용
suspend fun cancellable1() {
  repeat(1000) {
    println("Working $it")
    delay(100) // 취소 체크 지점
  }
}

// 권장 2: isActive 체크
suspend fun cancellable2() {
  while (isActive) {
    println("Working")
    // CPU 집약적 작업
  }
}

// 권장 3: ensureActive() 사용
suspend fun cancellable3() {
  repeat(1000) {
    ensureActive() // 취소되었으면 CancellationException 발생
    complexCalculation()
  }
}
```

### 취소 불가능한 블록

```kotlin
suspend fun example() {
  withContext(NonCancellable) {
    // 취소되어도 반드시 실행되어야 하는 작업
    saveLog()
    closeResources()
  }
}
```

### 타임아웃

```kotlin
// 타임아웃 시 예외 발생
suspend fun example1() {
  try {
    withTimeout(1000) {
      slowOperation()
    }
  } catch (e: TimeoutCancellationException) {
    println("Timeout!")
  }
}

// 타임아웃 시 null 반환
suspend fun example2() {
  val result = withTimeoutOrNull(1000) {
    slowOperation()
  }

  if (result == null) {
    println("Timeout!")
  }
}
```

## Exception Handling

### 기본 예외 처리

```kotlin
// launch: 예외가 부모로 전파
CoroutineScope(Dispatchers.IO).launch {
  try {
    throw RuntimeException("Error")
  } catch (e: Exception) {
    println("Caught: ${e.message}")
  }
}

// async: await() 호출 시 예외 발생
val deferred = CoroutineScope(Dispatchers.IO).async {
  throw RuntimeException("Error")
}

try {
  deferred.await()
} catch (e: Exception) {
  println("Caught: ${e.message}")
}
```

### CoroutineExceptionHandler

```kotlin
val handler = CoroutineExceptionHandler { _, exception ->
  println("Caught: ${exception.message}")
}

CoroutineScope(Dispatchers.IO + handler).launch {
  throw RuntimeException("Error")
  // handler가 예외 처리
}
```

**주의:**
- launch에만 적용 (async는 await()에서 처리)
- 최상위 코루틴에만 적용 (자식 코루틴은 부모로 전파)

### supervisorScope와 예외

```kotlin
suspend fun example() {
  supervisorScope {
    val job1 = launch {
      try {
        throw RuntimeException("Error")
      } catch (e: Exception) {
        println("Job1 error: ${e.message}")
      }
    }

    val job2 = launch {
      delay(1000)
      println("Job2 완료")
    }

    // Job1 실패해도 Job2는 계속 실행
  }
}
```

## Flow - Reactive Stream

### 기본 개념

```kotlin
// Flow 생성
val flow: Flow<Int> = flow {
  for (i in 1..5) {
    delay(100)
    emit(i) // 값 방출
  }
}

// Flow 소비
flow.collect { value ->
  println(value)
}
```

### Flow vs Sequence vs List

| 구분       | Cold/Hot | 실행 방식 | 비동기 |
|----------|----------|-------|-----|
| List     | Cold     | 즉시 평가 | X   |
| Sequence | Cold     | 지연 평가 | X   |
| Flow     | Cold     | 지연 평가 | O   |

```kotlin
// List: 즉시 모든 값 계산
val list = listOf(1, 2, 3).map { it * 2 } // 즉시 실행

// Sequence: 필요할 때 계산
val sequence = sequenceOf(1, 2, 3).map { it * 2 } // 지연
val firstTwo = sequence.take(2).toList() // 2개만 계산

// Flow: 비동기 스트림
val flow = flowOf(1, 2, 3).map { delay(100); it * 2 }
flow.collect { println(it) } // 비동기 수집
```

### Flow 생성

```kotlin
// 1. flow { } 빌더
val flow1 = flow {
  emit(1)
  emit(2)
  emit(3)
}

// 2. flowOf()
val flow2 = flowOf(1, 2, 3)

// 3. asFlow()
val flow3 = listOf(1, 2, 3).asFlow()

// 4. channelFlow - 동시 방출
val flow4 = channelFlow {
  launch { send(1) }
  launch { send(2) }
  launch { send(3) }
}
```

### Flow 연산자

```kotlin
// map: 변환
flowOf(1, 2, 3)
  .map { it * 2 }
  .collect { println(it) } // 2, 4, 6

// filter: 필터링
flowOf(1, 2, 3, 4, 5)
  .filter { it % 2 == 0 }
  .collect { println(it) } // 2, 4

// transform: 복잡한 변환
flowOf(1, 2, 3)
  .transform { value ->
    emit(value)
    emit(value * 2)
  }
  .collect { println(it) } // 1, 2, 2, 4, 3, 6

// take: 제한
flowOf(1, 2, 3, 4, 5)
  .take(3)
  .collect { println(it) } // 1, 2, 3

// drop: 건너뛰기
flowOf(1, 2, 3, 4, 5)
  .drop(2)
  .collect { println(it) } // 3, 4, 5
```

### Flow 결합

```kotlin
// zip: 쌍으로 결합
val flow1 = flowOf(1, 2, 3)
val flow2 = flowOf("A", "B", "C")

flow1.zip(flow2) { num, letter ->
  "$num$letter"
}.collect { println(it) } // 1A, 2B, 3C

// combine: 최신 값 결합
val numbers = flowOf(1, 2, 3).onEach { delay(100) }
val letters = flowOf("A", "B", "C").onEach { delay(150) }

numbers.combine(letters) { num, letter ->
  "$num$letter"
}.collect { println(it) } // 여러 조합 출력

// flatMapConcat: 순차 병합
flowOf(1, 2, 3)
  .flatMapConcat { value ->
    flow {
      emit("$value-A")
      emit("$value-B")
    }
  }
  .collect { println(it) } // 1-A, 1-B, 2-A, 2-B, 3-A, 3-B

// flatMapMerge: 동시 병합
flowOf(1, 2, 3)
  .flatMapMerge { value ->
    flow {
      delay(100)
      emit(value * 2)
    }
  }
  .collect { println(it) } // 순서 보장 안 됨
```

### Flow Context

```kotlin
// flowOn: Upstream Dispatcher 변경
flow {
  println("Flow: ${Thread.currentThread().name}")
  emit(1)
}
  .flowOn(Dispatchers.IO) // Flow는 IO에서 실행
  .map {
    println("Map: ${Thread.currentThread().name}")
    it * 2
  }
  .collect {
    println("Collect: ${Thread.currentThread().name}")
  }

// 출력:
// Flow: DefaultDispatcher-worker-1
// Map: main
// Collect: main
```

### Flow 예외 처리

```kotlin
// catch: 예외 처리
flow {
  emit(1)
  throw RuntimeException("Error")
}
  .catch { e ->
    println("Caught: ${e.message}")
    emit(-1) // 대체 값
  }
  .collect { println(it) } // 1, -1

// onCompletion: 완료 처리 (finally와 유사)
flow {
  emit(1)
  emit(2)
}
  .onCompletion { cause ->
    if (cause != null) {
      println("Error: $cause")
    } else {
      println("Completed")
    }
  }
  .collect { println(it) }
```

### StateFlow와 SharedFlow

```kotlin
// StateFlow: 상태 관리 (항상 최신 값 보유)
class ViewModel {
  private val _state = MutableStateFlow("Initial")
  val state: StateFlow<String> = _state.asStateFlow()

  fun updateState(value: String) {
    _state.value = value
  }
}

// 사용
val viewModel = ViewModel()
viewModel.state.collect { state ->
  println("State: $state")
}
viewModel.updateState("Updated")

// SharedFlow: 이벤트 브로드캐스팅
class EventBus {
  private val _events = MutableSharedFlow<Event>()
  val events: SharedFlow<Event> = _events.asSharedFlow()

  suspend fun emit(event: Event) {
    _events.emit(event)
  }
}

// 사용
val eventBus = EventBus()
launch {
  eventBus.events.collect { event ->
    println("Event: $event")
  }
}
eventBus.emit(Event("Click"))
```

**StateFlow vs SharedFlow:**

| 특징     | StateFlow     | SharedFlow |
|--------|---------------|------------|
| 초기값    | 필수            | 선택         |
| 최신값 보관 | 항상            | 설정 가능      |
| 중복 방출  | 제거 (distinct) | 모두 방출      |
| 용도     | 상태 관리         | 이벤트        |

## Channel - 파이프라인

### 기본 개념
- 코루틴 간 통신 수단
- BlockingQueue와 유사하지만 suspend 함수 사용

```kotlin
val channel = Channel<Int>()

// 송신
launch {
  for (x in 1..5) {
    channel.send(x)
  }
  channel.close()
}

// 수신
launch {
  for (y in channel) {
    println(y)
  }
}
```

### Channel 타입

```kotlin
// 1. Unlimited - 무제한 버퍼
val unlimited = Channel<Int>(Channel.UNLIMITED)

// 2. Buffered - 제한된 버퍼
val buffered = Channel<Int>(10)

// 3. Rendezvous - 버퍼 없음 (기본값)
val rendezvous = Channel<Int>(Channel.RENDEZVOUS)

// 4. Conflated - 최신 값만 보관
val conflated = Channel<Int>(Channel.CONFLATED)
```

### produce - Channel 생성

```kotlin
fun CoroutineScope.produceNumbers() = produce {
  var x = 1
  while (true) {
    send(x++)
    delay(100)
  }
}

val numbers = produceNumbers()
repeat(5) {
  println(numbers.receive())
}
numbers.cancel()
```

### Pipeline 패턴

```kotlin
fun CoroutineScope.produceNumbers() = produce {
  var x = 1
  while (true) send(x++)
}

fun CoroutineScope.square(numbers: ReceiveChannel<Int>) = produce {
  for (x in numbers) send(x * x)
}

// 사용
val numbers = produceNumbers()
val squares = square(numbers)

repeat(5) {
  println(squares.receive())
}

coroutineContext.cancelChildren()
```

### Fan-out / Fan-in

```kotlin
// Fan-out: 하나의 송신자, 여러 수신자
fun CoroutineScope.produceNumbers() = produce {
  var x = 1
  while (true) {
    send(x++)
    delay(100)
  }
}

val producer = produceNumbers()

// 여러 소비자
repeat(5) { id ->
  launch {
    for (msg in producer) {
      println("Consumer $id received $msg")
    }
  }
}

// Fan-in: 여러 송신자, 하나의 수신자
suspend fun sendString(channel: SendChannel<String>, s: String, time: Long) {
  while (true) {
    delay(time)
    channel.send(s)
  }
}

val channel = Channel<String>()
launch { sendString(channel, "A", 200) }
launch { sendString(channel, "B", 500) }

repeat(10) {
  println(channel.receive())
}
```

## Select Expression

### 여러 Channel 중 먼저 도착한 것 처리

```kotlin
suspend fun selectExample(channel1: ReceiveChannel<String>,
                          channel2: ReceiveChannel<String>) {
  select<Unit> {
    channel1.onReceive { value ->
      println("Channel1: $value")
    }
    channel2.onReceive { value ->
      println("Channel2: $value")
    }
  }
}
```

### 타임아웃과 함께 사용

```kotlin
suspend fun selectWithTimeout(channel: ReceiveChannel<String>) {
  select<String?> {
    channel.onReceive { value ->
      value
    }
    onTimeout(1000) {
      null
    }
  }
}
```

## Spring 통합

### 의존성
```kotlin
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.7.3")
```

### Controller에서 Coroutine 사용

```kotlin
@RestController
@RequestMapping("/api/users")
class UserController(
  private val userService: UserService
) {

  @GetMapping("/{id}")
  suspend fun getUser(@PathVariable id: Long): User {
    return userService.findById(id)
  }

  @GetMapping
  fun getAllUsers(): Flow<User> {
    return userService.findAll()
  }

  @PostMapping
  suspend fun createUser(@RequestBody user: User): User {
    return userService.save(user)
  }
}
```

### Service Layer

```kotlin
@Service
class UserService(
  private val userRepository: UserRepository,
  private val orderRepository: OrderRepository
) {

  suspend fun findById(id: Long): User {
    return userRepository.findById(id)
  }

  fun findAll(): Flow<User> {
    return userRepository.findAll()
  }

  // 병렬 처리
  suspend fun getUserProfile(userId: Long): UserProfile = coroutineScope {
    val user = async { userRepository.findById(userId) }
    val orders = async { orderRepository.findByUserId(userId) }

    UserProfile(user.await(), orders.await())
  }
}
```

### Reactor와 Coroutine 상호 변환

```kotlin
// Mono → Coroutine
suspend fun fromMono() {
  val user: User = mono.awaitSingle()
}

// Flux → Flow
fun fromFlux(): Flow<User> {
  return flux.asFlow()
}

// Coroutine → Mono
fun toMono(): Mono<User> {
  return mono {
    fetchUser()
  }
}

// Flow → Flux
fun toFlux(): Flux<User> {
  return flow.asFlux()
}
```

### WebClient와 Coroutine

```kotlin
@Service
class ApiClient(private val webClient: WebClient) {

  suspend fun getUser(id: Long): User {
    return webClient.get()
      .uri("/users/{id}", id)
      .retrieve()
      .awaitBody<User>()
  }

  fun getUsers(): Flow<User> {
    return webClient.get()
      .uri("/users")
      .retrieve()
      .bodyToFlow<User>()
  }
}
```

### Transaction 처리

```kotlin
@Service
class OrderService(
  private val transactionalOperator: TransactionalOperator
) {

  suspend fun createOrder(order: Order): Order {
    return transactionalOperator.executeAndAwait { status ->
      // Transaction 내에서 실행
      orderRepository.save(order)
    } ?: throw Exception("Transaction failed")
  }
}
```

## 실무 패턴

### 1. 병렬 API 호출

```kotlin
suspend fun loadDashboard() = coroutineScope {
  val users = async { userApi.getUsers() }
  val orders = async { orderApi.getOrders() }
  val stats = async { statsApi.getStats() }

  Dashboard(
    users.await(),
    orders.await(),
    stats.await()
  )
}
```

### 2. 재시도 로직

```kotlin
suspend fun <T> retry(
  times: Int = 3,
  initialDelay: Long = 100,
  maxDelay: Long = 1000,
  factor: Double = 2.0,
  block: suspend () -> T
): T {
  var currentDelay = initialDelay
  repeat(times - 1) {
    try {
      return block()
    } catch (e: Exception) {
      delay(currentDelay)
      currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
  }
  return block() // 마지막 시도
}

// 사용
val result = retry {
  apiClient.call()
}
```

### 3. 타임아웃과 폴백

```kotlin
suspend fun fetchDataWithFallback(): Data {
  return withTimeoutOrNull(3000) {
    apiClient.fetchData()
  } ?: cache.getData() // 타임아웃 시 캐시 사용
}
```

### 4. 배치 처리

```kotlin
suspend fun processBatch(items: List<Item>) = coroutineScope {
  items.chunked(100).forEach { chunk ->
    chunk.map { item ->
      async(Dispatchers.IO) {
        processItem(item)
      }
    }.awaitAll()
  }
}
```

### 5. Rate Limiting

```kotlin
class RateLimiter(
  private val permits: Int,
  private val period: Duration
) {
  private val semaphore = Semaphore(permits)

  suspend fun <T> execute(block: suspend () -> T): T {
    semaphore.acquire()
    return try {
      block()
    } finally {
      launch {
        delay(period.toMillis())
        semaphore.release()
      }
    }
  }
}

// 사용
val limiter = RateLimiter(10, Duration.ofSeconds(1))
limiter.execute {
  apiClient.call()
}
```

### 6. 동시 실행 제한

```kotlin
suspend fun processWithLimit(items: List<Item>) {
  val semaphore = Semaphore(10) // 최대 10개 동시 실행

  coroutineScope {
    items.forEach { item ->
      launch {
        semaphore.withPermit {
          processItem(item)
        }
      }
    }
  }
}
```

## 테스트

### 의존성
```kotlin
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
```

### runTest - 가상 시간

```kotlin
@Test
fun testDelay() = runTest {
  val startTime = currentTime

  delay(1000) // 실제 대기 없음

  val endTime = currentTime
  assertEquals(1000, endTime - startTime)
}
```

### TestDispatcher

```kotlin
@Test
fun testWithTestDispatcher() = runTest {
  val repository = UserRepository(this.coroutineContext)

  launch {
    delay(1000)
    repository.save(user)
  }

  advanceTimeBy(1000) // 시간 진행

  val saved = repository.findById(1L)
  assertNotNull(saved)
}
```

### Flow 테스트

```kotlin
@Test
fun testFlow() = runTest {
  val flow = flow {
    emit(1)
    delay(1000)
    emit(2)
    delay(1000)
    emit(3)
  }

  val result = flow.toList()
  assertEquals(listOf(1, 2, 3), result)
}

@Test
fun testFlowWithTurbine() = runTest {
  val flow = flowOf(1, 2, 3)

  flow.test {
    assertEquals(1, awaitItem())
    assertEquals(2, awaitItem())
    assertEquals(3, awaitItem())
    awaitComplete()
  }
}
```

## 주의사항

### 1. GlobalScope 사용 금지

```kotlin
// 안티패턴
GlobalScope.launch {
  // 생명주기 제어 불가
}

// 권장
class MyClass {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  fun doWork() {
    scope.launch {
      // 작업
    }
  }

  fun cleanup() {
    scope.cancel()
  }
}
```

### 2. Blocking 코드 격리

```kotlin
// 안티패턴
suspend fun badExample() {
  Thread.sleep(1000) // Thread 블로킹!
}

// 권장
suspend fun goodExample() {
  withContext(Dispatchers.IO) {
    Thread.sleep(1000) // IO Dispatcher에서 실행
  }
}
```

### 3. 예외 처리

```kotlin
// 안티패턴: 예외 무시
launch {
  // 예외 발생 시 자동으로 전파
}

// 권장 1: CoroutineExceptionHandler
val handler = CoroutineExceptionHandler { _, e ->
  log.error("Error", e)
}
CoroutineScope(Dispatchers.IO + handler).launch {
  // 작업
}

// 권장 2: try-catch
launch {
  try {
    riskyOperation()
  } catch (e: Exception) {
    log.error("Error", e)
  }
}

// 권장 3: supervisorScope
supervisorScope {
  launch { task1() } // 실패해도 다른 작업에 영향 없음
  launch { task2() }
}
```

### 4. 취소 가능한 코루틴 작성

```kotlin
// 안티패턴: 취소 체크 없음
suspend fun nonCancellable() {
  while (true) {
    expensiveComputation()
  }
}

// 권장
suspend fun cancellable() {
  while (isActive) { // 취소 체크
    expensiveComputation()
  }
}
```

### 5. Flow collect는 suspend

```kotlin
// 안티패턴: 여러 번 collect
fun badExample() {
  flow.collect { } // 첫 번째 collect는 완료될 때까지 블로킹
  flow.collect { } // 두 번째는 실행 안 됨
}

// 권장: launch로 분리
fun goodExample() {
  scope.launch {
    flow.collect { }
  }
  scope.launch {
    flow.collect { }
  }
}
```

## 성능 비교

### Thread vs Coroutine

```kotlin
// Thread: 10,000개 생성 시 OutOfMemoryError
repeat(10_000) {
  Thread {
    Thread.sleep(1000)
  }.start()
}

// Coroutine: 100,000개도 문제없음
repeat(100_000) {
  CoroutineScope(Dispatchers.Default).launch {
    delay(1000)
  }
}
```

### Blocking vs Suspend

```kotlin
// Blocking: 10개 요청 = 10개 Thread 필요
fun blockingCall() {
  val threads = (1..10).map {
    Thread {
      Thread.sleep(1000)
      println("Done $it")
    }
  }
  threads.forEach { it.start() }
  threads.forEach { it.join() }
}

// Suspend: 10개 요청 = 1개 Thread로 처리 가능
suspend fun suspendCall() = coroutineScope {
  (1..10).map {
    launch {
      delay(1000)
      println("Done $it")
    }
  }
}
```

## 언제 Coroutine을 사용하는가?

### 권장 사용

- I/O 작업이 많은 경우 (Network, DB, File)
- 대량의 동시 작업이 필요한 경우
- 구조화된 동시성이 필요한 경우
- 취소와 타임아웃이 중요한 경우
- Spring WebFlux와 함께 사용

### 권장하지 않는 경우

- 간단한 동기 작업
- CPU-bound 작업만 있는 경우 (Thread Pool이 나을 수 있음)
- 팀의 코루틴 경험 부족
- 레거시 코드베이스 (마이그레이션 비용)

## 주요 개념 정리

### Coroutine vs Thread

| 구분                | Thread | Coroutine |
|-------------------|--------|-----------|
| 생성 비용             | 높음     | 낮음        |
| 최대 개수             | 수천     | 수백만       |
| 취소                | 어려움    | 쉬움        |
| Context Switching | OS     | 프로그래머     |

### suspend vs blocking

| 구분       | Thread 상태 | 다른 작업 가능 | 예시             |
|----------|-----------|----------|----------------|
| suspend  | 작업 중      | O        | delay()        |
| blocking | 대기        | X        | Thread.sleep() |

### launch vs async

| 구분     | 반환 타입       | 용도              |
|--------|-------------|-----------------|
| launch | Job         | Fire-and-forget |
| async  | Deferred<T> | 결과 필요           |

### Flow vs Channel

| 구분      | Cold/Hot | 용도         |
|---------|----------|------------|
| Flow    | Cold     | 데이터 스트림 처리 |
| Channel | Hot      | 코루틴 간 통신   |

### StateFlow vs SharedFlow

| 구분         | 초기값 | 최신값 보관 | 용도    |
|------------|-----|--------|-------|
| StateFlow  | 필수  | 항상     | 상태 관리 |
| SharedFlow | 선택  | 설정 가능  | 이벤트   |
