package tutorials.keycloak.auth

import com.fasterxml.jackson.annotation.JsonProperty

data class KeycloakTokenResponse(
  @param:JsonProperty("access_token")
  val accessToken: String? = null,
  @param:JsonProperty("refresh_token")
  val refreshToken: String? = null,
  @param:JsonProperty("id_token")
  val idToken: String? = null,
  @param:JsonProperty("token_type")
  val tokenType: String? = null,
  @param:JsonProperty("expires_in")
  val expiresIn: Int? = null,
  @param:JsonProperty("refresh_expires_in")
  val refreshExpiresIn: Int? = null,
  val scope: String? = null,
  val error: String? = null,
  @param:JsonProperty("error_description")
  val errorDescription: String? = null
) {
  fun toView(): KeycloakTokenView {
    return KeycloakTokenView(
      accessToken = accessToken,
      refreshToken = refreshToken,
      idToken = idToken,
      tokenType = tokenType,
      expiresIn = expiresIn,
      refreshExpiresIn = refreshExpiresIn,
      scope = scope
    )
  }
}
