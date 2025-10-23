package tutorials.lgtm

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.logging.LogFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime
import java.util.Random
import kotlin.math.sqrt

@RestController
class TestController(
  meterRegistry: MeterRegistry,
  private val restTemplate: RestTemplate
) {

  private val log = LogFactory.getLog(javaClass)

  private val helloCounter = Counter.builder("api.hello.calls")
    .description("Counts total calls to /hello endpoint")
    .register(meterRegistry)

  @GetMapping("/counter")
  fun counter(): String {
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
  fun cpuTask(): String {
    val operations = 1000000000L
    var result = 0.0
    val startTime = System.nanoTime()

    for (i in 0..<operations) {
      result += sqrt(i.toDouble())
    }

    val endTime = System.nanoTime()

    val durationNano = endTime - startTime
    val durationMillis = durationNano / 1000000.0 // 밀리초(ms)
    val durationSeconds = durationNano / 1000000000.0 // 초(s)

    log.debug("테스트 완료.")
    log.debug("----------------------------------------")
    log.debug("총 걸린 시간: $durationMillis ms")
    log.debug("총 걸린 시간: $durationSeconds s")

    return "cpu_task: $result : [${LocalDateTime.now()}]"
  }

  @GetMapping("/random_sleep")
  @Throws(InterruptedException::class)
  fun randomSSSSSleep(): String {
    Thread.sleep((Math.random() / 5 * 10000).toInt().toLong())
    log.info("random_sleep")
    return "random_sleep"
  }

  @GetMapping("/random_status")
  @Throws(InterruptedException::class)
  fun randomStatus(response: HttpServletResponse): String {
    val givenList = mutableListOf<Int?>(200, 200, 300, 400, 500)
    val rand = Random()
    val randomElement: Int = givenList.get(rand.nextInt(givenList.size))!!
    response.setStatus(randomElement)
    log.info("random_status")
    return "random_status"
  }

  @GetMapping("/error_test")
  @Throws(Exception::class)
  fun errorTest(): String? {
    throw Exception("Error test")
  }

  @GetMapping("/chain")
  fun chain(): String {
    log.debug("chain is starting")
    restTemplate.getForObject("http://localhost:8080/random_sleep", String::class.java)
    restTemplate.getForObject("http://10.2.114.21:8080/io_task", String::class.java)
    restTemplate.getForObject("http://10.2.114.21:8080/cpu_task", String::class.java)
    log.debug("chain is finished")
    return "chain"
  }

}
