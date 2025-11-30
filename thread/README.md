# Java Thread 학습 가이드

## 목차

### 1. [Thread 기본 개념](docs/01-thread-basics.md)
- Thread vs Process
- Thread vs Runnable vs Callable vs Future
- Thread State (생명주기)
- Daemon Thread
- Thread 이름 지정

### 2. [Thread 동시성 문제](docs/02-thread-issues.md)
- Race Condition & Critical Section
- volatile과 가시성 문제
- happens-before 관계
- Deadlock 4가지 조건과 예방

### 3. [동기화 메커니즘](docs/03-synchronization.md)
- Mutex (ReentrantLock)
- Semaphore
- Monitor (synchronized)
- synchronized vs ReentrantLock
- Fair vs Unfair Lock
- ReadWriteLock

### 4. [Thread Pool](docs/04-thread-pool.md)
- ExecutorService 종류
- ThreadPoolExecutor 설정
- 적절한 Pool Size 계산
- RejectedExecutionHandler 전략
- ForkJoinPool

### 5. [Virtual Thread](docs/05-virtual-thread.md) (Java 21+)
- KLT vs ULT
- Virtual Thread 개념
- Platform Thread vs Virtual Thread
- Pinning 문제와 해결
- Spring Boot 통합
- 구조화된 동시성

### 6. [ThreadLocal과 Scope Value](docs/06-thread-local.md)
- ThreadLocal 개념과 사용 사례
- Memory Leak 문제와 해결
- Scope Value (Java 21+)
- ThreadLocal vs ScopedValue

### 7. [Concurrent Collections](docs/07-concurrent-collections.md)
- ConcurrentHashMap
- CopyOnWriteArrayList
- BlockingQueue
- ConcurrentLinkedQueue

### 8. [Resilience4j](docs/08-resilience4j.md)
- Circuit Breaker
- Rate Limiter
- Bulkhead
- Retry
- 패턴 조합 및 Spring Boot 통합

### 9. [Spring Boot와 Thread](docs/09-spring.md)
- @Async 비동기 메서드 실행
- TaskExecutor 설정
- @Scheduled 스케줄링
- Virtual Thread 통합
- 실무 패턴

### 10. [NIO Select Loop](docs/10-nio-select-loop.md)
- NIO 개념
- Selector와 Channel
- Non-blocking I/O
- Select Loop 패턴
- NIO vs BIO 비교

### 11. [Reactor (Project Reactor)](docs/11-reactor.md)
- Reactive Programming 개념
- Mono와 Flux
- 주요 연산자 (map, flatMap, filter, zip 등)
- 에러 처리 및 재시도
- Scheduler와 Thread 관리
- Cold vs Hot Stream
- Backpressure
- 테스트 (StepVerifier)

### 12. [Spring Reactor](docs/12-spring-reactor.md)
- Spring WebFlux 통합
- Controller에서 Reactor 사용
- WebClient 비동기 HTTP 클라이언트
- R2DBC 리액티브 데이터베이스
- 실무 패턴 및 주의사항

### 13. [Kotlin Coroutines](docs/13-kotlin-coroutines.md)
- 코루틴 기본 개념
- Suspend Function
- Coroutine Builders (launch, async, runBlocking)
- Dispatcher와 Thread 관리
- Structured Concurrency
- Flow (Reactive Stream)
- StateFlow와 SharedFlow
- Channel
- Spring 통합
- 실무 패턴
