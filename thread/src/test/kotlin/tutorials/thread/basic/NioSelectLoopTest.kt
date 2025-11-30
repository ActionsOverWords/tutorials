package tutorials.thread.basic

import org.apache.commons.logging.LogFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class NioSelectLoopTest {

  val log = LogFactory.getLog(javaClass)

  // ===== Buffer 동작 =====
  @Test
  fun bufferOperationTest() {
    log.info("=== 1. Buffer 기본 동작 (position, limit, capacity) ===")
    bufferBasic()

    Thread.sleep(300)

    log.info("=== 2. Buffer flip() 동작 ===")
    bufferFlip()

    Thread.sleep(300)

    log.info("=== 3. Buffer clear() vs compact() ===")
    bufferClearVsCompact()
  }

  /**
   * Buffer 기본 동작
   * - position, limit, capacity
   */
  private fun bufferBasic() {
    val buffer = ByteBuffer.allocate(10)
    log.info("Buffer 생성: capacity=10")
    log.info("  초기 상태: position=${buffer.position()}, limit=${buffer.limit()}, capacity=${buffer.capacity()}")

    assertEquals(0, buffer.position())
    assertEquals(10, buffer.limit())
    assertEquals(10, buffer.capacity())

    // 데이터 쓰기
    buffer.put("ABC".toByteArray())
    log.info("  'ABC' 쓰기 후: position=${buffer.position()}, limit=${buffer.limit()}")
    assertEquals(3, buffer.position())
  }

  /**
   * Buffer flip() 동작
   * - 쓰기 모드 → 읽기 모드 전환
   */
  private fun bufferFlip() {
    val buffer = ByteBuffer.allocate(10)

    // 쓰기
    buffer.put("Hello".toByteArray())
    log.info("쓰기 모드: position=${buffer.position()}, limit=${buffer.limit()}")
    assertEquals(5, buffer.position())
    assertEquals(10, buffer.limit())

    // flip() - 읽기 모드로 전환
    buffer.flip()
    log.info("flip() 후: position=${buffer.position()}, limit=${buffer.limit()}")
    assertEquals(0, buffer.position())
    assertEquals(5, buffer.limit(), "limit이 이전 position으로 설정")

    // 읽기
    val data = ByteArray(buffer.remaining())
    buffer.get(data)
    log.info("  읽은 데이터: ${String(data)}")
    assertEquals("Hello", String(data))
  }

  /**
   * clear() vs compact()
   * - clear(): 전체 버퍼 재사용
   * - compact(): 읽지 않은 데이터 보존
   */
  private fun bufferClearVsCompact() {
    // clear() 테스트
    val buffer1 = ByteBuffer.allocate(10)
    buffer1.put("ABCDE".toByteArray())
    log.info("clear() 테스트:")
    log.info("  쓰기 후: position=${buffer1.position()}")

    buffer1.clear()
    log.info("  clear() 후: position=${buffer1.position()}, limit=${buffer1.limit()}")
    assertEquals(0, buffer1.position())
    assertEquals(10, buffer1.limit())

    buffer1.flip()
    val data = ByteArray(buffer1.remaining())
    buffer1.get(data)
    log.info("  읽은 데이터: [${String(data)}]")
    assertEquals("", String(data))

    // compact() 테스트
    val buffer2 = ByteBuffer.allocate(10)
    buffer2.put("ABCDE".toByteArray())
    buffer2.flip()
    buffer2.get() // 'A' 읽기
    buffer2.get() // 'B' 읽기
    log.info("compact() 테스트:")
    log.info("  2바이트 읽은 후: position=${buffer2.position()}, remaining=${buffer2.remaining()}")

    buffer2.compact()
    log.info("  compact() 후: position=${buffer2.position()}, remaining=${buffer2.remaining()}")
    assertEquals(3, buffer2.position(), "읽지 않은 3바이트(CDE)가 앞으로 이동")

    // 확인
    buffer2.flip()
    val remaining = ByteArray(buffer2.remaining())
    buffer2.get(remaining)
    log.info("  보존된 데이터: ${String(remaining)}")
    assertEquals("CDE", String(remaining))
  }

  // ===== Channel 기본 동작 =====
  @Test
  fun channelBasicTest() {
    log.info("=== 1. ServerSocketChannel 생성 및 설정 ===")
    serverSocketChannelBasic()

    Thread.sleep(300)

    log.info("=== 2. SocketChannel 논블록킹 동작 ===")
    socketChannelNonBlocking()

    Thread.sleep(300)

    log.info("=== 3. Channel 양방향 통신 ===")
    channelBidirectional()
  }

  /**
   * ServerSocketChannel 기본 설정
   * - 논블록킹 모드 설정
   * - 포트 바인딩
   */
  private fun serverSocketChannelBasic() {
    val serverChannel = ServerSocketChannel.open()
    log.info("ServerSocketChannel 생성")

    // 논블록킹 모드 설정
    serverChannel.configureBlocking(false)
    log.info("  논블록킹 모드 설정: ${!serverChannel.isBlocking}")
    assertTrue(!serverChannel.isBlocking, "논블록킹 모드여야 함")

    // 포트 바인딩
    val port = 19001
    serverChannel.socket().bind(InetSocketAddress(port))
    log.info("  포트 바인딩: $port")
    assertEquals(port, serverChannel.socket().localPort)

    // accept() 즉시 반환 (연결 없으면 null)
    val client = serverChannel.accept()
    log.info("  accept() 즉시 반환: ${client == null}")
    assertEquals(null, client, "연결 없으면 null 반환")

    serverChannel.close()
    log.info("  ServerSocketChannel 종료")
  }

  /**
   * SocketChannel 논블록킹 동작
   * - 논블록킹 read/write
   */
  private fun socketChannelNonBlocking() {
    // 서버 준비
    val serverChannel = ServerSocketChannel.open()
    serverChannel.configureBlocking(false)
    serverChannel.socket().bind(InetSocketAddress(19002))
    log.info("서버 준비: 19002")

    // 클라이언트 연결
    val clientChannel = SocketChannel.open()
    clientChannel.configureBlocking(false)
    clientChannel.connect(InetSocketAddress("localhost", 19002))
    log.info("클라이언트 연결 시작")

    // 연결 완료 대기
    while (!clientChannel.finishConnect()) {
      log.info("  연결 중...")
      Thread.sleep(10)
    }
    log.info("  연결 완료")

    // 서버 accept
    var accepted: SocketChannel? = null
    while (accepted == null) {
      accepted = serverChannel.accept()
      Thread.sleep(10)
    }
    log.info("서버가 연결 수락")

    // 클라이언트에서 메시지 전송
    val message = "Hello"
    clientChannel.write(ByteBuffer.wrap(message.toByteArray()))
    log.info("  클라이언트: 메시지 전송")

    // 메시지 도착 대기
    Thread.sleep(50)

    // 논블록킹 read (데이터 있으면 읽은 바이트 수 반환)
    val buffer = ByteBuffer.allocate(256)
    buffer.clear()
    val bytesRead = accepted.read(buffer)
    log.info("  논블록킹 read: $bytesRead bytes (데이터 있음)")
    assertTrue(bytesRead > 0, "데이터 있으면 읽은 바이트 수 반환")

    buffer.flip()
    val received = String(buffer.array(), 0, bytesRead)
    assertEquals(message, received)
    log.info("  수신 메시지: $received")

    // 정리
    clientChannel.close()
    accepted.close()
    serverChannel.close()
    log.info("채널 종료")
  }

  /**
   * Channel 양방향 통신
   * - 클라이언트 → 서버
   * - 서버 → 클라이언트 (Echo)
   */
  private fun channelBidirectional() {
    // 서버 준비
    val serverChannel = ServerSocketChannel.open()
    serverChannel.configureBlocking(true) // 블로킹 모드 (테스트 간소화)
    serverChannel.socket().bind(InetSocketAddress(19003))
    log.info("서버 준비: 19003")

    val executor = Executors.newSingleThreadExecutor()
    val latch = CountDownLatch(1)

    // 서버 Thread
    executor.submit {
      val client = serverChannel.accept()
      log.info("  서버: 연결 수락")

      // 읽기
      val buffer = ByteBuffer.allocate(256)
      val bytesRead = client.read(buffer)
      buffer.flip()
      val message = String(buffer.array(), 0, bytesRead)
      log.info("  서버: 수신 = $message")

      // Echo 응답
      val response = "Echo: $message"
      client.write(ByteBuffer.wrap(response.toByteArray()))
      log.info("  서버: 응답 = $response")

      client.close()
      latch.countDown()
    }

    Thread.sleep(100)

    // 클라이언트
    val clientChannel = SocketChannel.open()
    clientChannel.connect(InetSocketAddress("localhost", 19003))
    log.info("클라이언트: 연결 완료")

    // 메시지 전송
    val message = "Hello NIO"
    clientChannel.write(ByteBuffer.wrap(message.toByteArray()))
    log.info("  클라이언트: 전송 = $message")

    // 응답 수신
    val buffer = ByteBuffer.allocate(256)
    val bytesRead = clientChannel.read(buffer)
    buffer.flip()
    val response = String(buffer.array(), 0, bytesRead)
    log.info("  클라이언트: 수신 = $response")

    assertEquals("Echo: $message", response)

    clientChannel.close()
    latch.await(5, TimeUnit.SECONDS)
    executor.shutdown()
    serverChannel.close()
    log.info("양방향 통신 성공")
  }

  // ===== Selector 기본 동작 =====
  @Test
  fun selectorBasicTest() {
    log.info("=== 1. Selector 생성 및 등록 ===")
    selectorRegistration()

    Thread.sleep(300)

    log.info("=== 2. Selector 이벤트 감지 ===")
    selectorEventDetection()

    Thread.sleep(300)

    log.info("=== 3. SelectionKey 동작 ===")
    selectionKeyOperation()
  }

  /**
   * Selector 생성 및 채널 등록
   */
  private fun selectorRegistration() {
    val selector = Selector.open()
    log.info("Selector 생성")

    val serverChannel = ServerSocketChannel.open()
    serverChannel.configureBlocking(false)
    serverChannel.socket().bind(InetSocketAddress(19004))

    // 채널 등록
    val key = serverChannel.register(selector, SelectionKey.OP_ACCEPT)
    log.info("  ServerSocketChannel 등록: OP_ACCEPT")

    assertNotNull(key)
    assertTrue(key.isValid)
    assertEquals(SelectionKey.OP_ACCEPT, key.interestOps())
    log.info("  등록 성공: interestOps=${key.interestOps()}")

    selector.close()
    serverChannel.close()
  }

  /**
   * Selector 이벤트 감지
   * - select() vs selectNow()
   */
  private fun selectorEventDetection() {
    val selector = Selector.open()
    val serverChannel = ServerSocketChannel.open()
    serverChannel.configureBlocking(false)
    serverChannel.socket().bind(InetSocketAddress(19005))
    serverChannel.register(selector, SelectionKey.OP_ACCEPT)

    log.info("selectNow() 테스트 (이벤트 없음):")
    val ready1 = selector.selectNow() // 즉시 반환
    log.info("  준비된 채널 수: $ready1")
    assertEquals(0, ready1, "이벤트 없으면 0 반환")

    // 클라이언트 연결 생성
    val executor = Executors.newSingleThreadExecutor()
    executor.submit {
      Thread.sleep(100)
      SocketChannel.open(InetSocketAddress("localhost", 19005))
      log.info("  클라이언트 연결됨")
    }

    log.info("select(timeout) 테스트 (이벤트 대기):")
    val ready2 = selector.select(1000) // 최대 1초 대기
    log.info("  준비된 채널 수: $ready2")
    assertTrue(ready2 > 0, "Accept 이벤트 발생")

    val keys = selector.selectedKeys()
    log.info("  selectedKeys 크기: ${keys.size}")
    assertEquals(1, keys.size)

    val key = keys.first()
    assertTrue(key.isAcceptable, "OP_ACCEPT 이벤트")
    log.info("  이벤트 타입: isAcceptable=${key.isAcceptable}")

    executor.shutdown()
    selector.close()
    serverChannel.close()
  }

  /**
   * SelectionKey 동작
   * - attachment 사용
   */
  private fun selectionKeyOperation() {
    val selector = Selector.open()
    val serverChannel = ServerSocketChannel.open()
    serverChannel.configureBlocking(false)
    serverChannel.socket().bind(InetSocketAddress(19006))

    // attachment 설정
    val metadata = "server-metadata"
    val key = serverChannel.register(selector, SelectionKey.OP_ACCEPT, metadata)
    log.info("SelectionKey attachment 설정: $metadata")

    // attachment 조회
    val attached = key.attachment() as String
    log.info("  attachment 조회: $attached")
    assertEquals(metadata, attached)

    // interestOps 변경
    key.interestOps(0) // 관심 이벤트 제거
    log.info("  interestOps 변경: ${key.interestOps()}")
    assertEquals(0, key.interestOps())

    selector.close()
    serverChannel.close()
    log.info("SelectionKey 테스트 완료")
  }

  // ===== Echo 서버 (Select Loop) =====
  @Test
  fun echoServerTest() {
    log.info("=== 1. Echo 서버 (단일 클라이언트) ===")
    echoServerSingleClient()
  }

  /**
   * Echo 서버 - 단일 클라이언트
   * - 간소화된 버전
   */
  private fun echoServerSingleClient() {
    val port = 19007

    // 서버 준비
    val serverChannel = ServerSocketChannel.open()
    serverChannel.configureBlocking(true) // 블로킹 모드 (간소화)
    serverChannel.socket().bind(InetSocketAddress(port))
    log.info("Echo 서버 준비: $port")

    val executor = Executors.newSingleThreadExecutor()

    // 서버 Thread
    executor.submit {
      val client = serverChannel.accept()
      log.info("  서버: 연결 수락")

      // Echo
      val buffer = ByteBuffer.allocate(256)
      val bytesRead = client.read(buffer)
      buffer.flip()
      val message = String(buffer.array(), 0, bytesRead)
      log.info("  서버: 수신 = $message")

      val response = "Echo: $message"
      client.write(ByteBuffer.wrap(response.toByteArray()))
      log.info("  서버: 응답 = $response")

      client.close()
    }

    Thread.sleep(100)

    // 클라이언트
    val clientChannel = SocketChannel.open()
    clientChannel.connect(InetSocketAddress("localhost", port))
    log.info("클라이언트 연결")

    val message = "Hello"
    clientChannel.write(ByteBuffer.wrap(message.toByteArray()))
    log.info("  클라이언트: 전송 = $message")

    Thread.sleep(100)

    val buffer = ByteBuffer.allocate(256)
    val bytesRead = clientChannel.read(buffer)
    buffer.flip()
    val response = String(buffer.array(), 0, bytesRead)
    log.info("  클라이언트: 수신 = $response")

    assertEquals("Echo: $message", response)

    clientChannel.close()
    executor.shutdown()
    executor.awaitTermination(1, TimeUnit.SECONDS)
    serverChannel.close()
    log.info("Echo 서버 테스트 완료")
  }

  // ===== 논블록킹 동작 확인 =====
  @Test
  fun nonBlockingBehaviorTest() {
    log.info("=== 1. accept() 논블록킹 동작 ===")
    acceptNonBlocking()

    Thread.sleep(300)

    log.info("=== 2. read() 논블록킹 동작 ===")
    readNonBlocking()

    Thread.sleep(300)

    log.info("=== 3. write() 논블록킹 동작 ===")
    writeNonBlocking()
  }

  /**
   * accept() 논블록킹 동작
   * - 연결 없으면 즉시 null 반환
   */
  private fun acceptNonBlocking() {
    val serverChannel = ServerSocketChannel.open()
    serverChannel.configureBlocking(false)
    serverChannel.socket().bind(InetSocketAddress(19009))

    log.info("논블록킹 accept() 호출:")
    val startTime = System.currentTimeMillis()
    val client = serverChannel.accept()
    val elapsed = System.currentTimeMillis() - startTime

    log.info("  결과: $client")
    log.info("  소요 시간: ${elapsed}ms")

    assertEquals(null, client, "연결 없으면 null")
    assertTrue(elapsed < 10, "즉시 반환 (10ms 미만)")

    serverChannel.close()
    log.info("accept() 논블록킹 확인 완료")
  }

  /**
   * read() 논블록킹 동작
   * - 데이터 없으면 즉시 0 반환
   */
  private fun readNonBlocking() {
    // 서버 준비
    val serverChannel = ServerSocketChannel.open()
    serverChannel.configureBlocking(false)
    serverChannel.socket().bind(InetSocketAddress(19010))

    // 클라이언트 연결
    val clientChannel = SocketChannel.open()
    clientChannel.configureBlocking(false)
    clientChannel.connect(InetSocketAddress("localhost", 19010))

    while (!clientChannel.finishConnect()) {
      Thread.sleep(10)
    }

    var accepted: SocketChannel? = null
    while (accepted == null) {
      accepted = serverChannel.accept()
      Thread.sleep(10)
    }
    accepted.configureBlocking(false)

    log.info("논블록킹 read() 호출 (데이터 없음):")
    val buffer = ByteBuffer.allocate(256)
    val startTime = System.currentTimeMillis()
    val bytesRead = accepted.read(buffer)
    val elapsed = System.currentTimeMillis() - startTime

    log.info("  읽은 바이트: $bytesRead")
    log.info("  소요 시간: ${elapsed}ms")

    assertEquals(0, bytesRead, "데이터 없으면 0 반환")
    assertTrue(elapsed < 10, "즉시 반환 (10ms 미만)")

    clientChannel.close()
    accepted.close()
    serverChannel.close()
    log.info("read() 논블록킹 확인 완료")
  }

  /**
   * write() 논블록킹 동작
   * - 버퍼에 쓸 수 있으면 즉시 반환
   */
  private fun writeNonBlocking() {
    // 서버 준비
    val serverChannel = ServerSocketChannel.open()
    serverChannel.configureBlocking(false)
    serverChannel.socket().bind(InetSocketAddress(19011))

    // 클라이언트 연결
    val clientChannel = SocketChannel.open()
    clientChannel.configureBlocking(false)
    clientChannel.connect(InetSocketAddress("localhost", 19011))

    while (!clientChannel.finishConnect()) {
      Thread.sleep(10)
    }

    log.info("논블록킹 write() 호출:")
    val message = "Hello"
    val buffer = ByteBuffer.wrap(message.toByteArray())
    val startTime = System.currentTimeMillis()
    val bytesWritten = clientChannel.write(buffer)
    val elapsed = System.currentTimeMillis() - startTime

    log.info("  쓴 바이트: $bytesWritten")
    log.info("  소요 시간: ${elapsed}ms")

    assertEquals(message.length, bytesWritten)
    assertTrue(elapsed < 10, "즉시 반환 (10ms 미만)")

    clientChannel.close()
    serverChannel.close()
    log.info("write() 논블록킹 확인 완료")
  }

  // ===== 동기 + 논블록킹 확인 =====
  @Test
  fun synchronousPlusNonBlockingTest() {
    log.info("=== 동기 + 논블록킹 Select Loop ===")
    synchronousEventLoop()
  }

  /**
   * 동기 + 논블록킹 확인
   * - 이벤트 루프가 순차적으로 실행 (동기)
   * - I/O 작업이 즉시 반환 (논블록킹)
   */
  private fun synchronousEventLoop() {
    val selector = Selector.open()
    val serverChannel = ServerSocketChannel.open()
    serverChannel.configureBlocking(false)
    serverChannel.socket().bind(InetSocketAddress(19012))
    serverChannel.register(selector, SelectionKey.OP_ACCEPT)

    log.info("Select Loop 시작:")
    val eventOrder = mutableListOf<String>()
    var clientConnected = false
    var messageReceived = false

    // 클라이언트 연결
    val executor = Executors.newSingleThreadExecutor()
    executor.submit {
      Thread.sleep(50)
      val client = SocketChannel.open(InetSocketAddress("localhost", 19012))
      Thread.sleep(50)
      client.write(ByteBuffer.wrap("Test".toByteArray()))
      log.info("  클라이언트: 메시지 전송 완료")
      Thread.sleep(50)
      client.close()
      log.info("  클라이언트: 연결 종료")
    }

    var loopCount = 0
    val maxLoops = 5

    while (loopCount < maxLoops) {
      eventOrder.add("Loop-${++loopCount}: select() 호출")
      log.info("  Loop-$loopCount: select() 호출 (동기: 대기)")

      val ready = selector.select(100) // 동기: 이벤트 대기 (100ms 타임아웃)

      if (ready > 0) {
        eventOrder.add("Loop-$loopCount: 이벤트 처리")
        log.info("  Loop-$loopCount: 이벤트 처리 시작 (순차적)")

        val keys = selector.selectedKeys().iterator()
        while (keys.hasNext()) {
          val key = keys.next()
          keys.remove()

          try {
            when {
              key.isValid && key.isAcceptable -> {
                val server = key.channel() as ServerSocketChannel
                val client = server.accept() // 논블록킹: 즉시 반환
                if (client != null) {
                  client.configureBlocking(false)
                  client.register(selector, SelectionKey.OP_READ)
                  clientConnected = true
                  eventOrder.add("Loop-$loopCount: Accept (논블록킹)")
                  log.info("    Accept 처리 (논블록킹: 즉시 반환)")
                }
              }

              key.isValid && key.isReadable -> {
                val client = key.channel() as SocketChannel
                val buffer = ByteBuffer.allocate(256)
                val bytesRead = client.read(buffer) // 논블록킹: 즉시 반환

                when {
                  bytesRead == -1 -> {
                    eventOrder.add("Loop-$loopCount: Close (논블록킹)")
                    log.info("    연결 종료 (논블록킹: 즉시 반환)")
                    client.close()
                    key.cancel()
                  }
                  bytesRead > 0 -> {
                    messageReceived = true
                    eventOrder.add("Loop-$loopCount: Read $bytesRead bytes (논블록킹)")
                    log.info("    Read $bytesRead bytes (논블록킹: 즉시 반환)")
                  }
                }
              }
            }
          } catch (e: Exception) {
            log.warn("    에러 발생: ${e.message}")
            key.cancel()
          }
        }

        eventOrder.add("Loop-$loopCount: 이벤트 처리 완료")
        log.info("  Loop-$loopCount: 이벤트 처리 완료 (다음 루프로)")
      }

      // 클라이언트 연결 및 메시지 수신 완료 시 종료
      if (clientConnected && messageReceived) {
        log.info("  테스트 완료 조건 충족 (연결: $clientConnected, 수신: $messageReceived)")
        break
      }
    }

    log.info("실행 순서:")
    eventOrder.forEach { log.info("  $it") }

    log.info("동기 + 논블록킹 확인:")
    log.info("  - 동기: 이벤트 루프가 순차적으로 실행")
    log.info("  - 논블록킹: accept(), read()가 즉시 반환")

    assertTrue(eventOrder.any { it.contains("select()") }, "동기: select() 호출")
    assertTrue(eventOrder.any { it.contains("논블록킹") }, "논블록킹: 즉시 반환")
    assertTrue(clientConnected, "클라이언트 연결 성공")
    assertTrue(messageReceived, "메시지 수신 성공")

    executor.shutdown()
    executor.awaitTermination(1, TimeUnit.SECONDS)
    selector.close()
    serverChannel.close()
    log.info("동기 + 논블록킹 테스트 완료")
  }
}
