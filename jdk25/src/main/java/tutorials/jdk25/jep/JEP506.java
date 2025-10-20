package tutorials.jdk25.jep;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * JEP 506: Scoped Values (Final)
 * - 스레드 내 및 스레드 간에 변경 불가능한 데이터를 공유할 수 있도록 하여 개발자가 프로젝트의 사용 편의성, 이해 가능성, 성능, 견고성을 향상시킬 수 있도록 지원
 */
public class JEP506 {

  static final ScopedValue<String> USER = ScopedValue.newInstance();

  public static void main(String[] args) {
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      executor.submit(() -> ScopedValue.where(USER, "Alice").run(() -> {
        System.out.println("Thread: " + Thread.currentThread());
        System.out.println("User: " + USER.get());
      }));

      executor.submit(() -> ScopedValue.where(USER, "Bob").run(() -> {
        System.out.println("Thread: " + Thread.currentThread());
        System.out.println("User: " + USER.get());
      }));

      // Optional delay to ensure output appears before main exits
      Thread.sleep(200);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

}
