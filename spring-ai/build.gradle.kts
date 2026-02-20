plugins {
  kotlin("jvm") version "1.9.25"
  kotlin("plugin.spring") version "1.9.25"
  id("org.springframework.boot") version "3.5.10"
  id("io.spring.dependency-management") version "1.1.7"
}

group = "tutorials"
version = "0.0.1-SNAPSHOT"
description = "spring-ai"

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

repositories {
  mavenCentral()
}

extra["springAiVersion"] = "1.1.2"

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("org.jetbrains.kotlin:kotlin-reflect")

  implementation("org.springframework.ai:spring-ai-starter-model-openai")
  implementation("org.springframework.ai:spring-ai-starter-vector-store-pgvector")

  // document reader
  implementation("org.springframework.ai:spring-ai-tika-document-reader")
  implementation("org.springframework.ai:spring-ai-pdf-document-reader")
  implementation("org.springframework.ai:spring-ai-jsoup-document-reader")

  implementation("org.springframework.ai:spring-ai-advisors-vector-store")
  implementation("org.springframework.ai:spring-ai-rag")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
  imports {
    mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
  }
}

kotlin {
  compilerOptions {
    freeCompilerArgs.addAll("-Xjsr305=strict")
  }
}

tasks.withType<Test> {
  useJUnitPlatform()
}
