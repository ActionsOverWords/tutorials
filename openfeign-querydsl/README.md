# OpenFeign-QueryDSL

## 1. OpenFeign-querydsl 등장 배경
- QueryDSL 개발 현황
  - QueryDSL 프로젝트는 공식적으로 개발 중단을 선언하지는 않았으나, 2024년 1월 29일에 발표된 5.1.0 버전을 마지막으로 새로운 릴리스가 이루어지지 않고 있으며, 기능 추가나 개선 작업 또한 사실상 중단된 상태
- Kotlin kapt의 Maintenance 모드 전환
  - QueryDSL이 의존하는 kapt(Kotlin Annotation Processing Tool)는 Kotlin 공식 문서에서 maintenance 모드로 전환되었음을 공지
  - [kapt compiler plugin](https://kotlinlang.org/docs/kapt.html?source=post_page-----dee89cb3ec05---------------------------------------)
  - [openfeign.querydsl `6.9`에서 `KSP` 지원](https://github.com/OpenFeign/querydsl/releases/tag/6.9)
- Querydsl 유지 관리가 느려지면서 OpenFeign 에서 fork 하여 관리 중
  - https://github.com/OpenFeign/querydsl
- CVE-2024-49203
  - [Querydsl vulnerable to HQL injection trough orderBy](https://github.com/advisories/GHSA-6q3q-6v5j-h6vg)
  - openfeign.querydsl `6.10.1`에서 해결
- [Spring Data JPA - Querydsl Extension](https://docs.spring.io/spring-data/jpa/reference/repositories/core-extensions.html)
  - spring data jpa 에서 openfeign fork 를 지원하기로 결정

## 2. kapt vs ksp
### kapt
- Kotlin 코드에서 Java의 어노테이션 프로세서를 사용할 수 있도록 해주는 도구
- Kotlin 코드를 Java로 변환하고, 그 결과를 바탕으로 어노테이션 프로세서를 실행
- 추가적인 변환 단계가 있으므로 컴파일 시간이 늘어날 수 있음
### ksp
- Kotlin용 으로 설계된 경량의 어노테이션 프로세싱 도구
- Kotlin 코드를 Java로 변환하지 않고 직접 처리하므로, Kapt에 비해 컴파일 속도가 빠름

## 3. OpenFeign QueryDSL + KSP
###  build.gradle.kts
```groovy
plugins {
  kotlin("jvm") version "1.9.25"
  kotlin("plugin.spring") version "1.9.25"
  kotlin("plugin.jpa") version "1.9.25"
  id("org.springframework.boot") version "3.5.0"
  id("io.spring.dependency-management") version "1.1.7"
  id("com.google.devtools.ksp") version "1.9.25-1.0.20" // kotlin version 에 맞게 지정
}

val querydslVersion = 6.11
dependencies {
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("io.github.openfeign.querydsl:querydsl-jpa:$querydslVersion") // querydsl 의존성
  ksp("io.github.openfeign.querydsl:querydsl-ksp-codegen:$querydslVersion") // ksp 설정
}

allOpen {
  annotation("jakarta.persistence.Entity")
  annotation("jakarta.persistence.MappedSuperclass")
  annotation("jakarta.persistence.Embeddable")
}
```

### build
```shell
# run build
./gradlew clean build

# run ksp
./gradlew kspKotlin
```
- `build/generated/ksp/main/kotlin`에 위치

## 참고
- [OpenFeign QueryDSL 6.11 + KSP](https://velog.io/@csh0034/OpenFeign-QueryDSL-KSP)
- [QueryDSL 개발 중단 이후, OpenFeign QueryDSL로의 전환 배경](https://medium.com/@rlaeorua369/openfeign-querydsl-%EB%A7%88%EC%9D%B4%EA%B7%B8%EB%A0%88%EC%9D%B4%EC%85%98-%EC%B4%9D%EC%A0%95%EB%A6%AC-dee89cb3ec05)
- [OpenFeign QueryDSL 6.11 적용하기 (feat. KSP)](https://medium.com/@rlaeorua369/querydsl-5-1-0-openfeign-querydsl-6-11-%EB%A7%88%EC%9D%B4%EA%B7%B8%EB%A0%88%EC%9D%B4%EC%85%98-%EC%8B%A4%EC%A0%84-%EA%B0%80%EC%9D%B4%EB%93%9C-c2315d38308f)
