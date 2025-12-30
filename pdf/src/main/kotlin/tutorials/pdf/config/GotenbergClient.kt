package tutorials.pdf.config

import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.service.annotation.PostExchange

interface GotenbergClient {

  @PostExchange(
    url = "/forms/chromium/convert/html",
    contentType = MediaType.MULTIPART_FORM_DATA_VALUE
  )
  fun generatePdf(
    @RequestPart("files") htmlResource: Resource,

    //https://gotenberg.dev/docs/routes#wait-before-rendering-chromium
    @RequestPart("waitDelay") waitDelay: String = "2s"
  ): ByteArray

}
