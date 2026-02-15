package tutorials.multitenancy.security.jwt

import com.nimbusds.jose.*
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Component
import tutorials.multitenancy.base.extensions.logger
import java.util.*

@Component
class JwtProvider(
  private val jwtProperties: JwtProperties
) {
  private val logger by logger()

  private val signer: JWSSigner by lazy {
    MACSigner(jwtProperties.secret.toByteArray())
  }

  private val verifier: JWSVerifier by lazy {
    MACVerifier(jwtProperties.secret.toByteArray())
  }

  fun generateToken(username: String, tenant: String): String {
    val now = Date()
    val expiry = Date(now.time + jwtProperties.expirationMs)

    val claimsSet = JWTClaimsSet.Builder()
      .subject(username)
      .claim("tenant", tenant)
      .issueTime(now)
      .expirationTime(expiry)
      .build()

    val signedJWT = SignedJWT(
      JWSHeader(JWSAlgorithm.HS256),
      claimsSet
    )

    signedJWT.sign(signer)
    return signedJWT.serialize()
  }

  fun parseAndValidate(token: String): ValidatedJwtClaims {
    val claims = try {
      val signedJWT = SignedJWT.parse(token)

      if (!signedJWT.verify(verifier)) {
        logger.warn("Invalid JWT signature")
        throw BadCredentialsException("Invalid JWT token")
      }

      signedJWT.jwtClaimsSet
    } catch (e: BadCredentialsException) {
      throw e
    } catch (e: Exception) {
      logger.warn("Invalid JWT token: ${e.message}")
      throw BadCredentialsException("Invalid JWT token")
    }

    val expirationTime = claims.expirationTime
    if (expirationTime != null && expirationTime.before(Date())) {
      logger.warn("JWT token is expired")
      throw BadCredentialsException("Invalid JWT token")
    }

    val username = claims.subject?.takeIf { it.isNotBlank() }
      ?: throw BadCredentialsException("Invalid JWT token")
    val tenant = claims.getStringClaim("tenant")?.takeIf { it.isNotBlank() }
      ?: throw BadCredentialsException("Invalid JWT token")

    return ValidatedJwtClaims(
      username = username,
      tenant = tenant
    )
  }
}

data class ValidatedJwtClaims(
  val username: String,
  val tenant: String
)
