import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
  kotlin("jvm") version "1.9.25"
  kotlin("plugin.spring") version "1.9.25"
  id("org.springframework.boot") version "3.5.5"
  id("io.spring.dependency-management") version "1.1.7"
}

group = "tutorials"
version = "0.0.1-SNAPSHOT"
description = "influxdb3"

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

repositories {
  mavenCentral()
}

private val influxDb3Version = "1.4.0"
private val apacheArrowVersion = "18.3.0"

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("org.jetbrains.kotlin:kotlin-reflect")

  // influxdb 3
  implementation("com.influxdb:influxdb3-java:$influxDb3Version")

  // Apache Arrow Flight
  implementation("org.springframework.boot:spring-boot-starter-jdbc")
  implementation("org.apache.arrow:flight-sql-jdbc-driver:$apacheArrowVersion")
  implementation("org.apache.arrow:flight-core:$apacheArrowVersion")

  developmentOnly("org.springframework.boot:spring-boot-docker-compose")

  testImplementation("org.springframework.boot:spring-boot-starter-test") {
    exclude(module = "mockito-core")
  }
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testImplementation("org.springframework.boot:spring-boot-testcontainers")
  testImplementation("org.testcontainers:junit-jupiter")

  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

project.afterEvaluate {
  println(">> afterEvaluate")
  ext.set("projectRootDir", project.rootDir.path)
  println(">> projectRootDir: ${ext.properties["projectRootDir"]}")
}

kotlin {
  compilerOptions {
    freeCompilerArgs.addAll("-Xjsr305=strict")
  }
}

tasks{
  test {
    useJUnitPlatform()
    jvmArgs(
      "--add-opens", "java.base/java.nio=ALL-UNNAMED",
      "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED"
    )
  }

  withType<Test> {
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
