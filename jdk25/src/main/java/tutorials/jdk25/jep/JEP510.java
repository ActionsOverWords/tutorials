package tutorials.jdk25.jep;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * JEP 510: Key Derivation Function API (Final)
 * - 보안 강화를 위해 암호 키를 안전하게 생성할 수 있는 표준 API를 제공
 */
public class JEP510 {

  public static void main(String[] args) throws Exception {
    char[] password = "hunter2".toCharArray();
    byte[] salt = "somesalt".getBytes();
    PBEKeySpec spec = new PBEKeySpec(password, salt, 65536, 256);

    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    SecretKey key = factory.generateSecret(spec);

    System.out.println("Derived key format: " + key.getFormat());
  }

}
