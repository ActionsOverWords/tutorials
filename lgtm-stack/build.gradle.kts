import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
  kotlin("jvm") version "2.1.0"
  kotlin("plugin.spring") version "2.1.0"
  kotlin("plugin.jpa") version "2.1.0"

  id("org.springframework.boot") version "3.5.6"
  id("io.spring.dependency-management") version "1.1.7"
}

group = "tutorials"
version = "0.0.1-SNAPSHOT"
description = "lgtm-stack"

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("org.jetbrains.kotlin:kotlin-reflect")

  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.mariadb.jdbc:mariadb-java-client")

  // Metrics: Prometheus format for Mimir
  implementation("io.micrometer:micrometer-registry-prometheus")

  // Traces: OpenTelemetry for Tempo
  implementation("io.micrometer:micrometer-tracing-bridge-otel")
  implementation("io.opentelemetry:opentelemetry-exporter-otlp")

  // Logs: Send logs to Loki directly
  implementation("com.github.loki4j:loki-logback-appender:2.0.0")

  // Profiling: Pyroscope Java Client
  implementation("io.pyroscope:agent:2.1.2")

  developmentOnly("org.springframework.boot:spring-boot-devtools")
  developmentOnly("org.springframework.boot:spring-boot-docker-compose")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

allOpen {
  annotation("jakarta.persistence.Entity")
  annotation("jakarta.persistence.MappedSuperclass")
  annotation("jakarta.persistence.Embeddable")
}

kotlin {
  compilerOptions {
    freeCompilerArgs.addAll("-Xjsr305=strict")
  }
}

project.afterEvaluate {
  println(">> afterEvaluate")
  ext.set("projectRootDir", project.rootDir.path)
  println(">> projectRootDir: ${ext.properties["projectRootDir"]}")
}

tasks {
  withType<Test> {
    useJUnitPlatform()

    testLogging {
      showStandardStreams = true
      exceptionFormat = FULL
    }
  }
  processResources {
    filesMatching("**/application.yml") {
      expand(project.properties)
    }
  }
}
