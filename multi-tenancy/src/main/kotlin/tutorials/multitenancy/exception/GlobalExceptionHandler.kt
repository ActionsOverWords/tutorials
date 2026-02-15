package tutorials.multitenancy.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import tutorials.multitenancy.base.extensions.logger

@RestControllerAdvice
class GlobalExceptionHandler {
  private val logger by logger()

  @ExceptionHandler(BadCredentialsException::class)
  fun handleBadCredentialsException(e: BadCredentialsException): ResponseEntity<ErrorResponse> {
    logger.warn("Bad credentials: ${e.message}")

    return HttpStatus.UNAUTHORIZED
      .toErrorResponse(e.message ?: "Invalid credentials")
  }

  @ExceptionHandler(Exception::class)
  fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
    logger.error("Unhandled exception", e)

    return HttpStatus.INTERNAL_SERVER_ERROR
      .toErrorResponse("An unexpected error occurred")
  }
}

data class ErrorResponse(
  val status: Int,
  val error: String,
  val message: String
)

private fun HttpStatus.toErrorResponse(message: String): ResponseEntity<ErrorResponse> {
  return ResponseEntity
    .status(this)
    .body(
      ErrorResponse(
        status = this.value(),
        error = this.reasonPhrase,
        message = message
      )
    )
}
