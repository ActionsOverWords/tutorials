# Thread 기본 개념

## Thread vs Process

### 핵심 차이점
- **Process**: 독립된 메모리 공간을 가진 실행 단위, 다른 프로세스의 자원에 직접 접근 불가
- **Thread**: 프로세스 내 실행 단위, 프로세스의 메모리를 공유 (Heap, Code, Data), 각자의 Stack과 Register 보유

### 실무 관점
- Thread 간 통신은 공유 메모리 사용으로 빠름
```text
IPC (Inter Process Communication)
- 프로세스들 사이에 데이터를 주고받는 행위 또는 그 방법이나 경로를 의미
- Process 간 통신은 IPC 필요
```
- Context Switching: Thread가 Process보다 훨씬 빠름
- 단점: 하나의 Thread 문제가 전체 Process에 영향

## Thread vs Runnable vs Callable vs Future

### Runnable
```kotlin
val task = Runnable { println("Running") }
Thread(task).start()
```
- `run()` 메서드: 반환값 없음 (void)
- Checked Exception 던질 수 없음
- Thread 또는 ExecutorService로 실행 가능

### Callable
```kotlin
val task = Callable<Int> {
    42
}
```
- `call()` 메서드: 반환값 있음
- Checked Exception 던질 수 있음
- ExecutorService로만 실행 가능

### Future
```kotlin
val future: Future<Int> = executor.submit(callable)
val result: Int = future.get() // 블로킹
```
- 비동기 작업의 결과를 나타냄
- `get()`: 결과를 기다림 (블로킹)
- `isDone()`: 완료 여부 확인 (논블로킹)
- `cancel()`: 작업 취소

### Thread 직접 상속
```kotlin
class MyThread : Thread() {
    override fun run() { }
}
```
- 권장하지 않음 (상속을 소진)
- Runnable 구현 방식 선호

## Thread 실행 방법

### 1. Thread 직접 생성 및 시작
```kotlin
val thread = Thread {
    println("Running in thread")
}
thread.start()
```

### 2. Runnable을 Thread에 전달
```kotlin
val task = Runnable { println("Task") }
val thread = Thread(task, "worker-thread")
thread.start()
```

### 3. ExecutorService.execute() - 반환값 없음
```kotlin
val executor = Executors.newFixedThreadPool(10)
executor.execute {
    println("Executed")
}
```
- Fire-and-forget 방식
- 결과를 받을 수 없음
- Runnable만 가능

### 4. ExecutorService.submit() - Future 반환
```kotlin
val executor = Executors.newFixedThreadPool(10)

// Runnable 제출
val future1: Future<*> = executor.submit {
    println("Task")
}

// Callable 제출
val future2: Future<Int> = executor.submit(Callable {
    42
})

val result: Int = future2.get()
```
- 결과를 Future로 받을 수 있음
- Runnable 또는 Callable 모두 가능
- 예외 처리 가능

### 5. FutureTask - Callable/Runnable을 Future로 감싸기
```kotlin
// Callable을 FutureTask로 감싸기
val callable = Callable {
    Thread.sleep(1000)
    42
}
val futureTask = FutureTask(callable)

// Thread에 직접 전달 가능 (FutureTask는 Runnable 구현)
val thread = Thread(futureTask)
thread.start()

// 결과 받기
val result: Int = futureTask.get() // 42

// 또는 ExecutorService에 제출
val executor = Executors.newSingleThreadExecutor()
executor.submit(futureTask)
```

**FutureTask의 장점:**
- Callable을 Thread에 직접 전달 가능
- Future 인터페이스 구현으로 결과 조회/취소 가능
- ExecutorService 없이도 비동기 작업 결과 관리

**Runnable을 FutureTask로 감싸기:**
```kotlin
val runnable = Runnable { println("Task") }
val futureTask = FutureTask(runnable, null)

val thread = Thread(futureTask)
thread.start()

futureTask.get() // null 반환, 완료 대기용으로 사용
```

### 6. CompletableFuture - 비동기 체이닝
```kotlin
val future = CompletableFuture
    .supplyAsync {
        "Hello"
    }
    .thenApply { result -> "$result World" }
    .thenApply { it.uppercase() }

val result: String = future.get() // "HELLO WORLD"
```

**CompletableFuture 장점:**
- Non-blocking 체이닝 가능
- 여러 비동기 작업 조합 (thenCombine, allOf, anyOf)
- 예외 처리 편리 (exceptionally, handle)

```kotlin
CompletableFuture.supplyAsync {
    if (Math.random() > 0.5) {
        throw RuntimeException("Error")
    }
    "Success"
}.exceptionally {
    "Fallback value"
}.thenAccept { result ->
    println(result)
}
```

### 7. Virtual Thread (Java 21+)
```kotlin
// 1. Thread.startVirtualThread()
Thread.startVirtualThread {
    println("Virtual thread")
}

// 2. Thread.ofVirtual()
val thread = Thread.ofVirtual()
    .name("vt-worker")
    .start {
        println("Virtual thread")
    }

// 3. Executors.newVirtualThreadPerTaskExecutor()
val executor = Executors.newVirtualThreadPerTaskExecutor()
executor.submit {
    println("Virtual thread task")
}
```

### 실행 방법 선택 가이드

| 방법 | 사용 시점 |
|-----|---------|
| Thread 직접 생성 | 단순 일회성 작업 (비추천) |
| ExecutorService.execute() | 결과가 필요 없는 작업 |
| ExecutorService.submit() | 결과가 필요하거나 예외 처리가 필요한 작업 |
| FutureTask | Callable을 Thread에 직접 전달하거나, ExecutorService 없이 Future 기능 사용 |
| CompletableFuture | 비동기 작업 체이닝, 여러 작업 조합 |
| Virtual Thread | I/O-bound 작업, 대량의 동시 작업 (Java 21+) |

### 실무 권장 사항
- **기본**: ExecutorService.submit() 사용 (Thread Pool 재사용)
- **복잡한 비동기**: CompletableFuture 사용
- **I/O 많은 작업**: Virtual Thread 사용 (Java 21+)
- **Thread 직접 생성**: 가급적 피하기 (자원 낭비)

## Thread State (생명주기)

```
NEW → RUNNABLE ⇄ BLOCKED/WAITING/TIMED_WAITING → TERMINATED
```

### 6가지 상태
1. **NEW**: Thread 생성, start() 호출 전
2. **RUNNABLE**: 실행 중이거나 실행 가능 상태 (OS 스케줄러 대기 포함)
3. **BLOCKED**: 동기화 락 획득 대기
4. **WAITING**: 다른 Thread의 특정 작업 완료 대기 (Object.wait(), Thread.join())
5. **TIMED_WAITING**: 정해진 시간만큼 대기 (Thread.sleep(), wait(timeout))
6. **TERMINATED**: 실행 완료

### 실무 포인트
- Thread dump 분석 시 BLOCKED/WAITING 상태의 Thread가 많으면 성능 문제
- BLOCKED: Lock contention 발생
- WAITING: 작업 간 조율 문제 또는 데드락 가능성

## ThreadGroup

### 개념
- 관련된 Thread들을 논리적으로 그룹화하여 관리
- 계층 구조 지원 (부모-자식 관계)
- 모든 Thread는 반드시 하나의 ThreadGroup에 속함

### JVM 기본 ThreadGroup 계층

```
system (최상위 그룹)
  └─ main (애플리케이션 그룹)
      └─ 사용자 생성 Thread/ThreadGroup
```

**system 그룹:**
- JVM이 자동으로 생성하는 최상위 ThreadGroup
- JVM 내부 Thread 관리 (GC Thread, JIT Compiler Thread 등)
- 일반적으로 직접 접근하지 않음

**main 그룹:**
- system 그룹의 하위 그룹
- main() 메서드를 실행하는 main Thread가 속함
- 사용자가 생성한 Thread는 기본적으로 main 그룹에 속함

```kotlin
// 현재 Thread의 ThreadGroup 확인
val currentThread = Thread.currentThread()
val currentGroup = currentThread.threadGroup

println("Current group: ${currentGroup.name}") // "main"
println("Parent group: ${currentGroup.parent.name}") // "system"

// 최상위 그룹 찾기
var rootGroup = currentGroup
while (rootGroup.parent != null) {
    rootGroup = rootGroup.parent
}
println("Root group: ${rootGroup.name}") // "system"

// system 그룹의 모든 Thread 개수
println("System threads: ${rootGroup.activeCount()}")
```

**ThreadGroup 지정하지 않은 경우:**
```kotlin
// ThreadGroup을 명시하지 않으면 현재 Thread의 그룹에 속함
val thread = Thread {
    println(Thread.currentThread().threadGroup.name)
}
thread.start() // "main" 출력

// ExecutorService도 동일
val executor = Executors.newFixedThreadPool(2)
executor.submit {
    println(Thread.currentThread().threadGroup.name)
} // "main" 출력
```

**Virtual Thread의 ThreadGroup:**
```kotlin
// Virtual Thread는 별도의 ThreadGroup 없음 (null)
val vt = Thread.startVirtualThread {
    val group = Thread.currentThread().threadGroup
    println(group) // null
}
```

### 기본 사용법

```kotlin
// ThreadGroup 생성
val group = ThreadGroup("worker-group")

// ThreadGroup에 Thread 추가
val t1 = Thread(group, {
    println("Thread in group")
}, "worker-1")

val t2 = Thread(group, {
    println("Another thread")
}, "worker-2")

t1.start()
t2.start()

// 그룹 정보 조회
println("Active threads: ${group.activeCount()}")
group.list() // 그룹 내 모든 Thread 출력
```

### 계층 구조

```kotlin
// 부모 그룹
val parent = ThreadGroup("parent-group")

// 자식 그룹
val child = ThreadGroup(parent, "child-group")

val thread = Thread(child, {
    println("In child group")
})
thread.start()

// 부모 그룹 조회
println(child.parent.name) // "parent-group"
```

### 주요 메서드

```kotlin
val group = ThreadGroup("task-group")

// 활성 Thread 수
val count = group.activeCount()

// 그룹 내 모든 Thread 가져오기
val threads = arrayOfNulls<Thread>(group.activeCount())
group.enumerate(threads)

// 그룹 내 모든 Thread 중단 (deprecated)
// group.stop()  // 사용 금지

// 그룹의 최대 우선순위 설정
group.maxPriority = Thread.NORM_PRIORITY
```

### 실무 관점

**과거 용도:**
- Thread 일괄 관리 (일괄 중단, 우선순위 설정)
- 보안 정책 적용 (SecurityManager와 함께)
- 디버깅 및 모니터링

**현재 상황:**
- 실무에서 거의 사용하지 않음
- 주요 메서드가 deprecated (stop, suspend, resume)
- 더 나은 대안들이 존재

**권장 대안:**

```kotlin
// 1. ExecutorService 사용 (권장)
val executor = Executors.newFixedThreadPool(
    10,
    ThreadFactoryBuilder()
        .setNameFormat("worker-%d")
        .build()
)

// 모든 작업 종료
executor.shutdown()
executor.awaitTermination(10, TimeUnit.SECONDS)

// 2. 직접 Thread 관리
val threads = CopyOnWriteArrayList<Thread>()
threads.forEach { thread ->
    if (thread.isAlive) {
        thread.interrupt()
    }
}
```

### ThreadGroup을 써야 하는 경우
- Legacy 코드 유지보수
- Thread 계층 구조가 명시적으로 필요한 특수한 경우
- 간단한 학습/테스트 목적

### 주의사항
- `stop()`, `suspend()`, `resume()` 메서드는 deprecated (사용 금지)
- 실무에서는 ExecutorService + ThreadFactory 조합 사용
- ThreadGroup의 예외 처리 메서드(`uncaughtException`)는 여전히 유용할 수 있음

```kotlin
// 예외 처리 커스터마이징
val group = object : ThreadGroup("error-handler") {
    override fun uncaughtException(t: Thread, e: Throwable) {
        System.err.println("Exception in ${t.name}: ${e.message}")
        // 로깅, 알림 등 처리
    }
}
```

## Daemon Thread

### 개념
- JVM은 모든 non-daemon thread가 종료되면 프로그램 종료
- Daemon thread는 background 작업용 (GC, 모니터링 등)

```kotlin
val thread = Thread(task)
thread.isDaemon = true // start() 전에 설정
thread.start()
```

### 주의사항
- Daemon thread에서 중요한 I/O 작업 금지 (갑작스런 종료 가능)
- main thread 종료 시 daemon thread는 즉시 종료

## Worker Thread vs Daemon Thread

### 핵심 차이

| 구분 | Worker Thread (Non-Daemon) | Daemon Thread |
|-----|---------------------------|---------------|
| 종료 조건 | JVM이 종료를 기다림 | JVM 종료 시 즉시 종료 |
| 기본값 | `setDaemon(false)` (기본) | `setDaemon(true)` |
| 용도 | 핵심 비즈니스 로직 | 보조 작업 (GC, 모니터링, 로깅) |
| 종료 보장 | 작업 완료까지 실행 보장 | 강제 종료 가능 |

### 동작 예시

```kotlin
// Worker Thread: main 종료 후에도 계속 실행
val worker = Thread {
    for (i in 0..<5) {
        println("Worker: $i")
        Thread.sleep(1000)
    }
}
worker.isDaemon = false // 기본값
worker.start()

// Daemon Thread: main 종료 시 함께 종료
val daemon = Thread {
    for (i in 0..<100) {
        println("Daemon: $i")
        Thread.sleep(1000)
    }
}
daemon.isDaemon = true
daemon.start()

println("Main thread ending...")
// Worker는 5번 모두 출력
// Daemon은 main 종료 시 중단 (100번 출력 안 됨)
```

### 실무 가이드

**Worker Thread 사용:**
- 사용자 요청 처리 (API 호출, DB 쿼리)
- 파일 저장, 데이터 전송 등 완료가 중요한 작업
- 트랜잭션 처리

**Daemon Thread 사용:**
- 메모리 정리, 캐시 갱신
- 헬스 체크, 메트릭 수집
- 백그라운드 로깅

### 주의사항
- Thread Pool(ExecutorService)의 Thread는 기본적으로 non-daemon
- Virtual Thread는 자동으로 daemon 설정됨 (Java 21+)
- Daemon thread 내에서 중요한 자원 정리 코드 작성 금지

## Thread 이름 지정

### 실무 필수
```kotlin
val thread = Thread.ofVirtual()
    .name("order-processor-", 0) // 순차 번호 자동 부여
    .start(task)
```

### 중요성
- Thread dump 분석 시 식별 용이
- 로깅/모니터링에서 문제 추적 용이
- Thread pool 사용 시 ThreadFactory로 이름 패턴 지정

```kotlin
val factory = ThreadFactoryBuilder()
    .setNameFormat("api-worker-%d")
    .setDaemon(false)
    .build()
```
