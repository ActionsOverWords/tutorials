# Reactor (Project Reactor)

## Reactive Programming 개념

### 정의
**비동기 데이터 스트림**을 처리하는 프로그래밍 패러다임

### 핵심 원칙
1. **Non-blocking**: Thread를 블로킹하지 않음
2. **Backpressure**: Consumer가 처리 속도 제어
3. **Event-driven**: 데이터가 준비되면 반응
4. **Stream-oriented**: 데이터를 스트림으로 처리

## Mono와 Flux

### Mono<T> - 0 또는 1개의 아이템
```kotlin
val mono = Mono.just("Hello")
val empty = Mono.empty<String>()
val error = Mono.error<String>(RuntimeException("Error"))
```

### Flux<T> - 0~N개의 아이템
```kotlin
val flux = Flux.just("A", "B", "C")
val range = Flux.range(1, 5) // 1, 2, 3, 4, 5
val interval = Flux.interval(Duration.ofSeconds(1)) // 1초마다 emit
```

### Mono vs Flux

| 구분    | Mono                        | Flux             |
|-------|-----------------------------|------------------|
| 아이템 수 | 0 또는 1개                     | 0~N개             |
| 유사 개념 | Optional, CompletableFuture | Stream, List     |
| 사용 사례 | 단일 결과 (User, String)        | 컬렉션 (List<User>) |

## 구독 (Subscribe)

**핵심: Publisher는 구독되기 전까지 아무것도 하지 않음**

```kotlin
val mono = Mono.just("Hello")

// 구독하지 않으면 아무 일도 일어나지 않음!
mono.subscribe() // 실행됨

// Consumer 전달
mono.subscribe { value ->
  println(value) // "Hello"
}

// Error와 Complete 처리
mono.subscribe(
  { value -> println(value) },
  { error -> System.err.println(error) },
  { println("Completed") }
)
```

### Nothing Happens Until You Subscribe

```kotlin
val executionCount = AtomicInteger(0)

// 이 코드는 실행되지 않음
val mono = Mono.fromCallable {
  executionCount.incrementAndGet()
  "result"
}
// executionCount는 여전히 0

// 구독해야 실행됨
mono.subscribe()
// 이제 executionCount가 1
```

**Reactive Programming의 핵심 원칙:**
- **Lazy Execution**: 필요할 때만 실행
- **Resource Efficiency**: 불필요한 작업 방지
- **Declarative**: 무엇을 할지 선언하고, 구독으로 실행

## 주요 연산자

### 1. 변환 연산자

#### map - 동기 변환 (T → U)
```kotlin
val mono = Mono.just("hello")
  .map(String::uppercase) // "HELLO"
  .map { s -> "$s WORLD" } // "HELLO WORLD"

val flux = Flux.range(1, 5)
  .map { n -> n * 2 } // 2, 4, 6, 8, 10
```

#### flatMap - 비동기 변환 (T → Mono&lt;U&gt; or Flux&lt;U&gt;)

```kotlin
val mono = Mono.just(1L)
  .flatMap { id ->
    Mono.just("User-$id")
      .delayElement(Duration.ofMillis(100))
  }

val flux = Flux.just(1, 2, 3)
  .flatMap { num ->
    Flux.just("$num-A", "$num-B")
  }
// 결과: 1-A, 1-B, 2-A, 2-B, 3-A, 3-B
```

### 2. 필터링 연산자

#### filter
```kotlin
val flux = Flux.range(1, 10)
  .filter { n -> n % 2 == 0 } // 2, 4, 6, 8, 10
```

### 3. 결합 연산자

#### zip - 여러 Publisher 결합
```kotlin
val firstName = Mono.just("John")
val lastName = Mono.just("Doe")

val fullName = Mono.zip(firstName, lastName)
  .map { tuple -> "${tuple.t1} ${tuple.t2}" } // "John Doe"

// 3개 결합
val mono3 = Mono.just(30)
val combined = Mono.zip(firstName, lastName, mono3)
  .map { tuple -> "${tuple.t1} ${tuple.t2}, ${tuple.t3}" }
// "John Doe, 30"
```

#### merge - 병합 (순서 보장 안 됨)
```kotlin
val flux1 = Flux.just("A", "B").delayElements(Duration.ofMillis(100))
val flux2 = Flux.just("C", "D").delayElements(Duration.ofMillis(50))

val merged = Flux.merge(flux1, flux2)
// 순서 보장 안 됨 (먼저 emit되는 대로)
```

#### concat - 순차 연결 (순서 보장)
```kotlin
val flux1 = Flux.just("A", "B")
val flux2 = Flux.just("C", "D")

val concatenated = Flux.concat(flux1, flux2)
// A, B, C, D (순서 보장)
```

### 4. 부수 효과 연산자

#### doOnNext, doOnError, doOnComplete
```kotlin
val mono = Mono.just("Hello")
  .doOnNext { value -> log.info("Value: {}", value) }
  .doOnError { error -> log.error("Error: {}", error) }
  .doOnSuccess { value -> log.info("Success: {}", value) }
```

#### doOnSubscribe, doFinally
```kotlin
val mono = Mono.just("Data")
  .doOnSubscribe { subscription ->
    log.info("구독 시작: $subscription")
  }
  .doOnNext { value ->
    log.info("데이터 emit: $value")
  }
  .doFinally { signalType ->
    log.info("종료: $signalType") // onComplete, onError, cancel
  }
```

**실행 순서:**
1. doOnSubscribe: 구독 시작 시
2. doOnNext: 데이터 emit 시
3. doFinally: 완료/에러/취소 시 (항상 실행)

### 5. 에러 처리 연산자

#### onErrorReturn - 에러 시 기본값 반환
```kotlin
val mono = Mono.error<String>(RuntimeException("Error"))
  .onErrorReturn("Fallback value")

// 특정 예외 타입만 처리
val mono2 = Mono.error<String>(IllegalArgumentException("Invalid"))
  .onErrorReturn(IllegalArgumentException::class.java, "IllegalArgument Fallback")
```

**주의: Eager Evaluation**
```kotlin
// 비효율적 - 에러가 안 나도 실행됨
.onErrorReturn(expensiveFallback())

// 올바른 사용
.onErrorReturn("Static Fallback")
```

#### onErrorResume - 에러 시 대체 Publisher
```kotlin
// 기본 사용
val mono = Mono.error<String>(RuntimeException("Error"))
  .onErrorResume { error ->
    log.warn("에러 발생: ${error.message}")
    Mono.just("Alternative value")
  }

// 특정 예외 타입만 처리
val mono2 = Mono.error<String>(IllegalArgumentException("Invalid"))
  .onErrorResume(IllegalArgumentException::class.java) { error ->
    Mono.just("IllegalArgument Fallback")
  }

// 예외 타입별 다른 처리
val mono3 = Mono.error<String>(IllegalStateException("Invalid State"))
  .onErrorResume { error ->
    when (error) {
      is IllegalArgumentException -> Mono.just("From DB")
      is IllegalStateException -> Mono.just("From Cache")
      else -> Mono.just("Default")
    }
  }
```

#### onErrorReturn vs onErrorResume

| 연산자           | 반환 타입     | Evaluation | 사용 사례                 |
|---------------|-----------|------------|-----------------------|
| onErrorReturn | 정적 값      | Eager      | 단순 기본값 (빈 리스트, 기본 객체) |
| onErrorResume | Publisher | Lazy       | 대체 로직 (캐시, 백업 API 호출) |

### 6. 재시도 및 타임아웃

#### retry - 재시도
```kotlin
val mono = Mono.fromCallable {
  if (attemptCount.incrementAndGet() < 3) {
    throw RuntimeException("Attempt failed")
  }
  "Success"
}.retry(2) // 최대 2번 재시도 (총 3번 시도)
```

#### timeout - 타임아웃
```kotlin
// 타임아웃 발생
val slow = Mono.delay(Duration.ofSeconds(2))
  .timeout(Duration.ofMillis(500))  // 500ms 제한
  .onErrorReturn(-1L)

// 타임아웃 미 발생
val fast = Mono.delay(Duration.ofMillis(100))
  .timeout(Duration.ofSeconds(1))
```

### 7. 기본값 처리

#### switchIfEmpty
```kotlin
val empty = Mono.empty<String>()
  .switchIfEmpty(Mono.just("Default Value"))

val notEmpty = Mono.just("Actual Value")
  .switchIfEmpty(Mono.just("Default Value"))
// "Actual Value" 반환
```

## 시간 관련 연산자

### delay 연산자

#### Mono.delay() - 지정된 시간 후 신호 emit
```kotlin
Mono.delay(Duration.ofSeconds(1))
  .subscribe { value -> println("1초 후: $value") }  // 출력: 1초 후: 0
```

#### delaySubscription() - 구독 시작을 지연
```kotlin
Mono.just("Hello")
  .delaySubscription(Duration.ofSeconds(1))  // 1초 대기 후 구독 시작
  .subscribe { println(it) }
```

#### delayElement() (Mono) / delayElements() (Flux)
```kotlin
// 각 아이템 사이에 지연 추가
Flux.just("A", "B", "C")
  .delayElements(Duration.ofMillis(100))
// A → 100ms → B → 100ms → C
```

**주의: delayElements는 Scheduler를 변경함**
```kotlin
Flux.just("A", "B")
  .doOnNext { println(Thread.currentThread().name) }  // main
  .delayElements(Duration.ofMillis(100))
  .doOnNext { println(Thread.currentThread().name) }  // parallel-1
```

### delay vs timeout 비교

| 구분        | delay                   | timeout               |
|-----------|-------------------------|-----------------------|
| **목적**    | 의도적으로 시간을 지연            | 응답 시간을 제한             |
| **결과**    | 지연 후 정상 진행              | 시간 초과 시 에러 발생         |
| **사용 사례** | Rate limiting, 테스트, 백오프 | API 타임아웃, 응답 시간 보장    |
| **에러**    | 발생 안 함                  | `TimeoutException` 발생 |

## Scheduler - Thread 관리

### 종류와 특징

#### 1. immediate()
- **Thread**: 현재 Thread 사용
- **사용 사례**: Thread 전환 불필요할 때
- **특징**: 오버헤드 없음

```kotlin
Mono.just("Hello")
  .subscribeOn(Schedulers.immediate())
```

#### 2. single()
- **Thread**: 단일 재사용 Thread
- **Thread Pool 크기**: 1개
- **사용 사례**: 순차 실행이 필요한 경량 작업

```kotlin
Mono.just("Hello")
  .subscribeOn(Schedulers.single())
```

#### 3. parallel()
- **Thread**: CPU 코어 수만큼 Thread Pool
- **Thread Pool 크기**: `Runtime.getRuntime().availableProcessors()`
- **사용 사례**: **CPU-bound 작업** (계산, 변환, 암호화)
- **특징**: Non-blocking 작업에 최적화

```kotlin
Flux.range(1, 100)
  .parallel()
  .runOn(Schedulers.parallel())
  .map { n -> n * 2 }  // CPU 집약적 계산
```

#### 4. boundedElastic()
- **Thread**: 동적으로 생성/제거되는 Thread Pool
- **Thread Pool 크기**:
  - 최대: `CPU 코어 수 * 10`
  - TTL: 60초 (유휴 Thread 제거)
- **사용 사례**: **I/O-bound & Blocking 작업** (파일, DB, API)
- **특징**: Blocking 코드 격리용

```kotlin
Mono.fromCallable {
  Thread.sleep(1000)  // Blocking!
  "result"
}.subscribeOn(Schedulers.boundedElastic())  // OK
```

### parallel vs boundedElastic 비교

| 구분              | parallel()   | boundedElastic()           |
|-----------------|--------------|----------------------------|
| **Thread 수**    | CPU 코어 수     | CPU 코어 수 * 10 (최대)         |
| **생성 방식**       | 고정 Pool      | 동적 생성/제거                   |
| **TTL**         | 없음 (항상 유지)   | 60초 (유휴 시 제거)              |
| **사용 목적**       | CPU-bound 작업 | I/O-bound & Blocking       |
| **Blocking 허용** | 안티패턴         | 권장                         |
| **예시**          | 계산, 변환, 암호화  | 파일 I/O, JDBC, Thread.sleep |

### subscribeOn vs publishOn

```kotlin
// subscribeOn: 소스(구독) 실행 Thread 지정
Flux.range(1, 5)
  .subscribeOn(Schedulers.boundedElastic()) // 여기서 실행
  .map { i -> i * 2 }

// publishOn: 이후 연산자 Thread 지정
Flux.range(1, 5)
  .map { i -> i * 2 } // main thread
  .publishOn(Schedulers.parallel())
  .map { i -> i + 1 } // parallel thread
```

| 메서드         | 영향 범위            | 사용 시점                     |
|-------------|------------------|---------------------------|
| subscribeOn | 소스(구독) 실행 Thread | 데이터 소스 Thread 지정 (DB, 파일) |
| publishOn   | 이후 연산자 실행 Thread | 중간 처리 Thread 변경           |

### Blocking 코드와 Scheduler

#### 잘못된 사용 - parallel에서 Blocking

```kotlin
// 안티패턴: parallel Scheduler에서 Blocking
Flux.range(1, 100)
  .parallel()
  .runOn(Schedulers.parallel())
  .map { n ->
    Thread.sleep(100)  // Blocking! parallel Thread를 차단함
    n * 2
  }
// 결과: parallel Thread가 모두 블로킹되어 전체 시스템 성능 저하
```

**왜 문제인가?**
- `parallel` Scheduler는 CPU 코어 수만큼만 Thread 생성 (예: 8개)
- 모든 Thread가 블로킹되면 다른 작업 처리 불가
- Event Loop가 멈춰서 전체 애플리케이션이 느려짐

#### 올바른 사용 - boundedElastic에서 Blocking

```kotlin
// 올바른 방법: boundedElastic Scheduler 사용
Flux.range(1, 100)
  .flatMap { n ->
    Mono.fromCallable {
      Thread.sleep(100)  // Blocking OK
      n * 2
    }.subscribeOn(Schedulers.boundedElastic())
  }
// 결과: Blocking 작업이 격리되어 다른 작업에 영향 없음
```

**왜 괜찮은가?**
- `boundedElastic`은 Thread를 동적으로 생성 (최대 CPU * 10개)
- Blocking 작업이 많아도 새 Thread 생성으로 대응
- TTL 60초로 유휴 Thread 자동 정리

### 실수 방지

```kotlin
// 절대 하지 말 것
Schedulers.parallel() + Thread.sleep()  // parallel Thread 블로킹!

// 올바른 방법
Schedulers.boundedElastic() + Thread.sleep()  // 격리됨

// 더 좋은 방법
Mono.delay(Duration.ofSeconds(1))  // Non-blocking delay
```

**핵심 원칙:**
- **Blocking 코드가 있다면 무조건 `boundedElastic`**
- **CPU 집약적 계산은 `parallel`**
- **확실하지 않다면 `boundedElastic`이 안전**

## Cold vs Hot Stream

### Cold Stream - 구독 시마다 새로 시작
```kotlin
val cold = Flux.range(1, 3)

// 첫 번째 구독자
cold.subscribe { value -> log.info("구독자1: $value") }
// 출력: 1, 2, 3

Thread.sleep(100)

// 두 번째 구독자 (처음부터 다시 시작)
cold.subscribe { value -> log.info("구독자2: $value") }
// 출력: 1, 2, 3
```

### Hot Stream - 모든 구독자가 공유
```kotlin
val hot = Flux.interval(Duration.ofMillis(100))
  .take(10)
  .publish() // ConnectableFlux로 변환
  .autoConnect(1) // 첫 번째 구독자가 연결되면 자동 시작

// 첫 번째 구독자 (즉시 시작)
hot.subscribe { value -> log.info("구독자1: $value") }

// 300ms 후 두 번째 구독자 (중간부터 수신)
Thread.sleep(350)
hot.subscribe { value -> log.info("구독자2: $value (중간부터)") }
// 구독자2는 현재 emit되는 값부터만 수신
```

### ConnectableFlux - Hot Stream 제어

`publish()`는 `ConnectableFlux`를 반환합니다. 이는 **명시적 시작 신호**가 필요한 특수한 Flux입니다.

#### 문제 상황

```kotlin
// autoConnect 없이 사용하면?
val hot = Flux.interval(Duration.ofMillis(100))
  .publish() // ConnectableFlux 생성

hot.subscribe { println("구독자1: $it") }
Thread.sleep(1000)
// 출력: 아무것도 없음! (upstream이 시작 안 됨)
```

**왜?**
- `ConnectableFlux`는 구독만으로는 시작되지 않음
- upstream(Flux.interval)이 실행되지 않아 데이터 발행 안 됨
- 모든 구독자는 대기 상태로만 있음

#### 해결 방법

| 방법                 | 설명                    | 사용 시점              |
|--------------------|-----------------------|--------------------|
| **autoConnect(n)** | n명의 구독자가 오면 자동 시작     | 가장 일반적인 Hot Stream |
| **connect()**      | 수동으로 시작               | 모든 구독자를 먼저 등록 후 시작 |
| **refCount(n)**    | n명 이상일 때만 유지, 0명이면 중단 | 리소스 절약이 중요한 경우     |

#### 1. autoConnect(n) - 자동 연결

```kotlin
val hot = Flux.interval(Duration.ofMillis(100))
  .publish()
  .autoConnect(1) // 첫 번째 구독자가 오면 자동 시작

hot.subscribe { println("A: $it") }
// A가 구독하는 순간 upstream 시작!

Thread.sleep(300)
hot.subscribe { println("B: $it") }
// B는 중간부터 수신 (A: 0,1,2 이미 발행된 후)

// 출력:
// A: 0
// A: 1
// A: 2
// A: 3, B: 3  <- B는 3부터 시작
// A: 4, B: 4
```

**특징:**
- 한번 시작되면 **구독자가 없어도 계속 실행**
- 구독자 수를 지정 가능 (ex: autoConnect(2) = 2명이 와야 시작)

#### 2. connect() - 수동 연결

```kotlin
val hot = Flux.interval(Duration.ofMillis(100))
  .publish()

// 모든 구독자를 먼저 등록
hot.subscribe { println("A: $it") }
hot.subscribe { println("B: $it") }
hot.subscribe { println("C: $it") }

// 이제 시작! 모든 구독자가 동시에 수신 시작
hot.connect()

// 출력: A, B, C가 모두 0부터 동시에 수신
// A: 0, B: 0, C: 0
// A: 1, B: 1, C: 1
```

**특징:**
- 모든 구독자를 먼저 등록한 후 **공정하게 동시 시작** 가능
- 시작 시점을 완전히 제어 가능

#### 3. refCount(n) - 구독자 수 기반 자동 관리

```kotlin
val hot = Flux.interval(Duration.ofMillis(100))
  .publish()
  .refCount(1) // 1명 이상이면 시작, 0명이면 중단

val disposable1 = hot.subscribe { println("A: $it") }
// A가 구독 -> 시작

Thread.sleep(300)
val disposable2 = hot.subscribe { println("B: $it") }
// B가 중간부터 수신

disposable1.dispose() // A 구독 해제
// B만 남았으므로 계속 실행

disposable2.dispose() // B 구독 해제
// 0명 -> upstream 자동 중단! (리소스 절약)
```

**특징:**
- 구독자가 **0명이 되면 자동 중단** (autoConnect는 계속 실행)
- 다시 구독하면 **처음부터 재시작** (새로운 Cold Stream처럼)
- 리소스 절약에 유용 (DB 커넥션, WebSocket 등)

#### autoConnect vs refCount 비교

| 구분        | autoConnect(n)    | refCount(n)         |
|-----------|-------------------|---------------------|
| **시작 조건** | n명의 구독자 필요        | n명의 구독자 필요          |
| **중단 조건** | 없음 (계속 실행)        | 구독자 0명이 되면 중단       |
| **재시작**   | 불가 (한번 시작하면 계속)   | 가능 (다시 구독 시 처음부터)   |
| **리소스**   | 구독자 없어도 계속 소비     | 구독자 없으면 해제          |
| **사용 사례** | 이벤트 브로드캐스트, 주식 시세 | WebSocket 연결, DB 폴링 |

#### 실전 예제

```kotlin
// 주식 시세 - autoConnect (항상 실행)
val stockPrice = fetchStockPriceStream()
  .publish()
  .autoConnect(1) // 첫 구독자 오면 시작, 이후 계속 실행

stockPrice.subscribe { log.info("UI: $it") }
stockPrice.subscribe { log.info("Logger: $it") }
// UI가 꺼져도 Logger는 계속 수신

// WebSocket 채팅방 - refCount (참여자 없으면 중단)
val chatRoom = connectToWebSocket()
  .publish()
  .refCount(1) // 1명 이상일 때만 연결 유지

val user1 = chatRoom.subscribe { log.info("User1: $it") }
val user2 = chatRoom.subscribe { log.info("User2: $it") }

user1.dispose()
user2.dispose()
// 모두 나감 -> WebSocket 자동 disconnect (리소스 절약)
```

## Context 전파 (ThreadLocal 대체)

**문제:**
- Reactive는 Thread가 계속 바뀜 (Scheduler에 의해)
- ThreadLocal은 특정 Thread에 종속 → 동작 안 함

**해결: Context API 사용**
```kotlin
val mono = Mono.deferContextual { ctx ->
  val userId = ctx.get<String>("userId")
  processUser(userId)
}.contextWrite(Context.of("userId", "user123"))

// Context는 불변이므로 chain에서 수정 가능
val modified = Mono.deferContextual { ctx ->
  val userId = ctx.get<String>("userId")
  val role = ctx.get<String>("role")
  Mono.just("$userId-$role")
}
  .contextWrite(Context.of("role", "admin"))
  .contextWrite(Context.of("userId", "user456"))
// "user456-admin"
```

## Backpressure

**문제**: Publisher의 발행 속도가 Subscriber의 처리 속도보다 빠를 때 발생

```
Publisher: 초당 1000개 발행
  ↓
Subscriber: 초당 100개 처리
  → 900개는 어떻게?
```

### 종류

#### 1. onBackpressureBuffer - 버퍼에 저장

```kotlin
val flux = Flux.range(1, 10)
  .onBackpressureBuffer(15) // 충분한 버퍼 크기
  .delayElements(Duration.ofMillis(50))
```

**특징**:
- 처리 못한 아이템을 버퍼에 임시 저장
- 메모리 사용
- 버퍼 가득 차면 기본적으로 `OverflowException` 발생

##### 버퍼 오버플로우 전략

| 전략              | 동작                   | 사용 시점                     |
|-----------------|----------------------|---------------------------|
| **ERROR (기본)**  | OverflowException 발생 | 데이터 손실 불가 (결제, 주문)        |
| **DROP_LATEST** | 최신 아이템 버림            | 이전 데이터가 더 중요 (로그, 히스토리)   |
| **DROP_OLDEST** | 가장 오래된 아이템 버림        | 최신 데이터가 더 중요 (실시간 시세, 센서) |

##### ERROR 전략 (기본)

```kotlin
// 버퍼 부족 시 에러 발생
val flux = Flux.range(1, 100) // 100개 발행
  .onBackpressureBuffer(10) // 버퍼 10개만
  .delayElements(Duration.ofMillis(100))

// 결과: 11번째 아이템부터 OverflowException
```

**사용 사례**: 주문 처리
```kotlin
orderStream
  .onBackpressureBuffer(1000) // ERROR 전략
  .flatMap { order -> processOrder(order) }
  .doOnError { error ->
    if (error is OverflowException) {
      alertOps("주문 처리 시스템 과부하!")
    }
  }
```

##### DROP_OLDEST 전략

```kotlin
val flux = Flux.range(1, 100)
  .onBackpressureBuffer(
    10,
    { droppedValue -> log.warn("버림: $droppedValue") },
    BufferOverflowStrategy.DROP_OLDEST
  )
  .delayElements(Duration.ofMillis(100))

// 동작:
// 버퍼: [1,2,3,4,5,6,7,8,9,10]
// 11 발행 → 1 버림, 버퍼: [2,3,4,5,6,7,8,9,10,11]
// 12 발행 → 2 버림, 버퍼: [3,4,5,6,7,8,9,10,11,12]
// 최종적으로 최신 10개만 유지
```

**사용 사례**: 실시간 주식 시세
```kotlin
stockPriceStream
  .onBackpressureBuffer(
    100,
    { dropped -> log.debug("오래된 시세 버림: $dropped") },
    BufferOverflowStrategy.DROP_OLDEST
  )
  .subscribe { price ->
    updateUI(price) // 항상 최신 시세 표시
  }
```

##### DROP_LATEST 전략

```kotlin
val flux = Flux.range(1, 100)
  .onBackpressureBuffer(
    10,
    { droppedValue -> log.warn("버림: $droppedValue") },
    BufferOverflowStrategy.DROP_LATEST
  )
  .delayElements(Duration.ofMillis(100))

// 동작:
// 버퍼: [1,2,3,4,5,6,7,8,9,10] (가득 참)
// 11 발행 → 11 버림 (최신 것 버림)
// 12 발행 → 12 버림
// 1~10이 처리될 때까지 새로운 아이템은 모두 버려짐
```

**사용 사례**: 로그 수집 (초기 상태가 중요)
```kotlin
logStream
  .onBackpressureBuffer(
    500,
    BufferOverflowStrategy.DROP_LATEST
  )
  .buffer(Duration.ofSeconds(5))
  .flatMap { logs -> sendToLogServer(logs) }
```

##### 버퍼 크기 선정 가이드

```kotlin
// 너무 작은 버퍼 - 빈번한 오버플로우
.onBackpressureBuffer(10)

// 너무 큰 버퍼 - OutOfMemoryError
.onBackpressureBuffer(1_000_000)

// 적절한 크기 계산
val bufferSize = (발행속도 * 처리지연시간) * 안전계수
// 예: 초당 1000개 발행, 처리 지연 2초, 안전계수 1.5
// = 1000 * 2 * 1.5 = 3000

.onBackpressureBuffer(
  3000,
  { dropped -> metrics.increment("dropped") },
  BufferOverflowStrategy.DROP_OLDEST
)
```

#### 2. onBackpressureDrop - 처리 못하는 아이템 드롭

```kotlin
val flux = Flux.range(1, 100)
  .onBackpressureDrop { value ->
    log.warn("드롭: $value")
  }
  .delayElements(Duration.ofMillis(10))
```

**특징**:
- 버퍼 없음, 즉시 드롭
- 메모리 안전
- 데이터 손실 허용 가능한 경우

#### 3. onBackpressureLatest - 최신 아이템만 유지

```kotlin
val flux = Flux.range(1, 10)
  .onBackpressureLatest()
  .delayElements(Duration.ofMillis(50))
```

**특징**:
- 1개 슬롯만 유지 (가장 최신 값)
- 처리 중일 때 새로운 값이 오면 대기 중인 값을 교체
- 최신 상태만 중요한 경우 (UI 업데이트, 센서 값)

## 주의사항

### 1. 메모리 누수
```kotlin
// 안티패턴 - 무한 스트림을 모두 메모리에 적재
Flux.interval(Duration.ofSeconds(1))
  .collectList() // OutOfMemoryError!
  .subscribe()

// 권장 - 윈도우, 버퍼, 제한 사용
Flux.interval(Duration.ofSeconds(1))
  .take(100) // 100개만
  .buffer(10) // 10개씩 처리
  .subscribe()
```

### 2. Reactor와 ThreadLocal
- ThreadLocal은 동작하지 않음
- Context API를 사용할 것

### 3. Blocking 코드는 반드시 격리
- `boundedElastic` Scheduler 사용
- `Mono.fromCallable` + `subscribeOn` 패턴

## 테스트 (StepVerifier)

### 의존성
```kotlin
testImplementation("io.projectreactor:reactor-test")
```

### 기본 검증
```kotlin
@Test
fun monoTest() {
  val mono = Mono.just("Hello")

  StepVerifier.create(mono)
    .expectNext("Hello")
    .verifyComplete()
}

@Test
fun fluxTest() {
  val flux = Flux.just("A", "B", "C")

  StepVerifier.create(flux)
    .expectNext("A", "B", "C")
    .verifyComplete()
}
```

### 에러 검증
```kotlin
@Test
fun errorTest() {
  val mono = Mono.error<String>(RuntimeException("Error"))

  StepVerifier.create(mono)
    .expectError(RuntimeException::class.java)
    .verify()
}

@Test
fun errorMessageTest() {
  val mono = Mono.error<String>(IllegalArgumentException("Invalid"))

  StepVerifier.create(mono)
    .expectErrorMatches { error ->
      error is IllegalArgumentException && error.message == "Invalid"
    }
    .verify()
}
```

### 시간 검증 - Virtual Time
```kotlin
@Test
fun virtualTimeTest() {
  // Virtual Time 사용 (실제 대기 안 함)
  StepVerifier.withVirtualTime {
    Flux.interval(Duration.ofHours(1)).take(3)
  }
    .expectSubscription()
    .thenAwait(Duration.ofHours(3)) // 가상 시간 3시간 경과
    .expectNext(0L, 1L, 2L)
    .verifyComplete()
}
```

### Backpressure 검증
```kotlin
@Test
fun backpressureTest() {
  val flux = Flux.range(1, 100)

  StepVerifier.create(flux, 10) // 처음 10개만 요청
    .expectNextCount(10)
    .thenRequest(10) // 추가로 10개 요청
    .expectNextCount(10)
    .thenCancel() // 취소
    .verify()
}
```

### Context 검증
```kotlin
@Test
fun contextTest() {
  val mono = Mono.deferContextual { ctx ->
    val userId = ctx.get<String>("userId")
    Mono.just("User: $userId")
  }

  StepVerifier.create(mono)
    .expectAccessibleContext()
    .contains("userId", "user123")
    .then()
    .expectNext("User: user123")
    .verifyComplete()
}
```

## 검증 팁

### 1. expectNextCount vs expectNext
```kotlin
// 정확한 값 검증이 필요하면
StepVerifier.create(flux)
  .expectNext("A", "B", "C")
  .verifyComplete()

// 개수만 검증하면
StepVerifier.create(flux)
  .expectNextCount(3)
  .verifyComplete()
```

### 2. Virtual Time 사용
```kotlin
// 안티패턴: 실제 10초 대기
@Test
fun slowTest() {
  val mono = Mono.delay(Duration.ofSeconds(10))
  StepVerifier.create(mono)
    .expectNext(0L)
    .verifyComplete()
  // 10초 소요
}

// 권장: Virtual Time (즉시 완료)
@Test
fun fastTest() {
  StepVerifier.withVirtualTime {
    Mono.delay(Duration.ofSeconds(10))
  }
    .expectSubscription()
    .thenAwait(Duration.ofSeconds(10))
    .expectNext(0L)
    .verifyComplete()
  // 즉시 완료
}
```
