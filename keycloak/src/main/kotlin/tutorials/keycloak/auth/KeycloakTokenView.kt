package tutorials.keycloak.auth

data class KeycloakTokenView(
  val accessToken: String?,
  val refreshToken: String?,
  val idToken: String?,
  val tokenType: String?,
  val expiresIn: Int?,
  val refreshExpiresIn: Int?,
  val scope: String?
)
