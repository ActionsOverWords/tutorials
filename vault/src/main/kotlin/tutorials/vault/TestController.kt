package tutorials.vault

import org.springframework.beans.factory.annotation.Value
import org.springframework.vault.core.VaultOperations
import org.springframework.vault.support.Plaintext
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TestController(
  @param:Value("\${vault.transit.keyName}") private val transitKeyName: String,

  val vaultOperations: VaultOperations,
) {

  @GetMapping("/encrypt")
  fun encrypt(text: String): String {
    val plaintext: Plaintext = Plaintext.of(text)
    return vaultOperations.opsForTransit().encrypt(transitKeyName, plaintext).ciphertext
  }

  @GetMapping("/decrypt")
  fun decrypt(ciphertext: String): String {
    return vaultOperations.opsForTransit().decrypt(transitKeyName, ciphertext)
  }

  @GetMapping("/rewrap")
  fun rewrap(ciphertext: String): String {
    return vaultOperations.opsForTransit().rewrap(transitKeyName, ciphertext)
  }

  @GetMapping("/rotate")
  fun rotate() {
    vaultOperations.opsForTransit().rotate(transitKeyName)
  }

}
