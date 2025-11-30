# Spring RestDocs + OpenAPI (Swagger)
- RestDocs와 Swagger의 장단점을 보완하여 API 문서화 방법

| 구분       | 장점                  | 단점                                     |
|----------|---------------------|----------------------------------------|
| Swagger  | - 문서에서 API 테스트 지원   | - 테스트 무관<br>- 소스 코드에 Swagger 어노테이션이 섞임 |
| RestDocs | - 깔끔한 문서<br>-테스트 강제 | - 문서에서 API 테스트 미지원<br>- IO 파라미터 작성     |

## 1. Spring RestDocs
- Spring Boot 지원
- build.gradle.kts 
  ```gradle
  plugins {
    id("org.asciidoctor.jvm.convert") version "3.3.2"
  }
  
  extra["snippetsDir"] = file("build/generated-snippets")
  
  dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
  }
  
  tasks.test {
    outputs.dir(project.extra["snippetsDir"]!!)
  }
  
  tasks.asciidoctor {
    inputs.dir(project.extra["snippetsDir"]!!)
    dependsOn(tasks.test)
  }
  ```
  
- TestCase
  ```kotlin
  mockMvc.perform(
    post("/users")
      .jsonContent(request)
  )
  // 테스트 검증
  .andExpect(status().isOk)
  .andExpect(jsonPath("$.id").isNotEmpty)
  // RestDocs 문서화
  .and(
    document(
      "users/register",
      requestFields(
        fieldWithPath("username").type(STRING).description("User ID"),
        fieldWithPath("password").type(STRING).description("Password"),
      ),
      responseFields(
        fieldWithPath("id").type(STRING).description("User ID"),
        fieldWithPath("username").type(STRING).description("Username")
      ),
    )
  )
  ```
  
- snippets 파일
```text
# default snippets
<output-directory>/curl-request.adoc
<output-directory>/http-request.adoc
<output-directory>/http-response.adoc
<output-directory>/httpie-request.adoc
<output-directory>/request-body.adoc
<output-directory>/response-body.adoc
```
  - `파라미터 필수 여부, ENUM 지원 여부` 등 아래 참고 링크 참조

- index.adoc
  - 위 생성된 snippets 파일을 사용하여 API 명세화
  - [다중 snippets include](https://docs.spring.io/spring-restdocs/docs/current/reference/htmlsingle/#working-with-asciidoctor-including-snippets-operation)

## 2. OpenAPI (구 Swagger)
- `epages`라는 독일 기업에서 RestDocs로 `OAS(Open API Specification)`를 만들어주는 [오픈소스](https://github.com/ePages-de/restdocs-api-spec)를 제공
- build.gradle.kts
  ```gradle
  plugins {
    id("com.epages.restdocs-api-spec") version "0.19.4"
    id("org.hidetake.swagger.generator") version "2.19.2"
  }
  
  dependencies {
    testImplementation("com.epages:restdocs-api-spec:0.19.4")
    testImplementation("com.epages:restdocs-api-spec-mockmvc:0.19.4")
    swaggerUI("org.webjars:swagger-ui:5.22.0")
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
    asciidoctor {
      dependsOn(test)
      inputs.dir(project.extra["snippetsDir"]!!)
      configurations(asciidoctorExt.name)
      baseDirFollowsSourceFile()
    }
  
    withType<GenerateSwaggerUI> {
      dependsOn("openapi3")
    }
  
    build {
      dependsOn(asciidoctor, generateSwaggerUI)
    }
  }
  ```
- TestCase
```kotlin
mockMvc.perform(
  //import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*
  post("/users")
    .jsonContent(request)
)
.andExpect(status().isOk)
.andExpect(jsonPath("$.id").isNotEmpty)
.andDo(
  //com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document
  document(
    identifier,
    preprocessRequest(prettyPrint()),
    preprocessResponse(prettyPrint()),
    *snippets
  )
)
```

# Custom
## REST Docs에 DTO Bean Validation 담기
- https://0soo.tistory.com/203
## RestDocs Custom - 문서 커스텀
- https://0soo.tistory.com/201
## Custom Error Code Enum 문서화
- https://0soo.tistory.com/210

## 참고
- [Spring REST Docs](https://docs.spring.io/spring-restdocs/docs/current/reference/htmlsingle/#documenting-your-api)
- 우아한 형제들
    - [Spring Rest Docs 적용 1부](https://techblog.woowahan.com/2597/)
    - [Spring Rest Docs 적용 2부](https://techblog.woowahan.com/2678/)
- [DEVOCEAN: OpenAPI Spec으로 API 스펙 작성하기](https://devocean.sk.com/blog/techBoardDetail.do?ID=165186)
