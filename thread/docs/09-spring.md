# Spring Boot와 Thread

## @Async - 비동기 메서드 실행

### 기본 설정
```kotlin
@Configuration
@EnableAsync
class AsyncConfig
```

### 사용 방법
```kotlin
@Service
class EmailService(
    private val emailClient: EmailClient
) {
    @Async
    fun sendEmail(to: String, subject: String) {
        emailClient.send(to, subject)
    }

    @Async
    fun sendEmailWithResult(to: String): CompletableFuture<String> {
        val result = emailClient.send(to)
        return CompletableFuture.completedFuture(result)
    }
}
```

### 호출 방법
```kotlin
@RestController
class UserController(
    private val emailService: EmailService,
    private val userRepository: UserRepository
) {
    @PostMapping("/register")
    fun register(@RequestBody user: User): ResponseEntity<String> {
        userRepository.save(user)
        emailService.sendEmail(user.email, "Welcome")
        return ResponseEntity.ok("Registered")
    }

    @PostMapping("/register-with-result")
    fun registerWithResult(@RequestBody user: User): ResponseEntity<String> {
        userRepository.save(user)
      
        val future = emailService.sendEmailWithResult(user.email)
        future.thenAccept { result ->
            log.info("Email sent: {}", result)
        }

        return ResponseEntity.ok("Registered")
    }
}
```

### 주의사항

#### 1. Self-invocation 문제
```kotlin
@Service
class UserService {
    @Async
    fun sendEmail() { }

    fun registerUser() {
        // 같은 클래스 내 호출 - @Async 동작 안 함
        this.sendEmail() // 동기 실행됨
    }
}
```

**해결 방법:**
```kotlin
@Service
class UserService(
    @Lazy private val self: UserService // Self-injection
) {
    fun registerUser() {
        self.sendEmail() // 프록시를 통해 호출 - @Async 동작
    }
}

// 또는 별도 서비스로 분리 (권장)
@Service
class EmailService {
    @Async
    fun sendEmail() { }
}
```

#### 2. 반환 타입 제한
```kotlin
@Async
fun method() { }

@Async
fun methodWithFuture(): Future<String> { }

@Async
fun methodWithCompletable(): CompletableFuture<String> { }

// 허용 안 됨 - 일반 객체 반환은 무시됨
@Async
fun method(): String {
    return "result" // 호출자는 null을 받음
}
```

#### 3. Exception 처리
```kotlin
@Configuration
@EnableAsync
class AsyncConfig : AsyncConfigurer {
    override fun getAsyncUncaughtExceptionHandler(): AsyncUncaughtExceptionHandler {
        return AsyncUncaughtExceptionHandler { ex, method, params ->
            log.error(
                "Async method {} threw exception: {}",
                method.name, ex.message, ex
            )
        }
    }
}
```

## TaskExecutor 설정

### 기본 Executor 커스터마이징
```kotlin
@Configuration
@EnableAsync
class AsyncConfig : AsyncConfigurer {
    override fun getAsyncExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 5              // 기본 Thread 수
        executor.maxPoolSize = 10              // 최대 Thread 수
        executor.queueCapacity = 100           // Queue 크기
        executor.setThreadNamePrefix("async-") // Thread 이름
        executor.setKeepAliveSeconds(60)       // 유휴 Thread 유지 시간
        executor.setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
        executor.setWaitForTasksToCompleteOnShutdown(true) // Graceful shutdown
        executor.setAwaitTerminationSeconds(60)
        executor.initialize()
        return executor
    }
}
```

### 여러 Executor 사용
```kotlin
@Configuration
@EnableAsync
class AsyncConfig {
    @Bean(name = ["emailExecutor"])
    fun emailExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 3
        executor.maxPoolSize = 5
        executor.queueCapacity = 50
        executor.setThreadNamePrefix("email-")
        executor.initialize()
        return executor
    }

    @Bean(name = ["notificationExecutor"])
    fun notificationExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 10
        executor.maxPoolSize = 20
        executor.queueCapacity = 200
        executor.setThreadNamePrefix("notification-")
        executor.initialize()
        return executor
    }
}
```

**사용:**
```kotlin
@Service
class MessageService {
    @Async("emailExecutor")
    fun sendEmail(to: String) {
    }

    @Async("notificationExecutor")
    fun sendNotification(userId: String) {
    }
}
```

## @Scheduled - 스케줄링

### 기본 설정
```kotlin
@Configuration
@EnableScheduling
class SchedulingConfig
```

### 고정 주기 실행
```kotlin
@Component
class ScheduledTasks {
    // 고정 간격 (이전 실행 완료 후 5초 대기)
    @Scheduled(fixedDelay = 5000)
    fun taskWithFixedDelay() {
        log.info("Fixed delay task")
    }

    // 고정 비율 (5초마다 실행, 이전 실행 완료 무관)
    @Scheduled(fixedRate = 5000)
    fun taskWithFixedRate() {
        log.info("Fixed rate task")
    }

    // 초기 지연 후 고정 간격
    @Scheduled(initialDelay = 10000, fixedDelay = 5000)
    fun taskWithInitialDelay() {
        log.info("Task with initial delay")
    }
}
```

### Cron 표현식

**형식:** `초 분 시 일 월 요일`

| 필드               | 허용값                      | 특수문자            |
|------------------|--------------------------|-----------------|
| 초 (Seconds)      | 0-59                     | `, - * /`       |
| 분 (Minutes)      | 0-59                     | `, - * /`       |
| 시 (Hours)        | 0-23                     | `, - * /`       |
| 일 (Day of month) | 1-31                     | `, - * ? / L W` |
| 월 (Month)        | 1-12 또는 JAN-DEC          | `, - * /`       |
| 요일 (Day of week) | 0-7 또는 SUN-SAT (0,7=일요일) | `, - * ? / L #` |

**특수문자:**
- `*` : 모든 값 (매 시간, 매일 등)
- `?` : 특정 값 없음 (일/요일 중 하나만 지정 시 사용)
- `-` : 범위 (예: `10-12` = 10시, 11시, 12시)
- `,` : 여러 값 (예: `MON,WED,FRI` = 월, 수, 금)
- `/` : 증가값 (예: `0/15` = 0, 15, 30, 45)
- `L` : 마지막 (월의 마지막 날, 주의 마지막 요일)
- `W` : 가장 가까운 평일
- `#` : N번째 요일 (예: `2#3` = 3번째 화요일)

```kotlin
@Component
class ScheduledTasks {
    // 매시 정각
    @Scheduled(cron = "0 0 * * * ?")
    fun hourlyTask() {
        log.info("Hourly task")
    }

    // 평일 오전 9~18시, 30분마다
    @Scheduled(cron = "0 0/30 9-18 ? * MON-FRI")
    fun businessHoursTask() {
      log.info("Business hours task")
    }
}
```

### TaskScheduler 커스터마이징
```kotlin
@Configuration
@EnableScheduling
class SchedulingConfig : SchedulingConfigurer {
    override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
        val scheduler = ThreadPoolTaskScheduler()
        scheduler.poolSize = 10
        scheduler.setThreadNamePrefix("scheduled-")
        scheduler.setAwaitTerminationSeconds(60)
        scheduler.setWaitForTasksToCompleteOnShutdown(true)
        scheduler.initialize()
        taskRegistrar.setTaskScheduler(scheduler)
    }
}
```

### 동적 스케줄링
```kotlin
@Component
class DynamicScheduler(
    private val taskScheduler: TaskScheduler
) {
    // Cron 표현식으로 동적 스케줄 등록
    fun scheduleTask(task: Runnable, cronExpression: String) {
        val trigger = CronTrigger(cronExpression)
        taskScheduler.schedule(task, trigger)
    }

    // 지연 시간 후 1회 실행
    fun scheduleTaskWithDelay(task: Runnable, delay: Long) {
        taskScheduler.schedule(task, Instant.now().plusMillis(delay))
    }
}
```

## Virtual Thread 통합 (Spring Boot 3.2+)

### 기본 활성화
```properties
# application.properties
spring.threads.virtual.enabled=true
```

### 효과
- Tomcat/Jetty 요청 처리 Thread → Virtual Thread
- 수천 개 동시 요청 처리 가능

### @Async와 함께 사용
```kotlin
@Configuration
@EnableAsync
class AsyncConfig : AsyncConfigurer {
    override fun getAsyncExecutor(): Executor {
        // Virtual Thread Executor
        return Executors.newVirtualThreadPerTaskExecutor()
    }
}
```

### 조건부 설정
```kotlin
@Configuration
@EnableAsync
class AsyncConfig {
    @Bean
    @ConditionalOnThreading(Threading.PLATFORM)
    fun platformThreadExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 10
        executor.maxPoolSize = 20
        executor.initialize()
        return executor
    }

    @Bean
    @ConditionalOnThreading(Threading.VIRTUAL)
    fun virtualThreadExecutor(): Executor {
        return Executors.newVirtualThreadPerTaskExecutor()
    }
}
```

### Virtual Thread 사용 시 주의사항
```kotlin
// 안티패턴 - synchronized 블록에서 블로킹
@Async
@Synchronized // Pinning 발생!
fun processOrder(order: Order) {
    // I/O 작업
    orderClient.submit(order)
}

// 권장 - ReentrantLock 사용
class OrderService {
    private val lock = ReentrantLock()

    @Async
    fun processOrder(order: Order) {
        lock.lock()
        try {
            orderClient.submit(order)
        } finally {
            lock.unlock()
        }
    }
}
```

## 사용 예

### 1. Fire-and-Forget (알림 발송)
```kotlin
@Service
class NotificationService(
    private val notificationClient: NotificationClient
) {
    @Async
    fun sendNotification(user: User, message: String) {
        try {
            notificationClient.send(user.id, message)
        } catch (e: Exception) {
            log.error("Failed to send notification", e)
        }
    }
}
```

### 2. 병렬 처리 후 결과 수집
```kotlin
@Service
class ReportService(
    private val reportGenerator: ReportGenerator
) {
    @Async
    fun generateUserReport(userId: Long): CompletableFuture<UserReport> {
        val report = reportGenerator.generate(userId)
        return CompletableFuture.completedFuture(report)
    }

    fun generateReports(userIds: List<Long>): List<UserReport> {
        val futures = userIds.map { generateUserReport(it) }
        return futures.map { it.join() }
    }
}
```

### 3. Timeout 적용
```kotlin
@Service
class ExternalApiService(
    private val apiClient: ApiClient
) {
    @Async
    fun callApi(endpoint: String): CompletableFuture<String> {
        val result = apiClient.get(endpoint)
        return CompletableFuture.completedFuture(result)
    }

    fun callApiWithTimeout(endpoint: String): String {
        val future = callApi(endpoint)

        return try {
            future.get(3, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            future.cancel(true)
            throw RuntimeException("API timeout", e)
        } catch (e: Exception) {
            throw RuntimeException("API error", e)
        }
    }
}
```

### 4. Bulk 작업 분산 처리
```kotlin
@Service
class DataProcessingService {
    @Async
    fun processBatch(batch: List<Data>): CompletableFuture<Void> {
        batch.forEach { processData(it) }
        return CompletableFuture.completedFuture(null)
    }

    fun processAllData(dataList: List<Data>) {
        val batchSize = 100
        val futures = mutableListOf<CompletableFuture<Void>>()

        for (i in dataList.indices step batchSize) {
            val batch = dataList.subList(
                i, minOf(i + batchSize, dataList.size)
            )
            futures.add(processBatch(batch))
        }

        CompletableFuture.allOf(*futures.toTypedArray()).join()
    }

    private fun processData(data: Data) {
        // 데이터 처리 로직
    }
}
```

## 모니터링

### Thread Pool 메트릭
```kotlin
@Component
class ExecutorMonitoring(
    private val meterRegistry: MeterRegistry
) {
    @Bean
    fun monitoredExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 10
        executor.maxPoolSize = 20
        executor.queueCapacity = 100
        executor.setThreadNamePrefix("monitored-")
        executor.initialize()

        // Micrometer로 메트릭 수집
        ExecutorServiceMetrics(
            executor.threadPoolExecutor,
            "async_executor",
            emptyList()
        ).bindTo(meterRegistry)

        return executor
    }
}
```

### Actuator Endpoints
```properties
management.endpoints.web.exposure.include=metrics,scheduledtasks
```

**확인:**
- `/actuator/metrics/executor.active` - 활성 Thread 수
- `/actuator/metrics/executor.queued` - 대기 중인 작업 수
- `/actuator/scheduledtasks` - 스케줄된 작업 목록
