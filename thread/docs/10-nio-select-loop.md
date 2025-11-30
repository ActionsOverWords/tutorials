# NIO Select Loop (동기 + 논블록킹)

## 개요

### 정의
**NIO (New I/O)** 는 Java의 Non-blocking I/O API로, **Select Loop**는 하나의 Thread로 여러 I/O 채널을 모니터링하는 패턴

### 핵심 개념
- **동기 (Synchronous)**: 이벤트 루프가 순차적으로 실행
- **논블록킹 (Non-blocking)**: I/O 작업이 Thread를 블로킹하지 않음
- **Polling**: 주기적으로 준비된 이벤트 확인

## NIO 핵심 구성 요소

### 1. Channel
데이터 읽기/쓰기를 위한 양방향 통로

```kotlin
// ServerSocketChannel: 서버 소켓
val serverChannel = ServerSocketChannel.open()
serverChannel.configureBlocking(false) // 논블록킹 설정

// SocketChannel: 클라이언트 연결
val clientChannel = SocketChannel.open()
clientChannel.configureBlocking(false)
```

### 2. Buffer
데이터를 임시 저장하는 컨테이너

```kotlin
val buffer = ByteBuffer.allocate(1024)

// 쓰기 모드
buffer.put("Hello".toByteArray())

// 읽기 모드 전환
buffer.flip()

// 읽기
val data = ByteArray(buffer.remaining())
buffer.get(data)
```

### 3. Selector
여러 Channel을 하나의 Thread에서 모니터링

```kotlin
val selector = Selector.open()

// Channel을 Selector에 등록
channel.register(selector, SelectionKey.OP_READ)

// 관심 이벤트 종류
// - OP_ACCEPT: 새 연결 수락
// - OP_CONNECT: 연결 완료
// - OP_READ: 읽기 준비
// - OP_WRITE: 쓰기 준비
```

## Select Loop 동작 방식

### 기본 구조

```kotlin
fun selectLoop() {
  val selector = Selector.open()

  // 1. 서버 채널 생성 및 등록
  val serverChannel = ServerSocketChannel.open()
  serverChannel.configureBlocking(false)
  serverChannel.socket().bind(InetSocketAddress(8080))
  serverChannel.register(selector, SelectionKey.OP_ACCEPT)

  log.info("서버 시작: 8080")

  // 2. 이벤트 루프 (동기 + 논블록킹)
  while (true) {
    // 준비된 이벤트 대기 (동기: 블로킹)
    selector.select()

    // 준비된 채널 처리 (논블록킹)
    val keys = selector.selectedKeys().iterator()
    while (keys.hasNext()) {
      val key = keys.next()
      keys.remove()

      handleEvent(key)
    }
  }
}
```

### 이벤트 처리

```kotlin
fun handleEvent(key: SelectionKey) {
  when {
    key.isAcceptable -> handleAccept(key)
    key.isReadable -> handleRead(key)
    key.isWritable -> handleWrite(key)
  }
}

fun handleAccept(key: SelectionKey) {
  val serverChannel = key.channel() as ServerSocketChannel
  val clientChannel = serverChannel.accept() // 논블록킹: 즉시 반환

  if (clientChannel != null) {
    clientChannel.configureBlocking(false)
    clientChannel.register(key.selector(), SelectionKey.OP_READ)
    log.info("새 연결: ${clientChannel.remoteAddress}")
  }
}

fun handleRead(key: SelectionKey) {
  val channel = key.channel() as SocketChannel
  val buffer = ByteBuffer.allocate(256)

  val bytesRead = channel.read(buffer) // 논블록킹: 즉시 반환

  when {
    bytesRead == -1 -> {
      // 연결 종료
      channel.close()
      log.info("연결 종료")
    }
    bytesRead > 0 -> {
      // 데이터 수신
      buffer.flip()
      val data = String(buffer.array(), 0, bytesRead)
      log.info("수신: $data")

      // Echo 응답
      channel.write(ByteBuffer.wrap("Echo: $data".toByteArray()))
    }
  }
}
```

## 실행 흐름

```
┌────────────────────────────────────┐
│   Single Thread                    │
│   (하나의 Thread로 모든 연결 처리)      │
└────────────────────────────────────┘
              │
              ▼
      ┌───────────────┐
      │  select()     │ ◄── 동기: 준비된 이벤트 대기
      │  (블로킹)       │     (이벤트 없으면 여기서 대기)
      └───────────────┘
              │
              ▼
      ┌───────────────┐
      │ 이벤트 준비?     │
      └───────────────┘
         │         │
        YES       NO (계속 대기)
         │
         ▼
      ┌───────────────┐
      │ Channel 1     │ ◄── 논블록킹: 즉시 처리
      │ accept()      │     (블로킹하지 않음)
      └───────────────┘
         │
         ▼
      ┌───────────────┐
      │ Channel 2     │ ◄── 논블록킹: 즉시 처리
      │ read()        │     (데이터 없으면 0 반환)
      └───────────────┘
         │
         ▼
      ┌───────────────┐
      │ Channel 3     │ ◄── 논블록킹: 즉시 처리
      │ write()       │     (버퍼 가득 차면 일부만 쓰기)
      └───────────────┘
         │
         └─── (루프 반복)
```

## 왜 "동기 + 논블록킹"인가?

### 동기 (Synchronous)

**이벤트 루프가 순차적으로 실행**

```kotlin
while (true) {
  selector.select()           // 1. 이벤트 대기 (순차)
  processEvents()             // 2. 이벤트 처리 (순차)
  // 다음 루프로 (순차)
}
```

- `select()` 호출이 완료될 때까지 기다림
- 이벤트 처리가 순차적으로 진행
- 하나의 이벤트 루프에서 모든 작업 수행

### 논블록킹 (Non-blocking)

**I/O 작업이 Thread를 블로킹하지 않음**

```kotlin
// 블로킹 I/O (전통적 방식)
val data = socket.read() // 데이터 올 때까지 Thread 대기 X

// 논블록킹 I/O
val bytesRead = channel.read(buffer) // 즉시 반환
if (bytesRead == 0) {
  // 데이터 없음, Thread는 계속 다른 일 가능
}
```

- I/O 작업이 즉시 반환
- 데이터가 없어도 Thread가 블로킹되지 않음
- Thread는 다른 채널을 계속 모니터링 가능

## Polling 방식

### 개념
주기적으로 상태를 확인하는 방식

```kotlin
while (true) {
  // Polling: 준비된 이벤트 있는지 확인
  val readyChannels = selector.select()

  if (readyChannels == 0) {
    continue // 준비된 이벤트 없음
  }

  // 준비된 채널들 처리
  val keys = selector.selectedKeys()
  for (key in keys) {
    processEvent(key) // 논블록킹으로 즉시 처리
  }
}
```

### Polling 전략

#### 1. select() - 블로킹 Polling
```kotlin
selector.select() // 이벤트 올 때까지 블로킹
```

#### 2. select(timeout) - 타임아웃 Polling
```kotlin
selector.select(1000) // 최대 1초 대기
```

#### 3. selectNow() - 논블록킹 Polling
```kotlin
val ready = selector.selectNow() // 즉시 반환
if (ready == 0) {
  // 준비된 이벤트 없음
  doOtherWork()
}
```

## 전통적 방식과 비교

### 1. Blocking I/O (동기 + 블로킹)

```kotlin
// Thread-per-Connection 모델
fun blockingServer() {
  val serverSocket = ServerSocket(8080)

  while (true) {
    val socket = serverSocket.accept() // 블로킹

    // 각 연결마다 Thread 생성
    thread {
      val input = socket.getInputStream()
      val data = input.read() // 블로킹 (데이터 올 때까지 대기)
      process(data)
    }
  }
}

// 문제점:
// - 100개 연결 = 100개 Thread 필요
// - Thread 생성/관리 비용
// - Context Switching 오버헤드
```

### 2. NIO Select Loop (동기 + 논블록킹)

```kotlin
fun nioServer() {
  val selector = Selector.open()
  val serverChannel = ServerSocketChannel.open()
  serverChannel.configureBlocking(false)
  serverChannel.register(selector, SelectionKey.OP_ACCEPT)

  // 하나의 Thread로 모든 연결 처리
  while (true) {
    selector.select() // 동기: 이벤트 대기

    val keys = selector.selectedKeys()
    for (key in keys) {
      // 논블록킹: 즉시 처리
      when {
        key.isAcceptable -> {
          val client = serverChannel.accept() // 즉시 반환
          client.configureBlocking(false)
          client.register(selector, SelectionKey.OP_READ)
        }
        key.isReadable -> {
          val channel = key.channel() as SocketChannel
          val buffer = ByteBuffer.allocate(256)
          channel.read(buffer) // 즉시 반환
        }
      }
    }
  }
}

// 장점:
// - 100개 연결 = 1개 Thread로 처리
// - Thread 생성 비용 없음
// - Context Switching 최소화
```

### 비교표

| 구분                | Blocking I/O               | NIO Select Loop    |
|-------------------|----------------------------|--------------------|
| Thread 수          | 연결 수만큼                     | 1개 (또는 소수)         |
| Thread 상태         | 대기 중 (블로킹)                 | 작업 중 (논블로킹)        |
| 동시 연결             | 수백 개                       | 수천~수만 개            |
| 메모리 사용            | 높음 (Thread per Connection) | 낮음 (Single Thread) |
| Context Switching | 많음                         | 적음                 |
| 코드 복잡도            | 단순                         | 복잡 (이벤트 처리)        |
| 적합한 경우            | 연결 수 적음, 간단한 로직            | 높은 동시성, I/O-bound  |

## 실제 사용 사례

### 1. Netty (고성능 네트워크 프레임워크)

```kotlin
val bossGroup = NioEventLoopGroup(1)    // Accept용
val workerGroup = NioEventLoopGroup()   // I/O용

val bootstrap = ServerBootstrap()
  .group(bossGroup, workerGroup)
  .channel(NioServerSocketChannel::class.java) // NIO 사용
  .childHandler(object : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
      ch.pipeline().addLast(MyHandler())
    }
  })

// 내부적으로 Select Loop 사용
// - EventLoop가 Selector 보유
// - 각 EventLoop는 여러 Channel 처리
```

### 2. Node.js (이벤트 루프)

```javascript
// Node.js는 내부적으로 libuv 사용
// libuv는 OS별 최적화된 I/O 멀티플렉싱 사용
// - Linux: epoll (select의 개선 버전)
// - macOS: kqueue
// - Windows: IOCP

http.createServer((req, res) => {
  // 논블록킹으로 동작
  fs.readFile('file.txt', (err, data) => {
    res.end(data)
  })
})
```

### 3. Redis (단일 Thread 이벤트 루프)

```
Redis는 단일 Thread 이벤트 루프 사용
- Linux: epoll
- BSD: kqueue
- Fallback: select

하나의 Thread로 수만 개의 클라이언트 처리 가능
```

### 4. Vert.x (Reactive 툴킷)

```kotlin
val vertx = Vertx.vertx()

vertx.createHttpServer()
  .requestHandler { request ->
    // Event Loop Thread에서 실행
    request.response()
      .putHeader("content-type", "text/plain")
      .end("Hello from Vert.x!")
  }
  .listen(8080)

// 내부적으로 Netty 사용 (NIO)
```

## OS별 I/O 멀티플렉싱

### select vs epoll vs kqueue vs IOCP

| OS                   | 메커니즘   | 특징                                       |
|----------------------|--------|------------------------------------------|
| **Unix/Linux (구버전)** | select | FD_SETSIZE 제한 (보통 1024)<br>O(n) 성능       |
| **Linux (2.6+)**     | epoll  | FD 제한 없음<br>O(1) 성능<br>Edge-triggered 지원 |
| **BSD/macOS**        | kqueue | 효율적<br>다양한 이벤트 지원                        |
| **Windows**          | IOCP   | Completion 기반<br>가장 효율적                  |

### Java NIO의 내부 동작

```kotlin
// Java NIO는 OS에 맞게 자동 선택
val selector = Selector.open()

// Linux → EPollSelectorImpl
// macOS → KQueueSelectorImpl
// Windows → WindowsSelectorImpl
// Fallback → PollSelectorImpl (select 기반)
```

## 실무 예제: Echo 서버

### 완전한 구현

```kotlin
class NioEchoServer(private val port: Int) {
  private val selector = Selector.open()
  private val log = LoggerFactory.getLogger(javaClass)

  fun start() {
    // 서버 채널 설정
    val serverChannel = ServerSocketChannel.open()
    serverChannel.configureBlocking(false)
    serverChannel.socket().bind(InetSocketAddress(port))
    serverChannel.register(selector, SelectionKey.OP_ACCEPT)

    log.info("Echo 서버 시작: $port")

    // 이벤트 루프
    while (true) {
      selector.select() // 이벤트 대기

      val keys = selector.selectedKeys().iterator()
      while (keys.hasNext()) {
        val key = keys.next()
        keys.remove()

        try {
          when {
            key.isValid.not() -> continue
            key.isAcceptable -> accept(key)
            key.isReadable -> read(key)
          }
        } catch (e: Exception) {
          log.error("이벤트 처리 실패", e)
          key.cancel()
          key.channel().close()
        }
      }
    }
  }

  private fun accept(key: SelectionKey) {
    val serverChannel = key.channel() as ServerSocketChannel
    val clientChannel = serverChannel.accept()

    if (clientChannel != null) {
      clientChannel.configureBlocking(false)
      clientChannel.register(selector, SelectionKey.OP_READ)
      log.info("새 연결: ${clientChannel.remoteAddress}")
    }
  }

  private fun read(key: SelectionKey) {
    val channel = key.channel() as SocketChannel
    val buffer = ByteBuffer.allocate(256)

    val bytesRead = channel.read(buffer)

    when {
      bytesRead == -1 -> {
        log.info("연결 종료: ${channel.remoteAddress}")
        channel.close()
        key.cancel()
      }
      bytesRead > 0 -> {
        buffer.flip()
        val message = String(buffer.array(), 0, bytesRead)
        log.info("수신: $message")

        // Echo back
        buffer.rewind()
        channel.write(buffer)
      }
    }
  }
}

// 사용
fun main() {
  val server = NioEchoServer(8080)
  server.start()
}
```

### 클라이언트 예제

```kotlin
class NioEchoClient(private val host: String, private val port: Int) {
  private val log = LoggerFactory.getLogger(javaClass)

  fun send(message: String): String {
    val channel = SocketChannel.open()
    channel.connect(InetSocketAddress(host, port))

    // 메시지 전송
    val writeBuffer = ByteBuffer.wrap(message.toByteArray())
    channel.write(writeBuffer)
    log.info("전송: $message")

    // 응답 수신
    val readBuffer = ByteBuffer.allocate(256)
    val bytesRead = channel.read(readBuffer)
    readBuffer.flip()

    val response = String(readBuffer.array(), 0, bytesRead)
    log.info("수신: $response")

    channel.close()
    return response
  }
}

// 사용
fun main() {
  val client = NioEchoClient("localhost", 8080)
  val response = client.send("Hello NIO!")
  println("응답: $response")
}
```

## 장단점

### 장점

#### 1. 높은 동시성
```
전통적 방식: 1000개 연결 = 1000개 Thread
NIO: 1000개 연결 = 1~8개 Thread
```

#### 2. 낮은 메모리 사용
```
Thread 1개 = 약 1MB 스택 메모리
1000개 Thread = 1GB
NIO (1개 Thread) = 1MB
```

#### 3. Context Switching 최소화
```
Thread 전환 비용 = 수십 마이크로초
단일 Thread = Context Switching 없음
```

#### 4. C10K 문제 해결
```
C10K: 10,000개 동시 연결 처리
NIO Select Loop로 해결 가능
```

### 단점

#### 1. 복잡한 코드
```kotlin
// Blocking: 직관적
val data = socket.read()
process(data)

// NIO: 복잡한 상태 관리
when {
  key.isReadable -> {
    val bytesRead = channel.read(buffer)
    if (bytesRead > 0) {
      buffer.flip()
      // 상태 관리 필요
    }
  }
}
```

#### 2. 디버깅 어려움
- 비동기 흐름 추적 힘듦
- 이벤트 기반 로직 이해 필요
- 상태 관리 복잡

#### 3. CPU-bound 작업 부적합
```kotlin
// I/O-bound: NIO 유리
channel.read(buffer) // 즉시 반환

// CPU-bound: NIO 불리
for (i in 0..1000000) {
  compute() // Event Loop 블로킹
}
```

#### 4. 순차 처리
- 하나의 이벤트 처리가 느리면 전체 지연
- CPU 코어 활용 제한 (단일 Thread)

## 성능 비교

### Throughput (처리량)

```
환경: 1000개 동시 연결, 각 1KB 데이터

Blocking I/O:
- Thread Pool (200 Threads)
- Throughput: ~5000 req/sec
- Latency: 평균 200ms

NIO Select Loop:
- Single Thread
- Throughput: ~15000 req/sec
- Latency: 평균 50ms
```

### 메모리 사용량

```
Blocking I/O (1000 연결):
- Thread: 1000개 × 1MB = 1GB
- Socket Buffer: 1000개 × 8KB = 8MB
- 총합: ~1GB

NIO (1000 연결):
- Thread: 1개 × 1MB = 1MB
- Socket Buffer: 1000개 × 8KB = 8MB
- Selector: ~1MB
- 총합: ~10MB
```

## 언제 사용하는가?

### NIO Select Loop 적합한 경우

**높은 동시 연결 수**
- Chat 서버 (수천 명 동시 접속)
- WebSocket 서버
- Game 서버

**I/O-bound 작업**
- 프록시 서버
- API Gateway
- 메시지 브로커

**긴 Connection 유지**
- Long Polling
- Server-Sent Events (SSE)
- Keep-Alive 연결

### Blocking I/O 적합한 경우

**간단한 로직**
- 단순 CRUD
- 적은 동시 연결 (< 100)

**CPU-bound 작업**
- 이미지 처리
- 데이터 암호화
- 복잡한 계산

**빠른 개발**
- 프로토타입
- 내부 시스템

## 주요 개념 정리

### 동기 + 논블록킹 = NIO Select Loop

| 구분          | 설명                  | 예시                        |
|-------------|---------------------|---------------------------|
| **동기**      | 이벤트 루프가 순차 실행       | `select()` → 이벤트 처리 → 반복  |
| **논블록킹**    | I/O가 Thread 블로킹 안 함 | `read()`가 즉시 반환 (데이터 없어도) |
| **Polling** | 주기적으로 상태 확인         | `select()`로 준비된 이벤트 확인    |

### 핵심 동작

```
1. Selector.select() → 이벤트 대기 (동기)
2. 이벤트 준비 → 처리 시작
3. channel.read() → 즉시 반환 (논블록킹)
4. 다른 채널 처리 → 계속 진행
5. 루프 반복 → 다음 이벤트 대기
```

### 성능 특성

| 항목        | Blocking I/O          | NIO Select Loop        |
|-----------|-----------------------|------------------------|
| Thread 모델 | Thread-per-Connection | Single Thread (or Few) |
| 확장성       | 제한적 (~수백 개)           | 높음 (~수만 개)             |
| 메모리       | 높음                    | 낮음                     |
| 코드 복잡도    | 낮음                    | 높음                     |
| 적합한 작업    | CPU-bound             | I/O-bound              |

### Reactor vs NIO

**NIO Select Loop는 Reactor Pattern의 구현체**

```
Reactor Pattern:
1. Event Demultiplexer (Selector)
2. Event Handler (채널 처리)
3. Synchronous Event Loop

→ Spring Reactor는 이를 고수준 API로 추상화
```

**관계:**
```
NIO Select Loop (Low-level)
    ↓
Netty (Mid-level Framework)
    ↓
Spring Reactor (High-level Reactive API)
```

## 마무리

NIO Select Loop는 **동기 + 논블록킹**의 대표적인 구현으로:
- **하나의 Thread**로 **수천 개 연결** 처리
- **I/O-bound** 작업에 최적화
- **높은 동시성**이 필요한 서버 구현의 기반

현대 고성능 서버 (Netty, Vert.x, Node.js)의 핵심 기술이며,
**Reactive Programming**의 토대가 되는 중요한 개념입니다.
