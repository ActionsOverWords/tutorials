package tutorials.multitenancy.config

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TenantContextTest {

  @AfterEach
  fun tearDown() {
    TenantContext.clear()
  }

  @Test
  fun `테넌트 설정 시 현재 스레드에 저장되어야 한다`() {
    val tenant = "tenant-a"

    TenantContext.setTenant(tenant)

    assertEquals(tenant, TenantContext.getTenant())
  }

  @Test
  fun `테넌트가 설정되지 않은 경우 null을 반환해야 한다`() {
    val tenant = TenantContext.getTenant()

    assertNull(tenant)
  }

  @Test
  fun `clear 호출 시 현재 스레드의 테넌트가 제거되어야 한다`() {
    TenantContext.setTenant("tenant-a")

    TenantContext.clear()

    assertNull(TenantContext.getTenant())
  }

  @Test
  fun `기존 테넌트 설정을 덮어쓸 수 있어야 한다`() {
    TenantContext.setTenant("tenant-a")

    TenantContext.setTenant("tenant-b")

    assertEquals("tenant-b", TenantContext.getTenant())
  }

  @Test
  fun `서로 다른 스레드 간에 테넌트가 격리되어야 한다`() {
    val executor = Executors.newFixedThreadPool(2)
    val latch = CountDownLatch(2)
    val results = ConcurrentHashMap<String, String?>()

    executor.submit {
      try {
        TenantContext.setTenant("tenant-a")
        Thread.sleep(50) // Simulate some work
        results["thread1"] = TenantContext.getTenant()
      } finally {
        TenantContext.clear()
        latch.countDown()
      }
    }

    executor.submit {
      try {
        TenantContext.setTenant("tenant-b")
        Thread.sleep(50) // Simulate some work
        results["thread2"] = TenantContext.getTenant()
      } finally {
        TenantContext.clear()
        latch.countDown()
      }
    }

    latch.await(5, TimeUnit.SECONDS)
    executor.shutdown()

    assertEquals("tenant-a", results["thread1"])
    assertEquals("tenant-b", results["thread2"])
  }

  @Test
  fun `한 스레드에서 clear 호출 시 다른 스레드에 영향을 주지 않아야 한다`() {
    val executor = Executors.newFixedThreadPool(2)
    val latch = CountDownLatch(2)
    val results = ConcurrentHashMap<String, String?>()

    executor.submit {
      try {
        TenantContext.setTenant("tenant-a")
        Thread.sleep(100) // Wait longer
        results["thread1"] = TenantContext.getTenant()
      } finally {
        TenantContext.clear()
        latch.countDown()
      }
    }

    executor.submit {
      try {
        TenantContext.setTenant("tenant-b")
        Thread.sleep(50)
        TenantContext.clear() // Clear early
        results["thread2"] = TenantContext.getTenant()
      } finally {
        latch.countDown()
      }
    }

    latch.await(5, TimeUnit.SECONDS)
    executor.shutdown()

    assertEquals("tenant-a", results["thread1"], "Thread 1 should still have its tenant")
    assertNull(results["thread2"], "Thread 2 should have null after clear")
  }

  @Test
  fun `동일 스레드에서 여러 번 설정 시 올바르게 동작해야 한다`() {
    TenantContext.setTenant("tenant-a")
    assertEquals("tenant-a", TenantContext.getTenant())

    TenantContext.setTenant("tenant-b")
    assertEquals("tenant-b", TenantContext.getTenant())

    TenantContext.setTenant("tenant-c")
    assertEquals("tenant-c", TenantContext.getTenant())

    TenantContext.clear()
    assertNull(TenantContext.getTenant())
  }

  @Test
  fun `다수의 스레드가 동시 접근 시 격리가 유지되어야 한다`() {
    val threadCount = 10
    val executor = Executors.newFixedThreadPool(threadCount)
    val latch = CountDownLatch(threadCount)
    val results = ConcurrentHashMap<Int, String?>()

    repeat(threadCount) { index ->
      executor.submit {
        try {
          val tenant = "tenant-$index"
          TenantContext.setTenant(tenant)
          Thread.sleep((10..50).random().toLong()) // Random delay
          results[index] = TenantContext.getTenant()
        } finally {
          TenantContext.clear()
          latch.countDown()
        }
      }
    }

    latch.await(10, TimeUnit.SECONDS)
    executor.shutdown()

    repeat(threadCount) { index ->
      assertEquals("tenant-$index", results[index], "Thread $index should have its own tenant")
    }
  }
}
