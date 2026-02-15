package tutorials.multitenancy.dto

data class LoginRequest(
  val username: String,
  val password: String,
  val tenant: String
)

data class LoginResponse(
  val token: String,
  val type: String = "Bearer",
  val username: String,
  val tenant: String
)
