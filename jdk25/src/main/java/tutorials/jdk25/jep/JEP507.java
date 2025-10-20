package tutorials.jdk25.jep;

/**
 * JEP 507: Primitive Types in Patterns (Third Preview)
 * - 모든 패턴 컨텍스트(instanceof, switch 등)에서 기본 유형 패턴을 허용하고 인스턴스오브 및 스위치 확장을 통해 모든 기본 유형과의 호환성을 제공
 */
public class JEP507 {

  static void test(Object obj) {
    if (obj instanceof int i) {
      System.out.println("It's an int: " + i);
    }
  }

}
