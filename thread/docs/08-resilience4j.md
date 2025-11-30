# Resilience4j

## 개요

### 정의
Fault-tolerant 마이크로서비스를 위한 경량 내결함성 라이브러리

### 5가지 핵심 패턴
1. **Circuit Breaker**: 장애 전파 방지
2. **Rate Limiter**: 요청 속도 제한
3. **Bulkhead**: 리소스 격리
4. **Retry**: 재시도
5. **TimeLimiter**: 실행 시간 제한

## Circuit Breaker

### 개념
반복적인 실패 시 요청을 즉시 차단하여 장애 전파 방지 (전기 회로 차단기와 유사)

### 3가지 상태
```
CLOSED (정상) → OPEN (차단) → HALF_OPEN (시험) → CLOSED
```

1. **CLOSED**: 정상 동작, 요청 통과
2. **OPEN**: 실패율 임계값 초과 시 전환, 모든 요청 즉시 실패 (CallNotPermittedException)
3. **HALF_OPEN**: 일정 시간 후 전환, 제한된 요청으로 서비스 회복 테스트

### 핵심 설정

#### Sliding Window 타입
- **COUNT_BASED** (기본): 최근 N개 호출 기준
- **TIME_BASED**: 최근 N초 동안의 호출 기준

```kotlin
val config = CircuitBreakerConfig.custom()
    // Sliding Window 설정
    .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
    .slidingWindowSize(10)                          // 최근 10개 호출 기준
    .minimumNumberOfCalls(5)                        // 최소 5개 호출 후 실패율 계산 (중요!)

    // 실패 임계값
    .failureRateThreshold(50.0f)                    // 실패율 50% 이상 시 OPEN
    .slowCallRateThreshold(50.0f)                   // 느린 호출 비율 50% 이상 시 OPEN
    .slowCallDurationThreshold(Duration.ofSeconds(2)) // 2초 이상을 느린 호출로 간주

    // 상태 전환 설정
    .waitDurationInOpenState(Duration.ofSeconds(10)) // OPEN 상태 10초 유지
    .automaticTransitionFromOpenToHalfOpenEnabled(false) // OPEN→HALF_OPEN 자동 전환 (기본: false)
    .permittedNumberOfCallsInHalfOpenState(3)        // HALF_OPEN 시 3개 요청 테스트

    // 예외 처리
    .recordExceptions(IOException::class.java)       // 기록할 예외
    .ignoreExceptions(BusinessException::class.java) // 무시할 예외
    .build()

val circuitBreaker = CircuitBreaker.of("api", config)
```

### 사용 방법

```kotlin
// 1. Functional API
val decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker) {
    apiService.call()
}

val result = runCatching { decoratedSupplier.get() }
    .recover { "Fallback value" }
    .getOrThrow()

// 2. Spring Boot + AOP
@CircuitBreaker(name = "api", fallbackMethod = "fallback")
fun callApi(): String {
    return restTemplate.getForObject(url, String::class.java)!!
}

fun fallback(e: Exception): String {
    return "Circuit breaker fallback"
}
```

### automaticTransitionFromOpenToHalfOpenEnabled 옵션

OPEN → HALF_OPEN 전환 방식 제어:

```kotlin
// false (기본값): 호출이 있어야 HALF_OPEN으로 전환
.automaticTransitionFromOpenToHalfOpenEnabled(false)

// true: waitDurationInOpenState 후 자동 전환
.automaticTransitionFromOpenToHalfOpenEnabled(true)
```

| 설정 | 동작 | 장점 | 단점 |
|-----|------|------|-----|
| false (기본) | OPEN 상태에서 요청이 와야 HALF_OPEN 전환 | 불필요한 전환 방지 | 트래픽 없으면 계속 OPEN |
| true | 시간 경과 시 자동 전환 | 서비스 회복 시 빠른 복구 | HALF_OPEN 시험 요청이 불필요할 수 있음 |

**권장 사용 시점**:
- **true**: 배치 작업, 스케줄러 등 정기적 호출이 없는 경우
- **false**: API 서버 등 지속적 트래픽이 있는 경우

### 주의사항

1. **minimumNumberOfCalls 설정 필수**: 초기 몇 번의 실패로 즉시 OPEN되는 것을 방지
2. **Slow call과 Failed call 구분**: 둘 다 Circuit Breaker를 OPEN시킬 수 있음
3. **CallNotPermittedException 처리**: OPEN 상태에서 발생하는 예외에 대한 적절한 fallback 필요


## Rate Limiter

### 개념
정해진 시간 동안 허용된 요청 수를 제한하여 과부하 방지

### 동작 원리
AtomicRateLimiter는 시간을 cycle로 나누고, 각 cycle 시작 시 권한(permission)을 갱신:
```
Cycle 1 (0-1초): 10개 권한
Cycle 2 (1-2초): 10개 권한
...
```

### 핵심 설정

```kotlin
val config = RateLimiterConfig.custom()
    .limitForPeriod(10)                            // 주기당 10개 요청 허용
    .limitRefreshPeriod(Duration.ofSeconds(1))     // 1초마다 갱신
    .timeoutDuration(Duration.ofMillis(500))       // 획득 대기 최대 500ms
    .build()

val rateLimiter = RateLimiter.of("api", config)

// 런타임 설정 변경 (필요시)
rateLimiter.changeLimitForPeriod(20)                     // 주기당 허용 요청 수 변경
rateLimiter.changeTimeoutDuration(Duration.ofSeconds(1)) // 대기 시간 변경
```

### 런타임 설정 변경

Rate Limiter는 운영 중 설정 변경 가능:

```kotlin
// 트래픽 급증 시 제한 완화
if (isHighTrafficPeriod()) {
    rateLimiter.changeLimitForPeriod(100)  // 10 → 100으로 증가
}

// 정상 상태로 복귀
rateLimiter.changeLimitForPeriod(10)
```

**주의사항**:
- `changeLimitForPeriod`: 다음 cycle부터 적용 (현재 cycle은 변경 안됨)
- `changeTimeoutDuration`: 대기 중인 Thread에는 영향 없음

### 사용 방법

```kotlin
// 1. Functional API
val decoratedSupplier = RateLimiter.decorateSupplier(rateLimiter) {
    apiService.call()
}

val result = runCatching { decoratedSupplier.get() }
    .recover { "Rate limit exceeded" }
    .getOrThrow()

// 2. Spring Boot
@RateLimiter(name = "api", fallbackMethod = "rateLimitFallback")
fun callApi(): String {
    return restTemplate.getForObject(url, String::class.java)!!
}

fun rateLimitFallback(e: RequestNotPermitted): String {
    return "Too many requests"
}
```

### 주의사항

1. **분산 환경 한계**: 단일 인스턴스 기준으로만 제한 (전체 클러스터 제한 불가)
2. **timeoutDuration 설정**: 0으로 설정 시 즉시 실패, 높게 설정 시 대기 시간 증가

## Bulkhead

### 개념
리소스를 격리하여 하나의 실패가 전체 시스템에 영향을 주지 않도록 방지 (배의 격벽과 유사)

### 두 가지 방식

#### 1. Semaphore-based (권장)

**장점**:
- 오버헤드 낮음 (별도 Thread 생성 없음)
- Virtual Thread와 호환성 좋음 (Java 21+)

**단점**:
- Timeout 제어 불가 (TimeLimiter 별도 필요)

```kotlin
val config = BulkheadConfig.custom()
    .maxConcurrentCalls(5)                         // 동시 실행 최대 5개
    .maxWaitDuration(Duration.ofMillis(100))       // 대기 최대 100ms
    .build()

val bulkhead = Bulkhead.of("api", config)
```

#### 2. Thread Pool-based

**장점**:
- 완전한 격리 (별도 Thread Pool 사용)
- Queue를 통한 버퍼링

**단점**:
- Context switching 오버헤드
- 메모리 사용량 증가

```kotlin
val config = ThreadPoolBulkheadConfig.custom()
    .maxThreadPoolSize(5)                          // 최대 Thread Pool 크기
    .coreThreadPoolSize(3)                         // 기본 Thread Pool 크기
    .queueCapacity(10)                             // 대기 Queue 용량
    .keepAliveDuration(Duration.ofMillis(20))      // 유휴 Thread 대기 시간
    .build()

val bulkhead = ThreadPoolBulkhead.of("api", config)
```

### 선택 가이드

| 상황 | 권장 방식 |
|-----|----------|
| 비동기 처리 (Coroutine, CompletableFuture) | Semaphore |
| Virtual Thread 사용 | Semaphore |
| 완전한 Thread 격리 필요 | ThreadPool |
| Blocking I/O + Timeout 필요 | ThreadPool |

### 사용 방법

```kotlin
// Spring Boot
@Bulkhead(name = "api", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "bulkheadFallback")
fun callApi(): String {
    return restTemplate.getForObject(url, String::class.java)!!
}

fun bulkheadFallback(e: BulkheadFullException): String {
    return "Too many concurrent requests"
}
```

### 사용 예
**문제**: 결제 API가 느려져서 모든 Thread가 결제 대기 중 → 다른 API도 응답 불가

**해결**: Bulkhead 적용 → 결제 API는 최대 5개 Thread만 사용 → 나머지 Thread는 다른 API 처리 가능

## Retry

### 개념
일시적 장애에 대해 자동으로 재시도

### 핵심 설정

```kotlin
val config = RetryConfig.custom()
    .maxAttempts(3)                                // 최대 3번 시도
    .waitDuration(Duration.ofSeconds(1))           // 재시도 간격 1초
    .retryExceptions(IOException::class.java)      // IOException만 재시도
    .ignoreExceptions(BusinessException::class.java) // BusinessException은 재시도 안 함
    .retryOnResult { result ->                     // 결과값 기반 재시도 (선택)
        result == null || result.isEmpty()
    }
    .failAfterMaxAttempts(false)                   // 최대 재시도 후 예외 던질지 (기본: false)
    .build()

val retry = Retry.of("api", config)
```

### Exponential Backoff + Jitter

```kotlin
val config = RetryConfig.custom()
    .maxAttempts(5)
    .intervalFunction(
        IntervalFunction.ofExponentialRandomBackoff(
            initialIntervalMillis = 1000L,
            multiplier = 2.0,
            randomizationFactor = 0.5  // Jitter: ±50%
        )
    )
    .build()

// 실제 간격 (예시):
// 1차: 1000ms ± 500ms (500~1500ms)
// 2차: 2000ms ± 1000ms (1000~3000ms)
// 3차: 4000ms ± 2000ms (2000~6000ms)
```

**Jitter를 추가하는 이유**: Thundering herd 현상 방지 (동시에 많은 요청이 재시도되는 것을 분산)

### retryOnResult 활용

예외 없이 실패를 나타내는 결과값에 대해서도 재시도 가능:

```kotlin
data class ApiResponse(val status: String, val data: String?)

val config = RetryConfig.custom<ApiResponse>()
    .maxAttempts(3)
    .waitDuration(Duration.ofSeconds(1))
    .retryOnResult { response ->
        // status가 "pending" 이거나 data가 null이면 재시도
        response.status == "pending" || response.data == null
    }
    .build()

// 사용 예시: 폴링 패턴
val result = Retry.decorateSupplier(retry) {
    externalApi.getJobStatus(jobId)  // ApiResponse 반환
}.get()
```

**실무 예시**:
- **비동기 작업 폴링**: Job 상태가 "completed"가 될 때까지 재시도
- **Rate Limit 응답 처리**: HTTP 429 상태에 대한 재시도
- **Partial success**: 일부 데이터만 반환된 경우 재시도

### 사용 방법

```kotlin
// Spring Boot
@Retry(name = "api", fallbackMethod = "retryFallback")
fun callApi(): String {
    return restTemplate.getForObject(url, String::class.java)!!
}

fun retryFallback(e: Exception): String {
    return "All retries failed"
}
```

### 주의사항

1. **멱등성(Idempotency) 필수**: 재시도해도 안전한 작업만 적용
   - 조회 (GET)
   - 멱등한 저장 (PUT with unique key)
   - 결제, 주문 등 - `중복 실행 위험`

2. **무한 재시도 방지**: maxAttempts 반드시 설정

3. **총 대기 시간 고려**: 사용자 경험에 영향

### 실무 시나리오
- **네트워크 일시 장애**: 재시도로 성공 가능
- **DB Connection 풀 부족**: 잠시 후 커넥션 반환되면 성공
- **Rate Limit 초과**: Exponential backoff로 재시도 간격 늘림

## TimeLimiter

### 개념
실행 시간을 제한하여 무한 대기 방지

### 핵심 설정

```kotlin
val config = TimeLimiterConfig.custom()
    .timeoutDuration(Duration.ofSeconds(3))        // 3초 타임아웃
    .cancelRunningFuture(true)                     // Future 취소 시도
    .build()

val timeLimiter = TimeLimiter.of("api", config)
```

### 사용 방법

```kotlin
// 비동기 실행
val future = CompletableFuture.supplyAsync {
    apiService.call()
}

val result = timeLimiter.executeFutureSupplier {
    future
}
```

### 주의사항

1. **CompletableFuture 필요**: 동기 메서드는 직접 지원 안 함
2. **Thread 취소 한계**: `cancelRunningFuture=true`여도 실제 Thread는 계속 실행될 수 있음

### 실무 시나리오
- **느린 외부 API**: 일정 시간 후 타임아웃으로 처리
- **DB 쿼리 시간 제한**: 긴 쿼리 방지

## 패턴 조합

### 실행 순서 (Aspect Order)

```
Retry ( CircuitBreaker ( RateLimiter ( TimeLimiter ( Bulkhead ( Function ) ) ) ) )
```

### 순서의 이유

1. **Retry가 가장 바깥**: Circuit Breaker가 OPEN이면 재시도해도 즉시 실패 (불필요한 재시도 방지)
2. **Circuit Breaker가 Rate Limiter보다 바깥**: Rate Limit 소진을 막기 위해
3. **Bulkhead가 가장 안쪽**: 실제 리소스 격리

### 실행 흐름

1. **Retry**: 재시도 로직 시작
2. **Circuit Breaker**: OPEN 상태면 즉시 실패 → Retry가 다시 시도
3. **Rate Limiter**: 허용 한도 확인
4. **TimeLimiter**: 타임아웃 시작
5. **Bulkhead**: 동시 실행 수 확인
6. **실제 호출**: 외부 API 호출

### 최악의 대기 시간 계산

```
최악의 경우 = Retry 횟수 × TimeLimiter timeout
```

## Spring Boot 통합

### 의존성

```kotlin
dependencies {
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.1.0")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-actuator") // 모니터링
}
```

### 설정 파일

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        sliding-window-type: COUNT_BASED
        sliding-window-size: 10
        minimum-number-of-calls: 5
        failure-rate-threshold: 50
        slow-call-rate-threshold: 50
        slow-call-duration-threshold: 2s
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 3
    instances:
      payment-api:
        base-config: default
      order-api:
        base-config: default
        failure-rate-threshold: 30  # 오버라이드

  ratelimiter:
    configs:
      default:
        limit-for-period: 100
        limit-refresh-period: 1s
        timeout-duration: 500ms
    instances:
      payment-api:
        base-config: default
      order-api:
        limit-for-period: 200  # 오버라이드

  bulkhead:
    configs:
      default:
        max-concurrent-calls: 10
        max-wait-duration: 100ms
    instances:
      payment-api:
        max-concurrent-calls: 5  # 결제는 더 제한적으로

  retry:
    configs:
      default:
        max-attempts: 3
        wait-duration: 1s
        retry-exceptions:
          - java.io.IOException
          - org.springframework.web.client.ResourceAccessException
        ignore-exceptions:
          - com.example.BusinessException
    instances:
      payment-api:
        max-attempts: 2  # 결제는 재시도 최소화
        enable-exponential-backoff: true
        exponential-backoff-multiplier: 2
        enable-random-backoff: true  # Jitter

  timelimiter:
    configs:
      default:
        timeout-duration: 3s
        cancel-running-future: true
    instances:
      payment-api:
        timeout-duration: 5s
```

## 모니터링

### Actuator 설정

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,circuitbreakers,ratelimiters,bulkheads,retries
  endpoint:
    health:
      show-details: always
  health:
    circuitbreakers:
      enabled: true
    ratelimiters:
      enabled: true
```

### 주요 Metrics

```bash
# Circuit Breaker
/actuator/metrics/resilience4j.circuitbreaker.calls
/actuator/metrics/resilience4j.circuitbreaker.state

# Rate Limiter
/actuator/metrics/resilience4j.ratelimiter.available.permissions
/actuator/metrics/resilience4j.ratelimiter.waiting.threads

# Bulkhead
/actuator/metrics/resilience4j.bulkhead.available.concurrent.calls
/actuator/metrics/resilience4j.bulkhead.max.allowed.concurrent.calls

# Retry
/actuator/metrics/resilience4j.retry.calls
```

### 이벤트 리스닝 (실무 예시)

```kotlin
@Component
class Resilience4jEventListener(
    private val alertService: AlertService
) {

    @EventListener
    fun onCircuitBreakerStateTransition(event: CircuitBreakerOnStateTransitionEvent) {
        val fromState = event.stateTransition.fromState
        val toState = event.stateTransition.toState
        val circuitBreakerName = event.circuitBreakerName

        logger.warn {
            "Circuit Breaker '$circuitBreakerName' state: $fromState -> $toState"
        }

        // OPEN 상태로 전환 시 알림
        if (toState == CircuitBreaker.State.OPEN) {
            alertService.sendAlert(
                title = "Circuit Breaker OPEN",
                message = "Circuit Breaker '$circuitBreakerName' is now OPEN",
                severity = AlertSeverity.HIGH
            )
        }
    }

    @EventListener
    fun onCircuitBreakerError(event: CircuitBreakerOnErrorEvent) {
        val name = event.circuitBreakerName
        val exception = event.throwable

        logger.error(exception) {
            "Circuit Breaker '$name' recorded error: ${exception.message}"
        }
    }

    @EventListener
    fun onRateLimiterEvent(event: RateLimiterOnFailureEvent) {
        logger.warn {
            "Rate limit exceeded for '${event.rateLimiterName}'"
        }
    }

    @EventListener
    fun onBulkheadEvent(event: BulkheadOnCallRejectedEvent) {
        logger.warn {
            "Bulkhead '${event.bulkheadName}' rejected call - too many concurrent requests"
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
```

## 주요 개념 정리

### Circuit Breaker 상태 전환

```
CLOSED (정상) → OPEN (차단) → HALF_OPEN (시험) → CLOSED
```

| 상태        | 동작           | 전환 조건                                   |
|-----------|--------------|-----------------------------------------|
| CLOSED    | 정상 동작, 요청 통과 | 실패율 or 느린 호출 비율 임계값 초과 시 OPEN으로         |
| OPEN      | 모든 요청 즉시 차단  | `waitDurationInOpenState` 후 HALF_OPEN으로 |
| HALF_OPEN | 제한된 시험 요청 허용 | 성공 시 CLOSED, 실패 시 OPEN으로                |

### 패턴별 목적과 사용 시점

| 패턴              | 목적        | 제한 대상          | 사용 시점                |
|-----------------|-----------|----------------|----------------------|
| Circuit Breaker | 장애 전파 방지  | 실패율            | 외부 서비스 장애 시 빠른 실패    |
| Rate Limiter    | 요청 속도 제한  | 시간당 요청 수       | API 호출 제한 준수, DoS 방지 |
| Bulkhead        | 리소스 격리    | 동시 실행 Thread 수 | 특정 서비스 장애가 전체에 영향 방지 |
| Retry           | 일시적 장애 대응 | 재시도 횟수         | 네트워크 일시 장애, DB 풀 부족  |
| TimeLimiter     | 실행 시간 제한  | Timeout        | 느린 응답 방지             |

### Rate Limiter vs Bulkhead

| 구분    | Rate Limiter                    | Bulkhead              |
|-------|---------------------------------|-----------------------|
| 제한 방식 | 시간당 요청 수                        | 동시 실행 Thread 수        |
| 목적    | 과부하 방지, API 제한 준수               | 리소스 격리, 장애 격리         |
| 예시    | 초당 100건만 허용                     | 동시 5개만 실행             |
| 효과    | DoS 공격 방지, 외부 API rate limit 준수 | 특정 서비스 느려져도 다른 서비스 정상 |

### Retry + Circuit Breaker 조합

**실행 순서:**
```
Retry ( Circuit Breaker ( API Call ) )
```

**동작 방식:**
1. 첫 번째 시도: Circuit Breaker 통과 → API 호출 → 실패
2. 두 번째 시도 (Retry): Circuit Breaker가 OPEN으로 전환됨 → CallNotPermittedException 즉시 발생
3. 세 번째 시도 (Retry): Circuit Breaker 여전히 OPEN → 즉시 실패

**주의사항:**
- Circuit Breaker OPEN 시 Retry해도 즉시 실패 (불필요한 재시도 방지)
- 최악의 지연 시간 = 재시도 횟수 × 타임아웃
- 사용자 경험을 고려하여 적절한 값 설정

**예시:**
- Retry 3회, Timeout 5초 → 최악 15초
- 권장: Retry 3회, Timeout 2초 → 최악 6초
