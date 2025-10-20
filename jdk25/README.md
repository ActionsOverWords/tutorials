# [JDK 25](https://openjdk.org/projects/jdk/25/)

## 1. JCP, JSR, JEP
### 1.1. JCP (Java Community Process)
- 자바의 표준을 만드는 의회
- 새로운 자바 기술 사양 제안(JSR)을 검토하고 승인하는 절차이자 조직

### 1.1. JSR (Java Specification Request)
- 자바 표준 기술을 위한 공식 제안서
- JCP를 통해 표준으로 만들고 싶은 새로운 자바 기술이나 기존 기술의 대규모 변경사항을 담은 공식 제안 문서

### 1.2. JEP (JDK Enhancement Proposal)
- JDK를 개선하기 위한 제안서
- OpenJDK 및 Oracle JDK에 새로운 기능을 추가하거나 개선하기 위한 제안서

> 과거에는 큰 언어 변화도 JSR을 통해 진행되었지만,<br>자바의 릴리스 주기가 6개월로 짧아진 이후로는 대부분의 JDK 관련 신기능 개발은 JEP를 중심으로 진행

## 2. 주요 기능 및 개선 사항
### 2.1. 플랫폼 및 런타임 관련 변화
- JEP 503: Remove the 32-bit x86 Port
- JEP 519: Compact Object Headers
- JEP 521: Generational Shenandoah

### 2.2. 언어 및 개발 생산성 개선
- JEP 506: Scoped Values
- JEP 505: Structured Concurrency (Fifth Preview)
- JEP 507: Primitive Types in Patterns, instanceof, and switch (Third Preview)
- JEP 511: Module Import Declarations
- JEP 512: Compact Source Files and Instance Main Methods
- JEP 513: Flexible Constructor Bodies

### 2.3. 성능 및 보안 관련 기능
- JEP 508: Vector API (Tenth Incubator)
- JEP 509: JFR CPU-Time Profiling (Experimental)
- JEP 510: Key Derivation Function API
- JEP 514: Ahead-of-Time Command-Line Ergonomics
- JEP 515: Ahead-of-Time Method Profiling
- JEP 518: JFR Cooperative Sampling
- JEP 520: JFR Method Timing & Tracing

### 2.4. 개발자 경험 강화 기능
- JEP 470: PEM Encodings of Cryptographic Objects (Preview)
- JEP 502: Stable Values


## 참고
- [오라클, 자바 25 출시](https://www.oracle.com/kr/news/announcement/oracle-releases-java-25-2025-09-16/)
- [Baeldung: New Features in Java 25](https://www.baeldung.com/java-25-features)
- [Java 25 정식 출시: 개발자들이 꼭 알아야 할 핵심 변화와 기능](https://digitalbourgeois.tistory.com/1971)
