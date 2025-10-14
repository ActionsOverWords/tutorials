package tutorials.lgtm

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.logging.LogFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Random

@RestController
class TestController(meterRegistry: MeterRegistry) {

  private val log = LogFactory.getLog(javaClass)

  private val helloCounter = Counter.builder("api.hello.calls")
    .description("Counts total calls to /hello endpoint")
    .register(meterRegistry)

  @GetMapping("/hello")
  fun hello(): String {
    // 1. Log 생성 (traceId, spanId가 자동으로 포함됨)
    log.info("Hello endpoint was invoked!")

    // 2. Metric 증가
    helloCounter.increment()

    try {
      Thread.sleep(150) // 가상 작업
      log.warn("Some work is being done...")
      Thread.sleep(50)
    } catch (e: InterruptedException) {
      log.error("Task interrupted", e)
    }

    return "Hello from Spring Boot with Grafana Alloy!"
  }

  @GetMapping("/io_task")
  @Throws(InterruptedException::class)
  fun io_task(): String {
    Thread.sleep(1000)
    log.info("io_task")
    return "io_task"
  }

  @GetMapping("/cpu_task")
  fun cpu_task(): String {
    for (i in 0..999) {
      val tmp = i * i * i
    }
    log.info("cpu_task")
    return "cpu_task"
  }

  @GetMapping("/random_sleep")
  @Throws(InterruptedException::class)
  fun random_sleep(): String {
    Thread.sleep((Math.random() / 5 * 10000).toInt().toLong())
    log.info("random_sleep")
    return "random_sleep"
  }

  @GetMapping("/random_status")
  @Throws(InterruptedException::class)
  fun random_status(response: HttpServletResponse): String {
    val givenList = mutableListOf<Int?>(200, 200, 300, 400, 500)
    val rand = Random()
    val randomElement: Int = givenList.get(rand.nextInt(givenList.size))!!
    response.setStatus(randomElement)
    log.info("random_status")
    return "random_status"
  }

/*
  @GetMapping("/chain")
  @Throws(InterruptedException::class, IOException::class)
  fun chain(): String {
    val TARGET_ONE_HOST = System.getenv().getOrDefault("TARGET_ONE_HOST", "localhost")
    val TARGET_TWO_HOST = System.getenv().getOrDefault("TARGET_TWO_HOST", "localhost")
    log.debug("chain is starting")
    Request.Get("http://localhost:8080/")
      .execute().returnContent()
    Request.Get(String.format("http://%s:8080/io_task", TARGET_ONE_HOST))
      .execute().returnContent()
    Request.Get(String.format("http://%s:8080/cpu_task", TARGET_TWO_HOST))
      .execute().returnContent()
    log.debug("chain is finished")
    return "chain"
  }
*/

  @GetMapping("/error_test")
  @Throws(Exception::class)
  fun error_test(): String? {
    throw Exception("Error test")
  }

}
