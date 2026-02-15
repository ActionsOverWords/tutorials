import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
  kotlin("jvm") version "1.9.25"
  kotlin("plugin.spring") version "1.9.25"
  id("org.springframework.boot") version "3.5.9"
  id("io.spring.dependency-management") version "1.1.7"
  kotlin("plugin.jpa") version "1.9.25"
}

group = "tutorials"
version = "0.0.1-SNAPSHOT"
description = "multi-tenancy"

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("com.nimbusds:nimbus-jose-jwt:9.37.3")
  implementation("org.flywaydb:flyway-core")
  implementation("org.flywaydb:flyway-mysql")
  developmentOnly("org.springframework.boot:spring-boot-docker-compose")
  runtimeOnly("org.mariadb.jdbc:mariadb-java-client")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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

allOpen {
  annotation("jakarta.persistence.Entity")
  annotation("jakarta.persistence.MappedSuperclass")
  annotation("jakarta.persistence.Embeddable")
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
