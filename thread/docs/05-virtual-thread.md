# Virtual Thread (Java 21+)

## KLT vs ULT

### Kernel-Level Thread (KLT)
- **OS 커널**이 직접 생성, 스케줄링, 관리
- 멀티코어 활용 우수
- **단점**: 생성/컨텍스트 스위칭 비용 높음

### User-Level Thread (ULT)
- **애플리케이션** 레벨에서 관리
- 생성/전환 빠름
- **단점**: 커널이 인지 못함 → 하나 블로킹 시 전체 프로세스 블로킹

### Java의 기존 Thread (Platform Thread)
- **1:1 매핑**: Java Thread = OS Thread (KLT)
- Thread당 1~2MB 메모리
- 수천 개 이상 생성 시 문제

## Virtual Thread 개념

### 핵심 아이디어
**M:N 매핑**: M개의 Virtual Thread → N개의 Platform Thread (Carrier Thread)

```
Virtual Thread (수백만 개 가능)
       ↓
Carrier Thread (Platform Thread, CPU 코어 수만큼)
       ↓
OS Thread
```

### 특징
- JVM이 관리하는 경량 Thread
- 스택 크기 유동적 (수 KB ~ 필요한 만큼)
- I/O 작업 시 자동으로 Carrier Thread에서 마운트/언마운트

### Mount/Unmount 메커니즘

Virtual Thread는 실행될 때만 Platform Thread(Carrier Thread)에 **마운트**됩니다:

```
[실행 중]
Virtual Thread A ──(mounted)──> Carrier Thread 1

[블로킹 발생]
Virtual Thread A (unmounted, 메모리에만 존재)
Carrier Thread 1 (해제) ──> 다른 Virtual Thread 실행 가능

[블로킹 완료]
Virtual Thread A ──(mounted)──> Carrier Thread 1 (or 다른 것)
```

**핵심:**
- Virtual Thread는 블로킹 시 **Unmount** → Carrier Thread 해제
- Carrier Thread는 다른 Virtual Thread를 실행
- 블로킹 완료 시 **다시 Mount** → 실행 재개

### 생성 방법
```kotlin
val thread = Thread.ofVirtual()
    .name("vt-worker")
    .start { println("Virtual Thread") }

val executor = Executors.newVirtualThreadPerTaskExecutor()
executor.submit {
    // task
}
```

## Platform Thread vs Virtual Thread

| 항목     | Platform Thread | Virtual Thread       |
|--------|-----------------|----------------------|
| 매핑     | 1:1 (OS Thread) | M:N (Carrier Thread) |
| 메모리    | 1~2MB           | 수 KB                 |
| 생성 비용  | 높음              | 낮음                   |
| 최대 수   | 수천 개            | 수백만 개                |
| 스케줄링   | OS 커널           | JVM                  |
| 적합한 작업 | CPU-bound       | I/O-bound            |

## Virtual Thread 사용 시점

### 사용해야 할 때 (I/O-bound)
- HTTP 요청 (RESTful API)
- 데이터베이스 쿼리
- 파일 I/O
- 메시지 큐 처리

**예시**: 수천 개의 API 동시 호출
```kotlin
Executors.newVirtualThreadPerTaskExecutor().use { executor ->
    for (i in 0 until 10_000) {
        executor.submit {
            httpClient.get(url) // I/O blocking
        }
    }
}
```

### 사용하면 안 되는 경우

#### 1. CPU-bound 작업
```kotlin
Executors.newVirtualThreadPerTaskExecutor().use { executor ->
    executor.submit {
        // 복잡한 계산 (I/O 없음)
        var sum = 0L
        for (i in 0 until 1_000_000_000) {
            sum += i
        }
    }
}
```
- Virtual Thread도 결국 Platform Thread 위에서 실행
- CPU-bound는 Platform Thread 수만큼만 병렬 처리
- 오버헤드만 증가

#### 2. synchronized 블록 많은 코드
- Pinning 문제 발생 (다음 섹션 참고)

## Pinning 문제

### 정의
Virtual Thread가 Carrier Thread에 **고정**되어 블로킹 시에도 해제되지 않는 현상

### 발생 조건
1. **synchronized 블록 내에서 블로킹 작업**
```kotlin
synchronized(lock) {
    Thread.sleep(1000) // Carrier Thread 1초간 점유
}
```

2. **Native method 호출**

### 영향
- Carrier Thread 고갈 → 다른 Virtual Thread 실행 불가
- Virtual Thread의 장점 상실

### 해결 방법: ReentrantLock 사용
```kotlin
// 안티패턴
synchronized(lock) {
    Thread.sleep(1000) // Pinning 발생
}

// 권장
val lock = ReentrantLock()
lock.lock()
try {
    Thread.sleep(1000) // Virtual Thread는 unmount, Carrier Thread 해제
} finally {
    lock.unlock()
}
```

**왜 ReentrantLock은 Pinning이 없는가?**

```
synchronized:
Virtual Thread A ─(PINNED)─> Carrier Thread 1 (블로킹 중에도 점유)

ReentrantLock:
Virtual Thread A (unmounted, 대기)
Carrier Thread 1 (해제) ─> Virtual Thread B 실행 가능
```

- **synchronized**: JVM이 네이티브 모니터 락을 사용 → Unmount 불가
- **ReentrantLock**: Java 코드로 구현 → JVM이 Unmount 가능

## Thread Pool 사용 시 주의사항

### 안티패턴: Fixed Thread Pool
```kotlin
val executor = Executors.newFixedThreadPool(10) // Platform Thread Pool
Thread.startVirtualThread {
    executor.submit(task) // Virtual Thread 장점 상실
}
```

**문제점:**
- Virtual Thread를 Platform Thread Pool에 제한 → 장점 상실
- Virtual Thread는 Pool 없이 사용하는 것이 Best Practice

### 권장: 동시성 제한은 Semaphore 사용
```kotlin
val semaphore = Semaphore(10) // 동시 10개로 제한

Executors.newVirtualThreadPerTaskExecutor().use { executor ->
    for (task in tasks) {
        executor.submit {
            semaphore.acquire()
            try {
                process(task)
            } finally {
                semaphore.release()
            }
        }
    }
}
```

## 구조화된 동시성 (Structured Concurrency)

### 개념
관련된 작업들을 **하나의 단위**로 취급하여 스레드 누수 방지 및 에러 처리 일관성 보장

### 기존 문제: ExecutorService의 한계

```kotlin
// 안티패턴: 스레드 누수 위험
val executor = Executors.newVirtualThreadPerTaskExecutor()
val user: Future<User> = executor.submit { fetchUser(id) }
val orders: Future<List<Order>> = executor.submit { fetchOrders(id) }

// 문제 1: orders 실패 시 user는 계속 실행됨
// 문제 2: 명시적 shutdown 필요
// 문제 3: 예외 처리 복잡
try {
    return UserProfile(user.get(), orders.get())
} catch (e: Exception) {
    // user, orders 각각 취소해야 함
} finally {
    executor.shutdown()
}
```

### StructuredTaskScope 장점

| 항목     | ExecutorService  | StructuredTaskScope |
|--------|------------------|---------------------|
| 스레드 누수 | 위험 (명시적 cleanup) | 안전 (자동 취소)          |
| 작업 관계  | 암묵적              | 명확한 부모-자식           |
| 예외 전파  | 수동 처리            | 자동 전파/취소            |
| 리소스 관리 | finally 블록       | use 블록              |

### ShutdownOnFailure: 모든 결과 필요 (Invoke All)

**사용 사례:** 모든 작업이 성공해야 할 때

```kotlin
StructuredTaskScope.ShutdownOnFailure().use { scope ->
    val userTask = scope.fork { fetchUser(id) }
    val ordersTask = scope.fork { fetchOrders(id) }
    val statsTask = scope.fork { fetchStats(id) }

    scope.join()           // 모든 작업 완료 대기
    scope.throwIfFailed()  // 하나라도 실패 시 예외

    return UserProfile(
        userTask.get(),
        ordersTask.get(),
        statsTask.get()
    )
}
```

**동작 방식:**
1. 3개 작업 모두 병렬 실행
2. `orders` 실패 시 → 즉시 `user`, `stats` 취소
3. `throwIfFailed()` → 예외 던짐
4. use 블록 종료 시 → 모든 스레드 정리

### ShutdownOnSuccess: 첫 성공 결과 (Invoke Any)

**사용 사례:** 가장 빠른 결과만 필요할 때 (Fallback, Racing)

```kotlin
StructuredTaskScope.ShutdownOnSuccess<String>().use { scope ->
    scope.fork { fetchFromPrimary() }   // 주 DB
    scope.fork { fetchFromCache() }     // 캐시
    scope.fork { fetchFromBackup() }    // 백업 DB

    scope.join()
    return scope.result() // 가장 먼저 성공한 결과
}
```

**동작 방식:**
1. 3개 작업 병렬 실행
2. `cache`가 먼저 성공 → 나머지 즉시 취소
3. `result()` → 성공한 값 반환

### 실전 예제: 데이터 통합

```kotlin
try {
  StructuredTaskScope.ShutdownOnFailure().use { scope ->
    val userTask = scope.fork { fetchData("User", 500) }
    val orderTask = scope.fork { fetchData("Order", 1000, shouldFail = true) }
    val statsTask = scope.fork { fetchData("Stats", 1500) }

    scope.join()
    scope.throwIfFailed()

    return Triple(
      userTask.get(),
      orderTask.get(),
      statsTask.get()
    )
  }
} catch (e: ExecutionException) {
  log.error("작업 실패: ${e.cause?.message}")
  //error handling
}
```

### 커스텀 Shutdown 정책

**사용 사례:** 일부 실패 허용, 성공한 결과만 수집

```kotlin
class PartialSuccessScope<T> : StructuredTaskScope<T>() {
    private val results = Collections.synchronizedList(mutableListOf<T>())

    override fun handleComplete(subtask: Subtask<out T>) {
        if (subtask.state() == Subtask.State.SUCCESS) {
            results.add(subtask.get())
        }
    }

    fun getResults(): List<T> {
        return Collections.unmodifiableList(results)
    }
}

PartialSuccessScope<String>().use { scope ->
    scope.fork { fetchFromService1() }
    scope.fork { fetchFromService2() }
    scope.fork { fetchFromService3() }

    scope.join()
    val results = scope.getResults()
}
```

### Virtual Thread와의 시너지

**대규모 작업도 안전하게 관리:**

```kotlin
StructuredTaskScope.ShutdownOnFailure().use { scope ->
    for (i in 0 until 10_000) {
        scope.fork { processTask(i) }
    }

    scope.join()
    scope.throwIfFailed() // 하나 실패 시 9,999개 즉시 취소
}
```

- Virtual Thread: 수백만 개 생성 가능
- Structured Concurrency: 안전하게 관리
- 조합: 대규모 병렬 처리 실용화

## ThreadLocal과 ScopedValue

Virtual Thread 환경에서 ThreadLocal을 사용할 때 메모리 부담이 클 수 있습니다. Java 21+에서는 **ScopedValue**를 통해 이 문제를 해결할 수 있습니다.

**주요 특징:**
- **불변**: 설정 후 변경 불가
- **자동 정리**: 범위 벗어나면 자동 제거
- **자동 전파**: Virtual Thread와 Structured Concurrency에 자동 전파

**간단한 예제:**
```kotlin
companion object {
    private val CURRENT_USER: ScopedValue<User> = ScopedValue.newInstance()
}

fun handleRequest(user: User) {
    ScopedValue.runWhere(CURRENT_USER, user) {
        // StructuredTaskScope와 함께 사용 시 자동 전파
        StructuredTaskScope.ShutdownOnFailure().use { scope ->
            scope.fork {
                val user = CURRENT_USER.get() // 자동 전파됨
                processTask(user)
            }
            scope.join()
        }
    }
}
```

> **자세한 내용은 [06-thread-local.md](./06-thread-local.md) 참고**
> - ThreadLocal vs ScopedValue 비교
> - 메모리 누수 방지 방법
> - 마이그레이션 가이드
> - 실무 활용 가이드

## 모니터링 및 튜닝

### Pinning 감지

#### -Djdk.tracePinnedThreads (권장하지 않음)
```bash
-Djdk.tracePinnedThreads=short  # 또는 full
```

**문제점:**
- JVM 크래시 발생 가능 (SIGSEGV)
- 디버거와 함께 사용 시 더 불안정
- Java 21에서 여러 버그 보고됨 (JDK-8322846)
- 향후 제거 예정

#### JFR (Java Flight Recorder) 사용 권장

**1. Gradle 테스트 설정 (build.gradle.kts)**
```kotlin
tasks.withType<Test> {
    jvmArgs(
        "-XX:StartFlightRecording=filename=pinning.jfr,settings=profile",
        "-XX:FlightRecorderOptions=stackdepth=256"
    )
}
```

**2. Pinning 테스트 실행**
```bash
./gradlew test

./gradlew test --tests tutorials.thread.basic.VirtualThreadTest.pinningTest
```

**3. JFR 파일 분석**
```bash
# 커맨드라인
jfr print --events jdk.VirtualThreadPinned pinning.jfr

# GUI (JDK Mission Control)
jmc pinning.jfr
```

```text
// jfr print --events jdk.VirtualThreadPinned pinning.jfr

jdk.VirtualThreadPinned {VirtualThreadPinned pinning.jfr
  startTime = 11:32:42.657
  duration = 1.001 s
  eventThread = "" (javaThreadId = 35)
  stackTrace = [
    java.lang.VirtualThread.parkOnCarrierThread(boolean, long) line: 689
    java.lang.VirtualThread.parkNanos(long) line: 648
    java.lang.VirtualThread.sleepNanos(long) line: 807
    java.lang.Thread.sleep(long) line: 507
    tutorials.thread.basic.VirtualThreadTest$PinningServiceImpl.process() line: 52
    ...
  ]
}

jdk.VirtualThreadPinned {
  startTime = 11:32:42.657
  duration = 1.001 s
  eventThread = "" (javaThreadId = 37)
  stackTrace = [
    java.lang.VirtualThread.parkOnCarrierThread(boolean, long) line: 689
    java.lang.VirtualThread.parkNanos(long) line: 648
    java.lang.VirtualThread.sleepNanos(long) line: 807
    java.lang.Thread.sleep(long) line: 507
    tutorials.thread.basic.VirtualThreadTest$PinningServiceImpl.process() line: 52
    ...
  ]
}
```

**JFR의 장점:**
- 안정적 (크래시 없음)
- 기본적으로 20ms 이상 pinning만 기록
- 프로덕션 환경에서도 사용 가능
- 상세한 스택 트레이스 및 지속 시간 제공

### JFR 이벤트

Virtual Thread 관련 주요 JFR 이벤트:

| 이벤트                             | 설명                   | 기본값      |
|---------------------------------|----------------------|----------|
| `jdk.VirtualThreadStart`        | Virtual Thread 시작    | Disabled |
| `jdk.VirtualThreadEnd`          | Virtual Thread 종료    | Disabled |
| `jdk.VirtualThreadPinned`       | Pinning 발생 (20ms 이상) | Enabled  |
| `jdk.VirtualThreadSubmitFailed` | 스케줄링 실패              | Enabled  |

**추가 이벤트 활성화 방법:**

VirtualThreadStart/End는 기본적으로 비활성화되어 있으며, 오버헤드가 크므로 필요 시에만 활성화 권장.

**방법 1: jcmd로 실행 중 활성화**
```bash
# 테스트를 백그라운드로 실행
./gradlew test --tests tutorials.thread.basic.VirtualThreadTest.structuredConcurrencyTest &

# 프로세스 ID 확인
jps | grep Gradle

# JFR 설정 조회
jcmd <pid> JFR.configure

# 추가 이벤트 활성화된 새 레코딩 시작
jcmd <pid> JFR.start name=vt-monitoring \
  settings=profile \
  filename=vt-monitoring.jfr \
  dumponexit=true \
  -e jdk.VirtualThreadStart=true \
  -e jdk.VirtualThreadEnd=true
```

**방법 2: 커스텀 JFC 파일 사용**
```bash
# 기본 profile 설정 복사
jfr configure --copy-default profile custom-profile.jfc

# custom-profile.jfc 편집하여 다음 이벤트 활성화:
# <event name="jdk.VirtualThreadStart">
#   <setting name="enabled">true</setting>
# </event>

# build.gradle.kts에서 커스텀 설정 사용
tasks.withType<Test> {
    jvmArgs(
        "-XX:StartFlightRecording=filename=vt-monitoring.jfr,settings=custom-profile.jfc"
    )
}
```

### jcmd를 이용한 실시간 모니터링

```bash
# 현재 실행 중인 모든 스레드 덤프 (Virtual + Platform)
jcmd <pid> Thread.dump_to_file -format=json threads.json

# Virtual Thread 통계 확인
jcmd <pid> Thread.print
```

### 스케줄러 튜닝 (고급)

```bash
# Carrier Thread Pool 크기 조정 (기본값: CPU 코어 수)
-Djdk.virtualThreadScheduler.parallelism=16

# 최대 Platform Thread 수 (기본값: 256)
-Djdk.virtualThreadScheduler.maxPoolSize=512

# 최소 실행 가능 스레드 수 (기본값: 1)
-Djdk.virtualThreadScheduler.minRunnable=1
```

**옵션 설명:**
- `parallelism`: ForkJoinPool의 병렬 처리 수준, 실제 Carrier Thread 개수
- `maxPoolSize`: Carrier Thread의 최대 개수 제한 (임시 확장 포함)
- `minRunnable`: 항상 실행 가능 상태로 유지할 최소 스레드 수 (데드락 방지)

**튜닝 주의사항:**
- 대부분의 경우 기본값이 최적
- CPU 코어 수보다 많이 설정해도 성능 향상 없음 (오히려 컨텍스트 스위칭 증가)
- Pinning 문제가 있다면 스케줄러 튜닝보다 **코드 수정** 우선

## Spring Boot 통합

### 설정 (Spring Boot 3.2+, Java 21+)
```properties
spring.threads.virtual.enabled=true
```

### 효과
- Tomcat/Jetty의 요청 처리 Thread가 Virtual Thread로 전환
- 수천 개 동시 요청 처리 가능

### 커스텀 Executor 설정
```java
// Java Spring Configuration
@Configuration
public class ExecutorConfig {

    @Bean
    @ConditionalOnThreading(Threading.VIRTUAL)
    public ExecutorService virtualThreadExecutor() {
        ThreadFactory factory = Thread.ofVirtual()
            .name("api-worker-", 0)
            .factory();
        return Executors.newThreadPerTaskExecutor(factory);
    }

    @Bean
    @ConditionalOnThreading(Threading.PLATFORM)
    public ExecutorService platformThreadExecutor() {
        return Executors.newFixedThreadPool(10);
    }
}
```

### @Async와 함께 사용
```java
// Java Spring Configuration
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
```

## 요약

### Virtual Thread 핵심 개념
- **M:N 매핑**: 수백만 Virtual Thread → CPU 코어 수만큼 Platform Thread
- **경량**: 스택 크기 유동적 (수 KB)
- **자동 스케줄링**: JVM이 자동으로 Mount/Unmount 관리

### 사용 시점
| 작업 유형 | 권장 Thread | 이유 |
|---------|------------|------|
| I/O-bound (HTTP, DB, File) | Virtual Thread | 메모리 효율적, 확장성 우수 |
| CPU-bound (계산 위주) | Platform Thread | CPU 코어 수만큼만 병렬 처리 |
| synchronized 많음 | Platform Thread | Pinning 문제 발생 |

### 주의사항
1. **Pinning 방지**: synchronized 대신 ReentrantLock 사용
2. **Thread Pool 사용 금지**: Virtual Thread는 Pool 없이 사용
3. **동시성 제한**: Pool 대신 Semaphore 사용
4. **모니터링**: JFR로 Pinning 감지 (tracePinnedThreads는 불안정)

### Best Practices
- Structured Concurrency로 스레드 누수 방지
- ScopedValue로 ThreadLocal 대체 (자세한 내용은 [06-thread-local.md](./06-thread-local.md) 참고)
- Spring Boot 3.2+에서는 `spring.threads.virtual.enabled=true`만 설정
- JFR로 Pinning 모니터링 (tracePinnedThreads는 불안정)
