package tutorials.thread.basic

import org.apache.commons.logging.LogFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask

class ThreadBasicTest {

  val log = LogFactory.getLog(javaClass)

  @Test
  fun thread() {
    val thread = Thread {
      log.info("run")
    }

    log.info("start")
    thread.start()
    log.info("end")
  }

  @Test
  fun runnable() {
    val runnable = Runnable {
      log.info("run")
    }

    log.info("start 1..")
    runnable.run() // method call
    log.info("end 1..")

    log.info("start 2..")
    Thread(runnable).start()
    log.info("end 2..")

    val executor = Executors.newSingleThreadExecutor()
    log.info("start 3..")
    executor.execute(runnable)
    log.info("end 3..")
  }

  @Test
  fun callable() {
    val callable = Callable {
      log.info("call")
      "result"
    }

    val executor = Executors.newFixedThreadPool(1)

    log.info("start 1..")
    val future = executor.submit(callable)
    log.info("end 1..")

    val futureTask = FutureTask(callable)
    val thread = Thread(futureTask)
    log.info("start 2..")
    thread.start()
    log.info("end 2..")

    assertThat(future.get()).isEqualTo("result")
    assertThat(futureTask.get()).isEqualTo("result")
  }

  @Test
  fun futureRunnable() {
    val runnable = Runnable {
      log.info("run")
    }

    val executor = Executors.newFixedThreadPool(1)
    log.info("start")
    val future = executor.submit(runnable)
    log.info("end")

    assertThat(future.get()).isNull() // 완료 대기용
  }

  @Test
  fun futureCallable() {
    val callable = Callable {
      log.info("call")
      "result"
    }

    val executor = Executors.newFixedThreadPool(1)
    log.info("start")
    val future = executor.submit(callable)
    log.info("end")

    assertThat(future.get()).isEqualTo("result")
  }

  @Test
  fun completableFuture() {
    val future = CompletableFuture
      .supplyAsync { "Hello" }
      .thenApply { result -> "$result World" }
      .thenApply { result -> result.uppercase() }

    assertThat(future.get()).isEqualTo("HELLO WORLD")
  }

}
