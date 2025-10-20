package tutorials.grafanaloki

import org.apache.commons.logging.LogFactory
import org.springframework.boot.logging.LogLevel
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TestController {

  private val log = LogFactory.getLog(javaClass)

  @GetMapping("/log")
  fun log(message: String, level: LogLevel) {
    level.log(log, message)
  }

  @GetMapping("/exception")
  fun exception() {
    throw IllegalStateException("error...")
  }

}
