package tutorials.keycloak.auth

data class PasswordLoginRequest(
  val username: String,
  val password: String
)
