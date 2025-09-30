package tutorials.jdk25.jep;

import module java.base;

//import java.nio.file.Paths;
//import java.time.LocalDateTime;
//import java.util.Date;

/**
 * JEP 511: Module Import Declarations (Preview)
 * - 모듈에서 엑스포트된 모든 패키지를 개발자가 손쉽게 임포트할 수 있게 지원
 */
public class JEP511 {

  public static void main(String[] args) {
    var date = new Date();
    var now = LocalDateTime.now();
    var path = Paths.get("/tmp");

    System.out.println(date);
    System.out.println(now);
    System.out.println(path);
  }

}
