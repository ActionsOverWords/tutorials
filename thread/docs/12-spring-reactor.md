# Spring WebFlux (Reactive Web Framework)

## 동기/비동기, 블록킹/논블록킹

### 동기(Synchronous) vs 비동기(Asynchronous)

**제어권과 결과 처리 시점의 관점**

#### 동기 (Synchronous)
- 작업을 **순차적으로 실행**
- 호출한 함수가 **완료될 때까지 대기**
- 결과를 **즉시 반환**받음

```kotlin
fun synchronousCall() {
  val result1 = apiCall1() // 완료될 때까지 대기
  val result2 = apiCall2() // result1 완료 후 실행
  val result3 = apiCall3() // result2 완료 후 실행
  return combine(result1, result2, result3)
}
// 총 시간 = apiCall1 + apiCall2 + apiCall3
```

#### 비동기 (Asynchronous)
- 작업을 **동시에 시작**
- 호출 후 **즉시 다음 작업 진행**
- 결과는 **나중에 콜백/Promise로 처리**

```kotlin
fun asynchronousCall() {
  val future1 = apiCall1Async() // 즉시 반환
  val future2 = apiCall2Async() // 즉시 반환
  val future3 = apiCall3Async() // 즉시 반환

  // 모두 완료될 때까지 대기
  return combine(future1.get(), future2.get(), future3.get())
}
// 총 시간 = max(apiCall1, apiCall2, apiCall3)
```

### 블록킹(Blocking) vs 논블록킹(Non-blocking)

**Thread 점유 관점**

#### 블록킹 (Blocking)
- 작업이 완료될 때까지 **Thread가 대기**
- Thread가 **다른 일을 할 수 없음**
- 리소스 낭비 발생

```kotlin
fun blockingRead() {
  val data = socket.read() // Thread가 여기서 멈춤 (CPU 유휴)
  process(data)
}
```

#### 논블록킹 (Non-blocking)
- 작업 완료를 **기다리지 않고 즉시 반환**
- Thread가 **다른 작업을 계속 수행**
- 리소스 효율적 사용

```kotlin
fun nonBlockingRead() {
  socket.readAsync { data -> // 즉시 반환, Thread는 계속 실행
    process(data) // 완료되면 콜백 실행
  }
  // Thread는 다른 작업 가능
}
```

### 4가지 조합

| 구분             | Thread 상태     | 제어권             | 예시                |
|----------------|---------------|-----------------|-------------------|
| **동기 + 블록킹**   | 대기 (Blocking) | 순차 실행           | JDBC 쿼리, File I/O |
| **동기 + 논블록킹**  | 작업 중          | 순차 실행하되 Polling | NIO (select loop) |
| **비동기 + 블록킹**  | 대기 (드물게 사용)   | 병렬 실행           | Future.get()      |
| **비동기 + 논블록킹** | 작업 중          | 병렬 실행           | Reactor, Node.js  |

### 1. 동기 + 블록킹 (Spring MVC 방식)

```kotlin
fun syncBlocking() {
  // Thread가 여기서 멈춤
  val user = userRepository.findById(1L) // 200ms 대기
  val orders = orderRepository.findByUserId(user.id) // 300ms 대기
  return UserProfile(user, orders)
}
// 총 시간: 500ms, Thread는 500ms 동안 블로킹
```

### 2. 비동기 + 논블록킹 (Spring WebFlux 방식)

```kotlin
fun asyncNonBlocking() {
  return userRepository.findById(1L) // 즉시 반환 (Mono)
    .flatMap { user ->
      orderRepository.findByUserId(user.id) // 즉시 반환 (Flux)
        .collectList()
        .map { orders -> UserProfile(user, orders) }
    }
}
// Thread는 블로킹되지 않고 계속 다른 작업 수행
```

## Spring WebFlux 개요

### 의존성
```kotlin
implementation("org.springframework.boot:spring-boot-starter-webflux")
```

### Spring MVC vs Spring WebFlux

| 구분        | Spring MVC         | Spring WebFlux   |
|-----------|--------------------|------------------|
| 프로그래밍 모델  | Servlet API        | Reactive Streams |
| Thread 모델 | Thread per Request | Event Loop       |
| 동시성       | Thread Pool 크기에 의존 | CPU 코어 수만큼       |
| 사용 난이도    | 쉬움                 | 어려움 (학습 곡선)      |
| 성능        | I/O-bound에서 불리     | I/O-bound에서 유리   |

### 언제 Spring WebFlux를 사용하는가?
- I/O-bound 작업이 많은 경우 (API 호출, DB 쿼리)
- 높은 동시성이 필요한 경우
- 스트리밍 데이터 처리가 필요한 경우
- Backpressure가 필요한 경우

## Reactive Controller

### 기본 Controller
```kotlin
@RestController
@RequestMapping("/api/users")
class UserController(
  private val userRepository: UserRepository
) {

  @GetMapping("/{id}")
  fun getUser(@PathVariable id: Long): Mono<User> =
    userRepository.findById(id)

  @GetMapping
  fun getAllUsers(): Flux<User> =
    userRepository.findAll()

  @PostMapping
  fun createUser(@RequestBody user: User): Mono<User> =
    userRepository.save(user)

  @DeleteMapping("/{id}")
  fun deleteUser(@PathVariable id: Long): Mono<Void> =
    userRepository.deleteById(id)
}
```

### ResponseEntity 사용
```kotlin
@RestController
class UserController {

  @GetMapping("/users/{id}")
  fun getUser(@PathVariable id: Long): Mono<ResponseEntity<User>> =
    userRepository.findById(id)
      .map { user -> ResponseEntity.ok(user) }
      .defaultIfEmpty(ResponseEntity.notFound().build())

  @PostMapping("/users")
  fun createUser(@RequestBody user: User): Mono<ResponseEntity<User>> =
    userRepository.save(user)
      .map { saved ->
        ResponseEntity
          .created(URI.create("/users/${saved.id}"))
          .body(saved)
      }
}
```

## Reactive Repository

### ReactiveCrudRepository
```kotlin
interface UserRepository : ReactiveCrudRepository<User, Long> {

  fun findByName(name: String): Flux<User>

  fun findByEmail(email: String): Mono<User>

  @Query("SELECT * FROM users WHERE age > :age")
  fun findByAgeGreaterThan(age: Int): Flux<User>
}
```

### R2DBC 설정
```kotlin
// build.gradle.kts
implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
implementation("io.r2dbc:r2dbc-postgresql")

// application.yml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/mydb
    username: user
    password: password
```

## WebClient - Reactive HTTP Client

### 기본 설정
```kotlin
@Configuration
class WebClientConfig {

  @Bean
  fun webClient(): WebClient =
    WebClient.builder()
      .baseUrl("https://api.example.com")
      .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .build()
}
```

### Service에서 사용
```kotlin
@Service
class ExternalApiService(private val webClient: WebClient) {

  fun getUser(id: Long): Mono<User> =
    webClient.get()
      .uri("/users/{id}", id)
      .retrieve()
      .bodyToMono(User::class.java)

  fun getAllUsers(): Flux<User> =
    webClient.get()
      .uri("/users")
      .retrieve()
      .bodyToFlux(User::class.java)

  fun createUser(user: User): Mono<User> =
    webClient.post()
      .uri("/users")
      .bodyValue(user)
      .retrieve()
      .bodyToMono(User::class.java)
}
```

### 에러 처리
```kotlin
fun getUser(id: Long): Mono<User> =
  webClient.get()
    .uri("/users/{id}", id)
    .retrieve()
    .onStatus(HttpStatusCode::is4xxClientError) { response ->
      Mono.error(ClientException("Client error: ${response.statusCode()}"))
    }
    .onStatus(HttpStatusCode::is5xxServerError) { response ->
      Mono.error(ServerException("Server error: ${response.statusCode()}"))
    }
    .bodyToMono(User::class.java)
```

### 타임아웃 설정
```kotlin
@Bean
fun webClient(): WebClient {
  val httpClient = HttpClient.create()
    .responseTimeout(Duration.ofSeconds(3))

  return WebClient.builder()
    .clientConnector(ReactorClientHttpConnector(httpClient))
    .build()
}
```

## Error 처리

### Controller에서 에러 처리
```kotlin
@RestController
class UserController {

  @GetMapping("/users/{id}")
  fun getUser(@PathVariable id: Long): Mono<User> =
    userRepository.findById(id)
      .switchIfEmpty(Mono.error(UserNotFoundException(id)))
      .onErrorMap(DatabaseException::class.java) { ex ->
        ServiceException("Database error", ex)
      }
}
```

### Global Exception Handler
```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {

  @ExceptionHandler(UserNotFoundException::class)
  fun handle(ex: UserNotFoundException): Mono<ResponseEntity<ErrorResponse>> =
    Mono.just(
      ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse("User not found", ex.message ?: "Unknown error"))
    )

  @ExceptionHandler(ServiceException::class)
  fun handle(ex: ServiceException): Mono<ResponseEntity<ErrorResponse>> =
    Mono.just(
      ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorResponse("Service error", ex.message ?: "Unknown error"))
    )
}

data class ErrorResponse(
  val error: String,
  val message: String,
  val timestamp: Instant = Instant.now()
)
```

## 실무 패턴

### 1. 병렬 API 호출
```kotlin
@Service
class UserProfileService(
  private val userService: UserService,
  private val orderService: OrderService,
  private val paymentService: PaymentService
) {

  fun getUserProfile(userId: Long): Mono<UserProfile> =
    Mono.zip(
      userService.getUser(userId),
      orderService.getOrders(userId),
      paymentService.getPayments(userId)
    ).map { tuple ->
      UserProfile(
        user = tuple.t1,
        orders = tuple.t2,
        payments = tuple.t3
      )
    }
}
```

### 2. Pagination
```kotlin
@RestController
class UserController(private val userRepository: UserRepository) {

  @GetMapping("/users")
  fun getUsers(
    @RequestParam(defaultValue = "0") page: Int,
    @RequestParam(defaultValue = "20") size: Int
  ): Mono<Page<User>> =
    PageRequest.of(page, size).let { pageable ->
      userRepository.findAllBy(pageable)
        .collectList()
        .zipWith(userRepository.count())
        .map { tuple ->
          PageImpl(
            tuple.t1,
            pageable,
            tuple.t2
          )
        }
    }
}
```

### 3. Stream Processing
```kotlin
@Service
class EventProcessingService {

  fun processEvents(): Flux<ProcessedEvent> =
    eventRepository.streamAll()
      .buffer(100) // 100개씩 묶어서 처리
      .flatMap { events ->
        // 병렬 처리
        Flux.fromIterable(events)
          .parallel()
          .runOn(Schedulers.parallel())
          .map(this::processEvent)
          .sequential()
      }
      .onErrorContinue { error, event ->
        log.error("Failed to process event: {}", event, error)
      }

  private fun processEvent(event: Event): ProcessedEvent {
    // 이벤트 처리 로직
    return ProcessedEvent(event.id, event.data)
  }
}
```

### 4. Server-Sent Events (SSE)
```kotlin
@RestController
class EventController {

  @GetMapping(value = ["/events"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
  fun streamEvents(): Flux<ServerSentEvent<String>> =
    Flux.interval(Duration.ofSeconds(1))
      .map { count ->
        ServerSentEvent.builder<String>()
          .id(count.toString())
          .event("message")
          .data("Event $count")
          .build()
      }
}
```

### 5. Caching
```kotlin
@Service
class UserService(private val userRepository: UserRepository) {

  // Hot stream으로 캐싱
  private val cachedUsers: Flux<User> = userRepository.findAll()
    .cache(Duration.ofMinutes(5)) // 5분 캐싱

  fun getAllUsers(): Flux<User> = cachedUsers
}
```

## Security with WebFlux

### 의존성
```kotlin
implementation("org.springframework.boot:spring-boot-starter-security")
```

### Security Configuration
```kotlin
@Configuration
@EnableWebFluxSecurity
class SecurityConfig {

  @Bean
  fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
    http
      .authorizeExchange { exchanges ->
        exchanges
          .pathMatchers("/api/public/**").permitAll()
          .pathMatchers("/api/admin/**").hasRole("ADMIN")
          .anyExchange().authenticated()
      }
      .httpBasic(Customizer.withDefaults())
      .csrf { it.disable() }
      .build()
}
```

### ReactiveSecurityContextHolder 사용
```kotlin
@RestController
class UserController {

  @GetMapping("/api/users/me")
  fun getCurrentUser(): Mono<User> =
    ReactiveSecurityContextHolder.getContext()
      .map(SecurityContext::getAuthentication)
      .flatMap { auth ->
        val username = auth.name
        userService.findByUsername(username)
      }
}
```

## Testing

### WebTestClient
```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerTest {

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Test
  fun `should get user by id`() {
    webTestClient.get()
      .uri("/api/users/1")
      .exchange()
      .expectStatus().isOk
      .expectBody<User>()
      .consumeWith { response ->
        assertThat(response.responseBody?.id).isEqualTo(1L)
      }
  }

  @Test
  fun `should create user`() {
    val newUser = User(name = "John", email = "john@example.com")

    webTestClient.post()
      .uri("/api/users")
      .bodyValue(newUser)
      .exchange()
      .expectStatus().isCreated
      .expectBody<User>()
      .consumeWith { response ->
        assertThat(response.responseBody?.name).isEqualTo("John")
      }
  }
}
```

### Repository Test
```kotlin
@DataR2dbcTest
class UserRepositoryTest {

  @Autowired
  lateinit var userRepository: UserRepository

  @Test
  fun `should find user by email`() {
    val user = User(name = "John", email = "john@example.com")

    StepVerifier.create(
      userRepository.save(user)
        .then(userRepository.findByEmail("john@example.com"))
    )
      .assertNext { found ->
        assertThat(found.name).isEqualTo("John")
      }
      .verifyComplete()
  }
}
```

## 성능 비교

### Blocking (Spring MVC)
```
Thread 수 = 동시 요청 수
100개 요청 = 100개 Thread 필요
Thread Pool 고갈 가능
```

### Reactive (Spring WebFlux)
```
Thread 수 = CPU 코어 수
100개 요청 = 8개 Thread로 처리 가능 (8코어 기준)
높은 동시성 처리 가능
```

### 성능 특성

| 작업 유형     | Blocking (MVC)    | Reactive (WebFlux) | 권장       |
|-----------|-------------------|--------------------|----------|
| I/O-bound | Thread 수 = 요청 수   | Thread 수 = CPU 코어  | Reactive |
| CPU-bound | Thread 수 = CPU 코어 | 동일                 | Blocking |
| 간단한 CRUD  | 단순, 이해 쉬움         | 복잡, 학습 곡선          | Blocking |

**주의:**
- WebFlux가 항상 빠른 것은 아님
- I/O-bound + 높은 동시성에서만 유리
- CPU-bound는 전통적 방식이 더 나을 수 있음

## 주의사항

### 1. Blocking 코드 사용 금지
```kotlin
// 안티패턴
@GetMapping("/users/{id}")
fun getUser(@PathVariable id: Long): Mono<User> =
  Mono.fromCallable {
    Thread.sleep(1000) // 블로킹!
    userRepository.findById(id) // JDBC는 블로킹!
  }

// 올바른 방법
@GetMapping("/users/{id}")
fun getUser(@PathVariable id: Long): Mono<User> =
  reactiveUserRepository.findById(id) // R2DBC 사용
```

### 2. Reactive Stack 전체 사용
- **Database**: R2DBC (JDBC는 블로킹)
- **HTTP Client**: WebClient (RestTemplate는 블로킹)
- **Redis**: ReactiveRedisTemplate

### Reactive Stack

| 계층          | Blocking        | Reactive                 |
|-------------|-----------------|--------------------------|
| Web         | Spring MVC      | Spring WebFlux           |
| Database    | JDBC            | R2DBC                    |
| HTTP Client | RestTemplate    | WebClient                |
| Redis       | RedisTemplate   | ReactiveRedisTemplate    |
| Security    | Spring Security | Spring Security Reactive |
