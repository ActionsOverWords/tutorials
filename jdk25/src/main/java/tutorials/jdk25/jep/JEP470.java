package tutorials.jdk25.jep;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * JEP 470: PEM Encodings of Cryptographic Objects (Preview)
 * - PEM 형식의 암호 객체를 쉽게 다룰 수 있어 보안 관련 개발의 편의성 증대
 */
public class JEP470 {

  public static void main(String[] args) {
    String pem = """
        -----BEGIN PUBLIC KEY-----
        MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDgjDohS0RHP395oJxciVaeks9N
        KNY5m9V1IkBBwYsMGyxskrW5sapgi9qlGSYOma9kkko1xlBs17qG8TTg38faxgGJ
        sLT2BAmdVFwuWdRtzq6ONn2YPHYj5s5pqx6vU5baz58/STQXNIhn21QoPjXgQCnj
        Pp0OxnacWeRSnAIOmQIDAQAB
        -----END PUBLIC KEY-----
        """;

    try {
      String base64 = pem.replaceAll("-----.*-----", "").replaceAll("\\s", "");
      byte[] keyBytes = Base64.getDecoder().decode(base64);

      X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
      KeyFactory factory = KeyFactory.getInstance("RSA");
      PublicKey key = factory.generatePublic(spec);

      System.out.println("Loaded key: " + key.getAlgorithm());
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      e.printStackTrace();
    }
  }

}
