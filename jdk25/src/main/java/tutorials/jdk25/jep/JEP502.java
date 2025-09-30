package tutorials.jdk25.jep;

/**
 * JEP 502: Stable Value API (Preview)
 * - 값이 변하지 않음을 보장하여 코드 안정성과 예측 가능성을 높임
 */
public class JEP502 {

  public static void main(String[] args) {
    // Create a new unset StableValue
    var greeting = StableValue.<String>of();
    System.out.println(greeting);

    String message = greeting.orElseSet(() -> "Hello from StableValue!");
    System.out.println(message);
  }

}
