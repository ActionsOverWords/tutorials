import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
  kotlin("jvm") version "1.9.25"
  kotlin("plugin.spring") version "1.9.25"
  kotlin("plugin.jpa") version "1.9.25"
  kotlin("kapt")version "1.9.25"

  id("org.springframework.boot") version "3.5.8"
  id("io.spring.dependency-management") version "1.1.7"
}

group = "tutorials"
version = "0.0.1-SNAPSHOT"
description = "javers"

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

repositories {
  mavenCentral()
}

val queryDslVersion = "5.1.0"
val JaVersVersion = "7.9.0"
dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("org.jetbrains.kotlin:kotlin-reflect")

  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.mariadb.jdbc:mariadb-java-client")
  implementation("com.querydsl:querydsl-jpa:$queryDslVersion:jakarta")
  compileOnly("com.querydsl:querydsl-apt:$queryDslVersion:jakarta")
  implementation("org.javers:javers-spring-boot-starter-sql:$JaVersVersion")

  kapt("com.querydsl:querydsl-apt:$queryDslVersion:jakarta")

  developmentOnly("org.springframework.boot:spring-boot-devtools")
  developmentOnly("org.springframework.boot:spring-boot-docker-compose")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.boot:spring-boot-testcontainers")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testImplementation("org.testcontainers:junit-jupiter")
  testImplementation("org.testcontainers:mariadb")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
  compilerOptions {
    freeCompilerArgs.addAll("-Xjsr305=strict")
  }
}

allOpen {
  annotation("jakarta.persistence.Entity")
  annotation("jakarta.persistence.MappedSuperclass")
  annotation("jakarta.persistence.Embeddable")
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
