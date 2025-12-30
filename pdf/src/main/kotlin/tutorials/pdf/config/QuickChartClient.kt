package tutorials.pdf.config

import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange

interface QuickChartClient {

  @GetExchange("/chart")
  fun generateChart(@RequestParam("c") queryParam: String): ByteArray

}
