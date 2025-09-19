package tutorials.influxdb3.base.advice

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import tutorials.influxdb3.base.extentions.logger
import java.time.LocalDateTime

@ControllerAdvice
class ControllerAdvice : ResponseEntityExceptionHandler() {

  val log = logger()

  @ExceptionHandler(Exception::class)
  fun handleException(exception: Exception): ProblemDetail {
    log.error("Exception: ", exception)
    return getProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, exception)
  }

  companion object {
    private fun getProblemDetail(status: HttpStatus, exception: Exception): ProblemDetail {
      val problemDetail = ProblemDetail.forStatusAndDetail(status, exception.message)

      problemDetail.setProperty("timestamp", LocalDateTime.now())
      problemDetail.setProperty("exception", exception.javaClass.getSimpleName())

      return problemDetail
    }
  }
}
