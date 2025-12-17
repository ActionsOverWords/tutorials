package tutorials.javers.domain

import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@Embeddable
data class SecurityOption(
  @Enumerated(EnumType.STRING)
  var passwordPolicy: PasswordPolicy = PasswordPolicy.LOW,

  var passwordExpirationDays: Int = 90,

  var passwordHistorySize: Int = 5
)
