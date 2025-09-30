package tutorials.jdk25.jep;

import java.util.concurrent.StructuredTaskScope;

/**
 * JEP 505: Structured Concurrency (Fifth Preview)
 * - 동시성 프로그래밍을 단순화해 멀티 스레드 코드의 유지 보수 용이성, 안정성, 관찰 가능성을 개선
 * - 다양한 스레드에서 실행 중인 관련 작업 그룹을 단일 작업 단위로 취급함으로써 스레드 누수 및 취소 지연 등 취소 및 종료로 인해 흔히 발생하는 위험을 최소화
 */
public class JEP505 {

  static String fetchUser() {
    sleep(100);
    return "Alice";
  }

  static String fetchOrder() {
    sleep(150);
    return "Order#42";
  }

  private static void sleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public static void main(String[] args) throws Exception {
    try (var scope = StructuredTaskScope.<String>open()) {
      var userTask = scope.fork(() -> fetchUser());
      var orderTask = scope.fork(() -> fetchOrder());

      scope.join();

      System.out.println(userTask.get() + " - " + orderTask.get());
    }
  }

}
