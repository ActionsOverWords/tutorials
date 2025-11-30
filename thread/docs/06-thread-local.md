# ThreadLocal과 ScopedValue

## ThreadLocal 개념

### 정의
각 Thread가 독립적인 변수 복사본을 가지는 메커니즘

### 기본 사용법
```kotlin
private val currentUser: ThreadLocal<User> = ThreadLocal()

fun handleRequest(user: User) {
    currentUser.set(user)
    try {
        processRequest()
    } finally {
        currentUser.remove() // 필수!
    }
}

fun processRequest() {
    val user = currentUser.get() // 현재 Thread의 user
}
```

### 사용 사례

#### 1. 사용자 컨텍스트 전파
```kotlin
// Spring Security의 SecurityContextHolder
val auth = SecurityContextHolder.getContext().authentication
```

#### 2. 트랜잭션 컨텍스트
```kotlin
// Spring의 @Transactional
TransactionSynchronizationManager.getCurrentTransaction()
```

#### 3. 로깅 컨텍스트 (MDC)
```kotlin
MDC.put("requestId", requestId)
log.info("Processing request") // [requestId=123] Processing request
MDC.clear()
```

#### 4. Thread-unsafe 객체 공유
```kotlin
private val formatter: ThreadLocal<SimpleDateFormat> =
    ThreadLocal.withInitial { SimpleDateFormat("yyyy-MM-dd") }

fun format(date: Date): String {
    return formatter.get().format(date)
}
```

## ThreadLocal 문제점

### 1. Memory Leak

#### 발생 원인
```
Thread → ThreadLocalMap → Entry(WeakReference<ThreadLocal>, value)
```

1. ThreadLocal 키는 **WeakReference**로 참조 → GC 가능
2. 하지만 **value**는 **Strong Reference** → GC 불가
3. Thread가 오래 살아있으면 (Thread Pool) value가 계속 메모리 점유

#### 전형적인 누수 시나리오
```kotlin
// Servlet/Spring Controller에서
val data: ThreadLocal<List<String>> = ThreadLocal()

fun handleRequest() {
    val largeData = loadLargeData() // 100MB
    data.set(largeData)
    // remove() 호출 안 함!
}
```

**문제:**
- Tomcat Worker Thread는 재사용됨 (Thread Pool)
- Thread가 종료되지 않으므로 100MB 계속 점유
- 요청 수만큼 메모리 누수 누적 → OutOfMemoryError

#### 해결 방법

**1) 반드시 remove() 호출**
```kotlin
val currentUser: ThreadLocal<User> = ThreadLocal()

fun handleRequest(user: User) {
    currentUser.set(user)
    try {
        processRequest()
    } finally {
        currentUser.remove() // 필수!
    }
}
```

**2) Filter로 전역 관리**
```kotlin
@WebFilter("/*")
class ThreadLocalCleanupFilter : Filter {
    override fun doFilter(
        request: ServletRequest,
        response: ServletResponse,
        chain: FilterChain
    ) {
        try {
            chain.doFilter(request, response)
        } finally {
            currentUser.remove()
            MDC.clear()
        }
    }
}
```

**3) AutoCloseable 패턴**
```kotlin
class UserContext(user: User) : AutoCloseable {
    companion object {
        private val currentUser: ThreadLocal<User> = ThreadLocal()

        fun get(): User? = currentUser.get()
    }

    init {
        currentUser.set(user)
    }

    override fun close() {
        currentUser.remove()
    }
}

// 사용
UserContext(user).use {
    processRequest()
} // 자동으로 remove 호출
```

#### 메모리 누수 진단

**1) Heap Dump 분석**
```bash
jmap -dump:live,format=b,file=heap.bin <pid>
```
- Eclipse MAT, VisualVM으로 분석
- ThreadLocalMap의 value 크기 확인

**2) Tomcat 경고 메시지**
```
WARNING: A web application appears to have started a thread named [thread-name]
but has failed to stop it. This is very likely to create a memory leak.
```

### 2. Virtual Thread와의 호환성 문제

```kotlin
val threadLocal = ThreadLocal<User>()

threadLocal.set(User("1", "Alice"))
log.info("ThreadLocal 설정: ${threadLocal.get()}")

// Virtual Thread에서 접근 시 전파 안됨
Thread.startVirtualThread {
    log.info("Virtual Thread: ${threadLocal.get()}") // null
}.join()
```

**문제점:**
- Virtual Thread는 부모 Thread의 ThreadLocal 값을 상속받지 않음
- 수백만 개의 Virtual Thread 사용 시 각각 ThreadLocal 복사본 생성 → 메모리 부담

## ScopedValue (Java 21+)

### 개념 및 등장 배경

ThreadLocal의 문제점을 해결하기 위해 Java 21에서 도입된 새로운 메커니즘:

1. **불변성**: 설정 후 변경 불가
2. **명확한 범위**: runWhere 블록 내에서만 유효
3. **자동 정리**: 범위 벗어나면 자동 제거
4. **메모리 효율**: Virtual Thread에 최적화
5. **자동 전파**: 자식 Thread에 자동 상속

> **버전 정보:**
> - JDK 21-24: Preview (--enable-preview 필요)
> - JDK 25+: 정식 지원

### 환경 설정

**build.gradle.kts (JDK 21-24 Preview):**
```kotlin
tasks.withType<Test> {
    jvmArgs("--enable-preview")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjdk-release=21")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
```

**build.gradle.kts (JDK 25+):**
```kotlin
// Preview 플래그 불필요
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjdk-release=25")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}
```

### 기본 사용법

```kotlin
data class User(val id: String, val name: String)

companion object {
    private val CURRENT_USER: ScopedValue<User> = ScopedValue.newInstance()
}

fun handleRequest(user: User) {
    ScopedValue.runWhere(CURRENT_USER, user) {
        // 이 범위 내에서만 user 접근 가능
        processRequest()
    }
    // 여기서는 CURRENT_USER.get() 불가 (NoSuchElementException)
}

private fun processRequest() {
    val user = CURRENT_USER.get()
    log.info("현재 사용자: $user")
    callService()
}

private fun callService() {
    val user = CURRENT_USER.get() // 상위에서 설정한 값 자동 전파
    log.info("서비스 호출 - 사용자: ${user.name}")
}
```

**특징:**
- `runWhere` 블록 내에서만 값 접근 가능
- 블록 종료 시 자동 정리 (remove 불필요)
- 하위 함수에 자동 전파

### 중첩 사용

```kotlin
companion object {
    private val REQUEST_ID: ScopedValue<String> = ScopedValue.newInstance()
    private val CURRENT_USER: ScopedValue<User> = ScopedValue.newInstance()
}

fun handleRequest(requestId: String, user: User) {
    ScopedValue.runWhere(REQUEST_ID, requestId) {
        ScopedValue.runWhere(CURRENT_USER, user) {
            // 둘 다 접근 가능
            log.info("요청 ID: ${REQUEST_ID.get()}")
            log.info("사용자: ${CURRENT_USER.get()}")
        }
    }
}
```

### Virtual Thread와 함께 사용

**주의: 단순 Thread.startVirtualThread 사용 시 전파 안됨**
```kotlin
// 안티패턴: ScopedValue 전파 안 될 수 있음
ScopedValue.runWhere(CURRENT_USER, user) {
    Thread.startVirtualThread {
        val user = CURRENT_USER.get() // NoSuchElementException 가능
    }.join()
}
```

**권장: StructuredTaskScope 사용**
```kotlin
// 권장: StructuredTaskScope로 자동 전파 보장
ScopedValue.runWhere(CURRENT_USER, user) {
    StructuredTaskScope.ShutdownOnFailure().use { scope ->
        scope.fork {
            val user = CURRENT_USER.get() // 자동 전파됨
            processTask(user)
            null
        }
        scope.join()
        scope.throwIfFailed()
    }
}
```

**ExecutorService 사용 시**
```kotlin
ScopedValue.runWhere(CURRENT_USER, user) {
    Executors.newVirtualThreadPerTaskExecutor().use { executor ->
        val futures = (1..3).map { i ->
            executor.submit<String> {
                // Virtual Thread에 자동 전파됨
                val user = CURRENT_USER.get()
                "Task $i - ${user.name}"
            }
        }
        futures.forEach { log.info(it.get()) }
    }
}
```

### 실전 예제: 여러 Virtual Thread에서 독립적인 Context

```kotlin
fun processMultipleUsers() {
    val users = listOf(
        User("1", "Alice"),
        User("2", "Bob"),
        User("3", "Charlie")
    )

    Executors.newVirtualThreadPerTaskExecutor().use { executor ->
        users.forEach { user ->
            executor.submit {
                // 각 Virtual Thread마다 독립적인 ScopedValue
                ScopedValue.runWhere(CURRENT_USER, user) {
                    log.info("[${Thread.currentThread()}] 요청 시작 - ${CURRENT_USER.get()}")

                    Thread.sleep(100)

                    nestedCall()

                    log.info("[${Thread.currentThread()}] 요청 완료 - ${CURRENT_USER.get()}")
                }
            }
        }
    }
}

private fun nestedCall() {
    val user = CURRENT_USER.get()
    log.info("  nestedCall - 현재 사용자: ${user.name}")
}
```

## ThreadLocal vs ScopedValue

| 항목 | ThreadLocal | ScopedValue |
|-----|------------|-------------|
| **가변성** | set()로 변경 가능 | 불변 (설정 후 변경 불가) |
| **생명주기** | 명시적 remove 필요 | 범위 벗어나면 자동 제거 |
| **메모리 누수** | 위험 높음 (Thread Pool) | 위험 낮음 (자동 정리) |
| **Virtual Thread** | 메모리 부담 큼 | 최적화됨 |
| **전파 방식** | 명시적 전파 필요 | 자동 전파 |
| **사용 시점** | Java 20 이하, 가변 필요 | Java 21+, 신규 코드 |
| **지원 버전** | 모든 Java 버전 | JDK 21+ (21-24 Preview, 25+ GA) |
| **Structured Concurrency** | 별도 처리 필요 | 자동 통합 |

## 실무 가이드

### ThreadLocal 사용 시 체크리스트
- [ ] finally 블록에서 remove() 호출하는가?
- [ ] Thread Pool 환경에서 사용하는가? (누수 위험 높음)
- [ ] 대용량 객체를 저장하는가? (메모리 부담)
- [ ] InheritableThreadLocal이 필요한가? (자식 Thread 전파)
- [ ] Virtual Thread 환경인가? (ScopedValue 고려)

### ThreadLocal → ScopedValue 마이그레이션

**Before: ThreadLocal**
```kotlin
private val currentUser: ThreadLocal<User> = ThreadLocal()

fun process(user: User) {
    currentUser.set(user)
    try {
        doWork()
    } finally {
        currentUser.remove()
    }
}
```

**After: ScopedValue (Java 21+)**
```kotlin
private val CURRENT_USER: ScopedValue<User> = ScopedValue.newInstance()

fun process(user: User) {
    ScopedValue.runWhere(CURRENT_USER, user) {
        doWork()
    }
}
```

### 언제 각각 사용하는가?

**ThreadLocal 사용:**
- Java 21 미만 환경
- 값이 동적으로 변경되어야 하는 경우
- 레거시 코드 유지보수
- InheritableThreadLocal이 필요한 경우

**ScopedValue 사용:**
- Java 21 이상 환경
- 값이 불변인 경우 (대부분의 컨텍스트)
- Virtual Thread 사용 환경
- Structured Concurrency 사용
- 신규 프로젝트

## 주요 개념 정리

### ThreadLocal 메모리 누수

**발생 메커니즘:**
```
Thread (살아있음)
  └─ ThreadLocalMap
      └─ Entry
          ├─ key: WeakReference<ThreadLocal> → GC 가능
          └─ value: Strong Reference → GC 불가 (누수!)
```

**해결:**
```kotlin
try {
    threadLocal.set(value)
    process()
} finally {
    threadLocal.remove() // 필수!
}
```

### ThreadLocal 주요 사용 사례

| 사용 사례 | 예시 | 주의사항 |
|---------|------|---------|
| 사용자 컨텍스트 | SecurityContextHolder | remove() 필수 |
| 트랜잭션 컨텍스트 | TransactionSynchronizationManager | Spring이 자동 관리 |
| MDC (로깅) | Log4j, Logback | 요청 종료 시 clear() |
| Thread-unsafe 객체 | SimpleDateFormat | DateTimeFormatter 권장 |

### 전파 방식 비교

**ThreadLocal:**
- 기본적으로 전파 안 됨
- InheritableThreadLocal: 자식 생성 시 복사 (일회성)
- Platform Thread 전용

**ScopedValue:**
- 모든 하위 스코프에 자동 전파
- Virtual Thread 포함
- Structured Concurrency와 통합
- 불변으로 안전

### 성능 특성

**ThreadLocal:**
- Thread당 별도 복사본 → Virtual Thread 많으면 메모리 부담
- get/set 빠름 (HashMap 조회)
- remove 누락 시 누수

**ScopedValue:**
- Virtual Thread에 최적화된 내부 구현
- 불변으로 복사 비용 없음
- 범위 기반 자동 정리
- get 성능: ThreadLocal과 유사

### 마이그레이션 전략

**1단계: 평가**
- Java 버전 확인 (21+ 필요)
- ThreadLocal 사용 패턴 분석
- 값이 불변인지 확인

**2단계: 단계적 전환**
```kotlin
// 호환성 레이어 (과도기)
object UserContext {
    private val scopedValue: ScopedValue<User> = ScopedValue.newInstance()
    private val threadLocal: ThreadLocal<User> = ThreadLocal()

    fun runWith(user: User, block: () -> Unit) {
        if (isJava21Plus()) {
            ScopedValue.runWhere(scopedValue, user, block)
        } else {
            threadLocal.set(user)
            try {
                block()
            } finally {
                threadLocal.remove()
            }
        }
    }

    fun get(): User? {
        return if (isJava21Plus()) {
            scopedValue.orElse(null)
        } else {
            threadLocal.get()
        }
    }
}
```

**3단계: 완전 전환**
- Java 21 이상 확정 시 ThreadLocal 제거
- 호환성 레이어 제거
- ScopedValue만 사용