package tutorials.thread.basic

import org.apache.commons.logging.LogFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.util.StopWatch
import java.util.Collections
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ConcurrentCollectionsTest {

  val log = LogFactory.getLog(javaClass)

  // ===== Thread-unsafe Collection 문제점 =====
  @Test
  fun threadUnsafeCollectionTest() {
    log.info("\n=== 1. HashMap (Thread-unsafe) ===")
    threadUnsafeHashMap()

    Thread.sleep(500)

    log.info("\n=== 2. Collections.synchronizedMap의 한계 ===")
    synchronizedMapLimitation()
  }

  /**
   * HashMap은 Thread-unsafe
   * - 동시 수정 시 데이터 손실 가능
   */
  private fun threadUnsafeHashMap() {
    val map = HashMap<String, Int>()
    val executor = Executors.newFixedThreadPool(10)

    // 10개 Thread가 동시에 카운터 증가 (각 1000번)
    val futures = (0 until 10).map {
      executor.submit {
        repeat(10_00) {
          val count = map.getOrDefault("count", 0)
          map["count"] = count + 1
        }
      }
    }

    futures.forEach { it.get() }
    executor.shutdown()

    val result = map["count"] ?: 0
    // 예상: 10_000, 실제: 10_000보다 작을 수 있음
    log.info("HashMap 결과: $result (예상: 10,000)")

    // Thread-unsafe이므로 10000보다 작거나 같을 수 있음
    assertTrue(result <= 10_000, "결과가 10,000을 초과하면 안됨")
    log.warn("Thread-unsafe: 예상 10,000, 실제 $result (데이터 손실 발생)")
  }

  /**
   * Collections.synchronizedMap의 한계
   * - 복합 연산(check-then-act)은 여전히 불안전
   */
  private fun synchronizedMapLimitation() {
    val map = Collections.synchronizedMap(HashMap<String, Int>())
    val executor = Executors.newFixedThreadPool(10)

    val futures = (0 until 10).map {
      executor.submit {
        repeat(10_00) {
          // 문제: containsKey()와 put() 사이에 다른 Thread가 끼어들 수 있음
          if (!map.containsKey("count")) {
            map["count"] = 0
          }
          map["count"] = map["count"]!! + 1
        }
      }
    }

    futures.forEach { it.get() }
    executor.shutdown()

    val result = map["count"] ?: 0
    log.info("synchronizedMap 결과: $result (복합 연산은 여전히 불안전)")

    // 복합 연산이 불안전하므로 10000이 아닐 수 있음
    assertTrue(result <= 10_000, "결과가 10,000을 초과하면 안됨")
    if (result < 10_000) {
      log.warn("복합 연산 불안전: 예상 10,000, 실제 $result")
    }
  }

  // ===== ConcurrentHashMap =====
  @Test
  fun concurrentHashMapTest() {
    log.info("\n=== 1. ConcurrentHashMap 기본 사용 ===")
    concurrentHashMapBasic()

    Thread.sleep(500)

    log.info("\n=== 2. 원자적 복합 연산 ===")
    atomicOperations()

    Thread.sleep(500)

    log.info("\n=== 3. 벌크 연산 ===")
    bulkOperations()
  }

  /**
   * ConcurrentHashMap 기본 사용
   * - Thread-safe
   * - 높은 동시성
   */
  private fun concurrentHashMapBasic() {
    val map = ConcurrentHashMap<String, Int>()
    val executor = Executors.newFixedThreadPool(10)

    val futures = (0 until 10).map {
      executor.submit {
        repeat(1_000) {
          map.merge("count", 1) { old, new -> old + new } // 원자적 증가
        }
      }
    }

    futures.forEach { it.get() }
    executor.shutdown()

    val result = map["count"]!!
    log.info("ConcurrentHashMap 결과: $result (예상: 10,000)")

    // ConcurrentHashMap은 Thread-safe하므로 정확히 10000이어야 함
    assertEquals(10_000, result, "ConcurrentHashMap은 Thread-safe해야 함")
  }

  /**
   * 원자적 복합 연산
   */
  private fun atomicOperations() {
    val map = ConcurrentHashMap<String, String>()

    // 1. putIfAbsent: 키가 없을 때만 추가
    val prev1 = map.putIfAbsent("key1", "value1")
    log.info("putIfAbsent 결과: $prev1 (null이면 새로 추가됨)")
    assertNull(prev1, "첫 번째 putIfAbsent는 null을 반환해야 함")

    val prev2 = map.putIfAbsent("key1", "value2")
    log.info("putIfAbsent 결과: $prev2 (기존 값 반환)")
    assertEquals("value1", prev2, "두 번째 putIfAbsent는 기존 값을 반환해야 함")
    assertEquals("value1", map["key1"], "값이 변경되지 않아야 함")

    // 2. computeIfAbsent: 캐시 패턴
    val cacheMap = ConcurrentHashMap<String, String>()
    val value = cacheMap.computeIfAbsent("expensive") { key ->
      log.info("[$key] 비싼 계산 수행 중...")
      Thread.sleep(100)
      "computed-$key"
    }
    log.info("computeIfAbsent 결과: $value")

    // 두 번째 호출 시 계산 안 함
    val cached = cacheMap.computeIfAbsent("expensive") { key ->
      log.info("[$key] 이 로그는 출력되지 않음")
      "should-not-compute"
    }
    log.info("캐시된 값: $cached")
    assertEquals(value, cached, "캐시된 값이 동일해야 함")
    assertEquals("computed-expensive", cached, "계산된 값이 반환되어야 함")

    // 3. merge: 카운터 증가
    val counterMap = ConcurrentHashMap<String, Int>()
    counterMap.merge("visits", 1) { old, new -> old + new }
    counterMap.merge("visits", 1) { old, new -> old + new }
    counterMap.merge("visits", 1) { old, new -> old + new }
    log.info("merge 결과: ${counterMap["visits"]} (예상: 3)")
    assertEquals(3, counterMap["visits"], "merge로 카운터가 3이 되어야 함")

    // 4. replace: 조건부 교체
    map["key1"] = "old-value"
    val replaced = map.replace("key1", "old-value", "new-value")
    log.info("replace 결과: $replaced, 현재 값: ${map["key1"]}")
    assertTrue(replaced, "교체가 성공해야 함")
    assertEquals("new-value", map["key1"], "값이 새 값으로 변경되어야 함")
  }

  /**
   * 벌크 연산 (Java 8+)
   */
  private fun bulkOperations() {
    val map = ConcurrentHashMap<String, Int>()
    map["A"] = 1
    map["B"] = 2
    map["C"] = 3
    map["D"] = 4

    // forEach: 각 엔트리 처리
    log.info("forEach:")
    map.forEach(1) { key, value ->
      log.info("  $key: $value")
    }

    // search: 조건 만족하는 첫 번째 값 반환
    val result = map.search(1) { key, value ->
      if (value > 2) key else null
    }
    log.info("search (value > 2): $result")

    // reduce: 모든 값 합산
    val sum = map.reduce(
      1,
      { _, value -> value },
      { v1, v2 -> v1 + v2 }
    )
    log.info("reduce (합계): $sum")
  }

  // ===== CopyOnWriteArrayList =====
  @Test
  fun copyOnWriteArrayListTest() {
    log.info("\n=== 1. CopyOnWriteArrayList 기본 사용 ===")
    copyOnWriteBasic()

    Thread.sleep(500)

    log.info("\n=== 2. 반복 중 수정 안전 ===")
    iterationSafety()

    Thread.sleep(500)

    log.info("\n=== 3. 리스너 패턴 예제 ===")
    listenerPattern()
  }

  /**
   * CopyOnWriteArrayList 기본 사용
   * - 쓰기 시 배열 복사
   * - 읽기는 락 없음
   */
  private fun copyOnWriteBasic() {
    val list = CopyOnWriteArrayList<String>()

    list.add("A")
    list.add("B")
    list.add("C")

    log.info("리스트 내용:")
    for (item in list) {
      log.info("  $item")
    }

    assertEquals(3, list.size, "리스트 크기가 3이어야 함")
    assertEquals(listOf("A", "B", "C"), list, "리스트 내용이 일치해야 함")
  }

  /**
   * 반복 중 수정 안전
   * - ConcurrentModificationException 발생 안 함
   */
  private fun iterationSafety() {
    val list = CopyOnWriteArrayList(listOf("A", "B", "C"))

    val executor = Executors.newFixedThreadPool(2)

    // Thread 1: 반복
    executor.submit {
      log.info("Thread 1 - 반복 시작")
      for (item in list) {
        log.info("  읽기: $item")
        Thread.sleep(100)
      }
      log.info("Thread 1 - 반복 완료")
    }

    // Thread 2: 동시에 수정
    executor.submit {
      Thread.sleep(50)
      log.info("Thread 2 - 요소 추가")
      list.add("D")
      log.info("Thread 2 - 현재 리스트: $list")
    }

    executor.shutdown()
    executor.awaitTermination(5, TimeUnit.SECONDS)

    log.info("최종 리스트: $list")

    // 반복 중 수정되었지만 ConcurrentModificationException 없음
    assertEquals(4, list.size, "D가 추가되어 크기가 4여야 함")
    assertTrue(list.contains("D"), "D가 포함되어야 함")
    assertEquals(listOf("A", "B", "C", "D"), list, "최종 리스트가 일치해야 함")
  }

  class EventSource {
    private val listeners = CopyOnWriteArrayList<(String) -> Unit>()

    fun addListener(listener: (String) -> Unit) {
      listeners.add(listener)
    }

    fun fireEvent(event: String) {
      for (listener in listeners) {
        listener(event)
      }
    }
  }

  /**
   * 리스너 패턴 예제
   * - 읽기가 훨씬 많은 경우
   */
  private fun listenerPattern() {
    val eventSource = EventSource()

    // 리스너 등록 (드물게 발생)
    eventSource.addListener { event ->
      log.info("  Listener 1 - 이벤트: $event")
    }
    eventSource.addListener { event ->
      log.info("  Listener 2 - 이벤트: $event")
    }

    // 이벤트 발생 (빈번하게 발생)
    eventSource.fireEvent("Event-1")
    eventSource.fireEvent("Event-2")

    // 반복 중에도 리스너 추가 가능
    Thread {
      Thread.sleep(50)
      eventSource.addListener { event ->
        log.info("  Listener 3 (동적 추가) - 이벤트: $event")
      }
    }.start()

    Thread.sleep(100)
    eventSource.fireEvent("Event-3")
  }

  // ===== BlockingQueue =====
  @Test
  fun blockingQueueTest() {
    log.info("\n=== 1. ArrayBlockingQueue (Producer-Consumer) ===")
    producerConsumerPattern()

    Thread.sleep(500)

    log.info("\n=== 2. BlockingQueue 메서드 비교 ===")
    blockingQueueMethods()
  }

  /**
   * Producer-Consumer 패턴
   * - BlockingQueue로 간단하게 구현
   */
  private fun producerConsumerPattern() {
    val queue = ArrayBlockingQueue<String>(10)
    val executor = Executors.newFixedThreadPool(2)

    // Producer
    executor.submit {
      log.info("Producer 시작")
      for (i in 1..20) {
        queue.put("item-$i")
        log.info("  생산: item-$i (큐 크기: ${queue.size})")
        Thread.sleep(50)
      }
      queue.put("DONE") // 종료 신호
      log.info("Producer 완료")
    }

    // Consumer
    executor.submit {
      log.info("Consumer 시작")
      while (true) {
        val item = queue.take() // 큐가 비면 대기
        if (item == "DONE") break

        log.info("  소비: $item (큐 크기: ${queue.size})")
        Thread.sleep(100) // 처리 시간
      }
      log.info("Consumer 완료")
    }

    executor.shutdown()
    executor.awaitTermination(10, TimeUnit.SECONDS)

    // 모든 아이템이 소비되어 큐가 비어있어야 함
    assertTrue(queue.isEmpty(), "큐가 비어있어야 함")
    assertEquals(0, queue.size, "큐 크기가 0이어야 함")
  }

  /**
   * BlockingQueue 메서드 비교
   */
  private fun blockingQueueMethods() {
    val queue = ArrayBlockingQueue<String>(2)

    // 1. add() - 예외 던짐
    queue.add("item1")
    queue.add("item2")
    try {
      queue.add("item3")
    } catch (e: IllegalStateException) {
      log.info("add() 실패: ${e.message}")
    }

    // 2. offer() - 특수 값 반환
    val offered = queue.offer("item3")
    log.info("offer() 결과: $offered (false)")
    assertFalse(offered, "큐가 가득 차서 offer는 false를 반환해야 함")

    // 3. poll() - 특수 값 반환
    val poll1 = queue.poll()
    log.info("poll(): $poll1")
    assertEquals("item1", poll1, "첫 번째 poll은 item1을 반환해야 함")
    assertEquals(1, queue.size)

    val poll2 = queue.poll()
    log.info("poll(): $poll2")
    log.info("queue size: ${queue.size}")
    assertEquals("item2", poll2, "두 번째 poll은 item2를 반환해야 함")
    assertEquals(0, queue.size)

    val poll3 = queue.poll()
    log.info("poll() 빈 큐: $poll3 (null)")
    assertNull(poll3, "빈 큐에서 poll은 null을 반환해야 함")

    // 4. offer/poll with timeout
    queue.clear()
    log.info("queue clear..")

    val offerResult = queue.offer("item1", 100, TimeUnit.MILLISECONDS)
    log.info("offer with timeout: $offerResult")
    assertTrue(offerResult, "타임아웃 offer는 성공해야 함")
    assertEquals(1, queue.size)

    val pollResult = queue.poll(100, TimeUnit.MILLISECONDS)
    log.info("poll with timeout: $pollResult")
    assertEquals("item1", pollResult, "타임아웃 poll은 item1을 반환해야 함")
  }

  // ===== ConcurrentLinkedQueue =====
  @Test
  fun concurrentLinkedQueueTest() {
    log.info("\n=== ConcurrentLinkedQueue (Lock-free) ===")

    val queue = ConcurrentLinkedQueue<String>()
    val executor = Executors.newFixedThreadPool(5)

    // 여러 Thread가 동시에 추가
    val futures = (1..5).map { threadNum ->
      executor.submit {
        repeat(3) { i ->
          val item = "Thread-$threadNum-Item-$i"
          queue.offer(item)
          log.info("추가: $item")
          Thread.sleep(10)
        }
      }
    }

    futures.forEach { it.get() }

    // 소비
    log.info("\n큐 내용:")
    var count = 0
    while (true) {
      val item = queue.poll() ?: break
      log.info("  $item")
      count++
    }
    log.info("총 아이템 수: $count (예상: 15)")

    // 5개 Thread * 3개 아이템 = 15개
    assertEquals(15, count, "총 아이템 수가 15개여야 함")
    assertTrue(queue.isEmpty(), "모든 아이템이 소비되어 큐가 비어있어야 함")

    executor.shutdown()
  }

  // ===== 성능 비교 =====

  @Test
  fun performanceComparisonTest() {
    log.info("=== 성능 비교: ConcurrentHashMap vs CopyOnWriteArrayList ===")

    val iterations = 10_000
    val threads = 10

    // 1. ConcurrentHashMap
    val stopWatchMap = StopWatch()
    stopWatchMap.start()

    val map = ConcurrentHashMap<Int, String>()

    val mapExecutor = Executors.newFixedThreadPool(threads)
    val mapFutures = (0 until threads).map { _ ->
      mapExecutor.submit {
        repeat(iterations) { i ->
          //R:W = 7:3
          if (i % 10 < 7) {
            map[i % 100]
          } else {
            map[i] = "value-$i"
          }
        }
      }
    }
    mapFutures.forEach { it.get() }
    mapExecutor.shutdown()

    stopWatchMap.stop()
    val mapTime = stopWatchMap.totalTimeMillis
    log.info("ConcurrentHashMap (읽기 70% / 쓰기 30%): ${mapTime}ms")

    // 2. CopyOnWriteArrayList
    val stopWatchList = StopWatch()
    stopWatchList.start()

    val list = CopyOnWriteArrayList<String>()

    val listExecutor = Executors.newFixedThreadPool(threads)
    val listFutures = (0 until threads).map { _ ->
      listExecutor.submit {
        repeat(iterations) { i ->
          //R:W = 9:1
          if (i % 10 == 0) {
            list.add("value-$i")
          } else {
            list.size
          }
        }
      }
    }
    listFutures.forEach { it.get() }
    listExecutor.shutdown()

    stopWatchList.stop()
    val readTime = stopWatchList.totalTimeMillis
    log.info("CopyOnWriteArrayList (읽기 90% / 쓰기 10%): ${readTime}ms")

    // 3. CopyOnWriteArrayList (읽기 70% / 쓰기 30%) - 일반 웹 애플리케이션 비율로 테스트
    val stopWatchListWrite = StopWatch()
    stopWatchListWrite.start()

    val listWrite = CopyOnWriteArrayList<String>()

    val listWriteExecutor = Executors.newFixedThreadPool(threads)
    val listWriteFutures = (0 until threads).map {
      listWriteExecutor.submit {
        repeat(iterations) { i ->
          //R:W = 7:3
          if (i % 10 < 7) {
            listWrite.size
          } else {
            listWrite.add("value-$i")
          }
        }
      }
    }
    listWriteFutures.forEach { it.get() }
    listWriteExecutor.shutdown()

    stopWatchListWrite.stop()
    val readWriteTime = stopWatchListWrite.totalTimeMillis
    log.info("CopyOnWriteArrayList (읽기 70% / 쓰기 30%): ${readWriteTime}ms")

    // 성능 검증
    assertTrue(readTime < readWriteTime,
      "CopyOnWriteArrayList는 읽기 집약적일 때(${readTime}ms)가 쓰기가 많을 때(${readWriteTime}ms)보다 빨라야 함")

    val performanceDiff = readWriteTime.toDouble() / readTime.toDouble()
    log.info("성능 차이: CopyOnWriteArrayList 쓰기 비율 증가 시 %.1f배 느림".format(performanceDiff))
  }
}
