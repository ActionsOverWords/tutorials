import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.hidetake.gradle.swagger.generator.GenerateSwaggerUI

plugins {
  kotlin("jvm") version "1.9.25"
  kotlin("plugin.spring") version "1.9.25"
  id("org.springframework.boot") version "3.5.7"
  id("io.spring.dependency-management") version "1.1.7"
  id("org.asciidoctor.jvm.convert") version "3.3.2"
  id("com.epages.restdocs-api-spec") version "0.19.4"
  id("org.hidetake.swagger.generator") version "2.19.2"
}

group = "tutorials"
version = "0.0.1-SNAPSHOT"
description = "spring-restdoc-swagger"

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

repositories {
  mavenCentral()
}

extra["snippetsDir"] = file("build/generated-snippets")
val asciidoctorExt: Configuration by configurations.creating

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("org.jetbrains.kotlin:kotlin-reflect")

  developmentOnly("org.springframework.boot:spring-boot-devtools")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")

  testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
  testImplementation("com.epages:restdocs-api-spec:0.19.4")
  testImplementation("com.epages:restdocs-api-spec-mockmvc:0.19.4")
  asciidoctorExt("org.springframework.restdocs:spring-restdocs-asciidoctor")
  swaggerUI("org.webjars:swagger-ui:5.22.0")
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

openapi3 {
  setServer("http://localhost:8080")
  title = "Tutorials API"
  description = "SpringRestDocs + Swagger tutorial"
  version = project.version.toString().substringBeforeLast("-")
  format = "yml"
}

swaggerSources {
  create("swaggerSource") {
    setInputFile(file("${openapi3.outputDirectory}/openapi3.yml"))
  }
}

tasks {
  test {
    outputs.dir(project.extra["snippetsDir"]!!)
    jvmArgs(
      "--add-opens", "java.base/java.nio=ALL-UNNAMED",
      "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED"
    )
  }

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

  asciidoctor {
    dependsOn(test)
    inputs.dir(project.extra["snippetsDir"]!!)

    configurations(asciidoctorExt.name)
    baseDirFollowsSourceFile()
    sources {
      include("**/index.adoc", "enums/*.adoc")
    }
  }

  withType<GenerateSwaggerUI> {
    dependsOn("openapi3")
  }

  build {
    dependsOn(asciidoctor, generateSwaggerUI)
  }

  jar {
    enabled = false
  }
}
