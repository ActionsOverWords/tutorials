import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
  kotlin("jvm") version "1.9.25"
  kotlin("plugin.spring") version "1.9.25"
  id("org.springframework.boot") version "3.5.7"
  id("io.spring.dependency-management") version "1.1.7"
}

group = "tutorials"
version = "0.0.1-SNAPSHOT"
description = "thread"

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

repositories {
  mavenCentral()
}

extra["springCloudVersion"] = "2025.0.0"

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")

  // Resilience4j 추가 모듈
  implementation("io.github.resilience4j:resilience4j-all:2.1.0")
  implementation("io.github.resilience4j:resilience4j-kotlin:2.1.0")

  // Reactor (Spring WebFlux)
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  testImplementation("io.projectreactor:reactor-test")

  // Kotlin Coroutines
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.8.0")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
  imports {
    mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
  }
}

kotlin {
  compilerOptions {
    freeCompilerArgs.addAll("-Xjsr305=strict")
  }
}

/*
tasks.withType<Test> {
  useJUnitPlatform()

  jvmArgs(
    "-XX:StartFlightRecording=filename=pinning.jfr,settings=profile",
    "-XX:FlightRecorderOptions=stackdepth=256",
    "--enable-preview" // ScopedValue 사용을 위한 Preview 기능 활성화
  )
}

tasks.withType<JavaCompile> {
  options.compilerArgs.add("--enable-preview")
}

tasks.withType<JavaExec> {
  jvmArgs("--enable-preview")
}
*/

tasks {
  withType<Test> {
    useJUnitPlatform()

    jvmArgs(
      "-XX:StartFlightRecording=filename=pinning.jfr,settings=profile",
      "-XX:FlightRecorderOptions=stackdepth=256",
      "--enable-preview" // ScopedValue 사용을 위한 Preview 기능 활성화
    )

    testLogging {
      showStandardStreams = true
      exceptionFormat = FULL
    }
  }

  withType<JavaCompile> {
    options.compilerArgs.add("--enable-preview")
  }

  withType<JavaExec> {
    jvmArgs("--enable-preview")
  }

}
