package tutorials.thread.basic

import org.apache.commons.logging.LogFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock

class SynchronizationTest {

  val log = LogFactory.getLog(javaClass)

  // -- Mutex
  inner class MutexService {
    private val lock = ReentrantLock()
    var count = 0

    fun process() {
      lock.lock()

      try {
        log.info("mutex process..")
        count++
      } finally {
        lock.unlock()
      }
    }
  }

  @Test
  fun mutex() {
    val mutexService = MutexService()
    val threadCount = 5
    val executor = Executors.newFixedThreadPool(threadCount)

    (0 until threadCount).forEach { _ ->
      executor.submit {
        mutexService.process()
      }
    }
    shutdown(executor)

    assertThat(mutexService.count).isEqualTo(threadCount)
  }

  // -- Semaphore
  @Test
  fun semaphoreAcquire() {
    val semaphore = Semaphore(2)

    assertAll(
      { assertThat(semaphore.tryAcquire()).isTrue },
      { assertThat(semaphore.availablePermits()).isEqualTo(1) },

      { assertThat(semaphore.tryAcquire()).isTrue },
      { assertThat(semaphore.availablePermits()).isEqualTo(0) },

      { assertThat(semaphore.tryAcquire()).isFalse },
    )
  }

  inner class SemaphoreService(
    permits: Int,
  ) {
    private val semaphore = Semaphore(permits)

    //@Volatile
    var count = 0

    fun process() {
      log.info("try semaphore process..")

      try {
        semaphore.acquire()

        synchronized(this) {
          count++
        }

        log.info("semaphore process.. count: $count")
        Thread.sleep(100)
      } finally {
        semaphore.release()
        log.info("semaphore release..")
      }
    }

    fun availablePermits() = semaphore.availablePermits()
  }

  @Test
  fun semaphore() {
    val permits = 1
    val semaphoreService = SemaphoreService(permits)

    val threadCount = 5
    val executor = Executors.newFixedThreadPool(threadCount)

    (0 until threadCount).forEach { _ ->
      executor.submit {
        semaphoreService.process()
      }
    }
    shutdown(executor)

    assertAll(
      { assertThat(semaphoreService.count).isEqualTo(threadCount) },
      { assertThat(semaphoreService.availablePermits()).isEqualTo(permits) },
    )
  }

  inner class ReadWriteLockService {
    private val readWriteLock = ReentrantReadWriteLock()
    private val readLock = readWriteLock.readLock()
    private val writeLock = readWriteLock.writeLock()
    private val cache = mutableMapOf<String, String>()

    fun get(key: String): String? {
      readLock.lock()
      try {
        log.info("read lock..")
        return cache[key]
      } finally {
        readLock.unlock()
      }
    }

    fun put(key: String, value: String) {
      writeLock.lock()
      try {
        log.info("write lock..")
        cache[key] = value
      } finally {
        writeLock.unlock()
      }
    }
  }

  @Test
  fun readWriteLockTest() {
    val readWriteLockService = ReadWriteLockService()

    val putCallable = { readWriteLockService.put("key", "value") }
    val getCallable = { readWriteLockService.get("key") }

    val executor = Executors.newFixedThreadPool(2)

    executor.execute(putCallable)
    val getFuture = executor.submit(getCallable)

    shutdown(executor)
    assertThat(getFuture.get()).isEqualTo("value")
  }

  private fun shutdown(executor: ExecutorService) {
    executor.shutdown()

    while (!executor.isTerminated) {
      Thread.sleep(100)
    }
  }

}
