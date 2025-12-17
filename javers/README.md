# JaVers

JPA 환경에서 JaVers를 활용한 변경 이력 추적

## 프로젝트 목적

- 복잡한 Domain 관계에서 트랜잭션 단위 변경 이력 관리
- JaVers 기능 학습 (Snapshot, Changes, Shadow)
- JaVers의 세 가지 감사 방식 비교
    - [명시적 commit](src/test/kotlin/tutorials/javers/basic/JaversManualCommitTest.kt)
    - [Repository 자동 감사](src/test/kotlin/tutorials/javers/basic/JaversSpringDataAuditableTest.kt)
    - [Service 자동 감사](src/test/kotlin/tutorials/javers/basic/JaversAuditableTest.kt)
- Hibernate Envers와의 차이점 이해 및 적절한 선택 가이드 제공

## JaVers란?

Java 객체의 변경 이력을 추적하고 감사(audit)하는 라이브러리

### 주요 특징

1. **객체 비교 (Diff)**: 두 객체 간의 차이를 자동으로 감지
2. **변경 이력 추적 (Audit)**: 객체의 변경 이력을 시간순으로 저장
3. **스냅샷 (Snapshot)**: 특정 시점의 객체 상태를 저장
4. **트랜잭션 단위 조회**: 여러 Entity의 변경을 하나의 커밋으로 그룹화

### JaVers 아키텍처

JaVers는 다음과 같은 핵심 컴포넌트들로 구성됩니다:

#### 1. Commit

트랜잭션 단위의 변경을 나타내는 최상위 개념입니다.

**구성 요소**:

- **CommitId**: 커밋 고유 식별자 (majorId.minorId 형식, 예: 1.0, 1.1, 2.0)
- **Author**: 변경 작업자 정보
- **CommitDate**: 커밋 시각 (LocalDateTime)
- **Snapshots**: 이 커밋에 포함된 스냅샷 목록
- **Properties**: 커밋 메타데이터 (선택적)

하나의 커밋에는 여러 객체의 변경이 포함될 수 있어, 트랜잭션 단위로 변경 이력을 조회할 수 있습니다.

```kotlin
// 명시적 커밋 예시
javers.commit("admin", company)  // author: "admin"

// 여러 객체를 하나의 커밋으로 저장
javers.commit("admin", users)
```

#### 2. JaversRepository

변경 이력을 저장하고 조회하는 저장소

**종류**:

- **JaversSqlRepository**: SQL 데이터베이스에 저장 (MySQL, PostgreSQL, H2 등)
- **JaversMongoRepository**: MongoDB에 저장
- **InMemoryRepository**: 메모리에 저장 (테스트용)

#### 3. TypeMapper

Java/Kotlin 클래스를 JaVers 타입으로 매핑

**JaVers 타입 분류**:

- **Entity**: @Id를 가진 도메인 객체 (Company, User 등)
- **ValueObject**: @Id가 없는 값 객체 (Address, Money 등)
- **Value**: 단순 값 타입 (String, Integer, Date 등)
- **Embedded**: @Embeddable로 표시된 임베디드 객체 (SecurityOption)
- **Container**: Collection 타입 (List, Set, Map, Array)

JaVers는 자동으로 타입을 추론하지만, 명시적으로 지정할 수도 있음

```kotlin
@Configuration
class JaversConfig {
  @Bean
  fun javersBuilder(): JaversBuilder {
    return JaversBuilder.javers()
      .registerEntity(Company::class.java)
      .registerValueObject(SecurityOption::class.java)
  }
}
```

#### 4. ObjectGraphBuilder

객체와 그 연관 객체들을 그래프 형태로 구성

객체를 저장할 때 연관된 객체들도 함께 추적합니다:

- 1:1 관계 (Company ↔ CompanyOption)
- 1:N 관계 (Company → Users)
- Embedded 객체 (CompanyOption → SecurityOption)

**Scope 설정**:

- **Shallow**: 해당 객체만 추적 (기본값)
- **Deep**: 연관된 모든 객체까지 추적

```kotlin
// Shallow 조회 (Company만)
javers.findSnapshots(
  QueryBuilder.byInstanceId(id, Company::class.java).build()
)

// Deep 조회 (Company + CompanyOption + Users)
javers.findShadows(
  QueryBuilder.byInstanceId(id, Company::class.java)
    .withScopeDeep()
    .build()
)
```

### JaVers 핵심 개념

#### 1. CdoSnapshot (Commit Domain Object Snapshot)

특정 시점의 객체 상태를 나타내는 불변 스냅샷입니다.

**특징**:

- 객체의 전체 상태를 JSON으로 저장
- 버전 관리 (version 1, 2, 3...)
- 타입별 분류: INITIAL(최초 생성), UPDATE(수정), TERMINAL(삭제)
- 커밋 정보 포함 (author, timestamp)

**사용 예시**:

```kotlin
// 특정 Entity의 모든 스냅샷 조회
val snapshots: List<CdoSnapshot> = javers.findSnapshots(
  QueryBuilder.byInstance(user).build()
)

val snapshots: List<CdoSnapshot> = javers.findSnapshots(
  QueryBuilder.byInstanceId(entityId, Company::class.java).build()
)

// 스냅샷 정보 확인
snapshots.forEach { snapshot ->
  println("Version: ${snapshot.version}")
  println("State: ${snapshot.state}")
  println("Commit ID: ${snapshot.commitId}")
  println("Changed Properties: ${snapshot.changed}")
}
```

#### 2. Changes

두 스냅샷(또는 객체) 간의 구체적인 차이

**Change 타입**:

- **NewObject**: 새 객체 생성
- **ValueChange**: 단순 값 변경 (String, Integer 등)
- **ObjectRemoved**: 객체 삭제
- **ReferenceChange**: 참조 변경 (Company 변경)
- **ListChange**: 리스트 변경 (employees 추가/삭제)
- **MapChange**: Map 변경
- **SetChange**: Set 변경
- **ArrayChange**: Array 변경

**사용 예시**:

```kotlin
// 변경 이력 조회
val changes: List<Change> = javers.findChanges(
  QueryBuilder.byInstance(company).build()
)

// 특정 속성의 변경만 조회
val nameChanges = javers.findChanges(
  QueryBuilder.byInstanceId(entityId, Company::class.java)
    .withChangedProperty("name")
    .build()
)

// 변경 타입별 처리
changes.forEach { change ->
  when (change) {
    is ValueChange -> {
      println("${change.propertyName}: ${change.left} -> ${change.right}")
    }
    is ListChange -> {
      println("List changes: ${change.changes}")
    }
    is NewObject -> {
      println("New object created: ${change.affectedGlobalId}")
    }
  }
}
```

#### 3. Shadow

과거 특정 시점의 객체를 재구성한 것으로, 실제 도메인 객체로 복원

**특징**:

- 스냅샷을 기반으로 실제 도메인 객체 재구성
- 과거 데이터를 현재 도메인 모델로 조회 가능
- 연관관계까지 복원 가능

**사용 예시**:

```kotlin
// Shadow 조회 - 과거 객체 상태를 도메인 객체로 복원
val shadows: List<Shadow<Company>> = javers.findShadows(
  QueryBuilder.byInstanceId(entityId, Company::class.java).build()
)

// Shadow에서 실제 도메인 객체 추출
shadows.forEach { shadow ->
  val company: Company = shadow.get()
  println("과거 회사명: ${company.name}")
  println("과거 CEO: ${company.ceoName}")
  println("Commit ID: ${shadow.commitMetadata.id}")
}

// 특정 시점의 객체 상태 복원
val shadowAtCommit = javers.findShadows(
  QueryBuilder.byInstanceId(entityId, Company::class.java)
    .withCommitId(commitId)
    .build()
)
```

### 세 가지 개념 비교

| 개념              | 설명         | 반환 타입                              | 사용 목적                     |
|-----------------|------------|------------------------------------|---------------------------|
| **CdoSnapshot** | 객체 상태의 스냅샷 | CdoSnapshot                        | 특정 시점의 객체 상태 조회 (JSON)    |
| **Changes**     | 스냅샷 간 차이   | Change (ValueChange, ListChange 등) | 무엇이 어떻게 변경되었는지 분석         |
| **Shadow**      | 과거 객체 복원   | Shadow<T>                          | 과거 시점의 도메인 객체로 비즈니스 로직 수행 |

### Hibernate Envers vs JaVers

| 기능                  | Hibernate Envers                      | JaVers                                         |
|---------------------|---------------------------------------|------------------------------------------------|
| **테이블 구조**          | Entity별 audit 테이블 (xxx_AUD) + REVINFO | 통합 테이블 (jv_snapshot, jv_commit, jv_global_id)  |
| **데이터 저장 방식**       | 각 Entity마다 별도 audit 테이블 생성            | 모든 Entity를 JSON으로 통합 저장                        |
| **트랜잭션 정보**         | REVINFO 테이블에 revision 정보 저장           | jv_commit 테이블에 commit 정보 저장                    |
| **트랜잭션 단위 조회**      | 가능하지만 복잡 (REVINFO 조인 필요)              | 용이 (QueryBuilder.anyDomainObject())            |
| **여러 Entity 동시 조회** | 어려움 (각 audit 테이블 개별 조회)               | 용이 (하나의 스냅샷 테이블에서 조회)                          |
| **쿼리 방식**           | HQL/Criteria API 사용                   | JQL (Javers Query Language)                    |
| **객체 비교**           | 수동으로 비교 로직 작성 필요                      | Diff API로 자동 비교                                |
| **설정**              | @Audited 어노테이션                        | @JaversSpringDataAuditable 또는 @JaversAuditable |
| **JPA 통합**          | 긴밀 (Hibernate 전용)                     | 느슨 (JPA 독립적, MongoDB, Redis 지원)                |
| **스키마 변경**          | Entity 변경 시 audit 테이블도 변경 필요          | JSON 저장으로 스키마 변경 영향 적음                         |
| **변경 타입 분류**        | MOD, ADD, DEL                         | INITIAL, UPDATE, TERMINAL + 상세 Change 타입       |

### 특정 필드 무시

민감한 정보나 불필요한 필드는 추적하지 않도록 설정할 수 있음

```kotlin
@Configuration
class JaversConfig {
  @Bean
  fun javersBuilder(): JaversBuilder {
    return JaversBuilder.javers()
      .registerIgnoredProperty(User::class.java, "password")
      .registerIgnoredProperty(Company::class.java, "createdAt")
  }
}

@Entity
class User(
  var name: String,

  @DiffIgnore
  var password: String
)
```

### SpringSecurity 지원

```kotlin
@Component
class SpringSecurityAuthorProvider : AuthorProvider {
  override fun provide(): String {
    val authentication = SecurityContextHolder.getContext().authentication
    return authentication?.name ?: "system"
  }
}
```

## JaVers SQL 구조

JaVers는 다음 테이블들을 사용하여 변경 이력을 관리합니다:

### 1. jv_commit

커밋(트랜잭션) 정보를 저장하는 테이블

| 컬럼                  | 설명              |
|---------------------|-----------------|
| commit_id           | 커밋 고유 ID (PK)   |
| author              | 변경 작업자          |
| commit_date         | 커밋 시간           |
| commit_date_instant | 커밋 시간 (Instant) |

### 2. jv_snapshot

객체의 스냅샷을 저장하는 테이블

| 컬럼                 | 설명                           |
|--------------------|------------------------------|
| snapshot_pk        | 스냅샷 고유 ID (PK)               |
| commit_fk          | 커밋 ID (FK to jv_commit)      |
| type               | 변경 타입 (INSERT/UPDATE/DELETE) |
| global_id_owner    | 객체의 전역 ID                    |
| global_id_fragment | 객체의 속성 경로                    |
| state              | 객체의 JSON 상태                  |
| changed_properties | 변경된 속성 목록                    |
| managed_type       | Entity 클래스명                  |
| version            | 버전 번호                        |

### 3. jv_global_id

객체의 전역 식별자를 저장하는 테이블

| 컬럼           | 설명                    |
|--------------|-----------------------|
| global_id_pk | 전역 ID (PK)            |
| local_id     | 로컬 ID (Entity의 실제 ID) |
| fragment     | 속성 경로                 |
| type_name    | 클래스명                  |
| owner_id_fk  | 소유자 ID (참조 관계)        |

### JaVers 조회 API 사용법

#### 1. Snapshot 조회 (CdoSnapshot)

객체 상태의 스냅샷을 JSON 형태로 조회

```kotlin
// 특정 Entity의 모든 스냅샷 조회
val snapshots: List<CdoSnapshot> = javers.findSnapshots(
  QueryBuilder.byInstanceId(entityId, Company::class.java).build()
)

val snapshots: List<CdoSnapshot> = javers.findSnapshots(
  QueryBuilder.byInstance(company).build()
)

// 특정 Class의 모든 스냅샷 조회
val companySnapshots = javers.findSnapshots(
  QueryBuilder.byClass(Company::class.java).build()
)

// 모든 도메인 객체의 스냅샷 조회 (트랜잭션 단위)
val allSnapshots = javers.findSnapshots(
  QueryBuilder.anyDomainObject().build()
)

// 최근 N개의 스냅샷만 조회
val recentSnapshots = javers.findSnapshots(
  QueryBuilder.byInstanceId(entityId, Company::class.java)
    .limit(10)
    .build()
)
```

#### 2. Changes 조회

스냅샷 간 구체적인 변경 내역 조회

```kotlin
// 특정 Entity의 모든 변경 이력 조회
val changes: List<Change> = javers.findChanges(
  QueryBuilder.byInstanceId(entityId, Company::class.java).build()
)

// 특정 속성의 변경만 조회
val nameChanges = javers.findChanges(
  QueryBuilder.byInstanceId(entityId, Company::class.java)
    .withChangedProperty("name")
    .build()
)

// 특정 Class의 모든 변경 조회
val companyChanges = javers.findChanges(
  QueryBuilder.byClass(Company::class.java).build()
)

// ValueChange만 필터링
changes.filterIsInstance<ValueChange>().forEach { change ->
  println("${change.propertyName}: ${change.left} -> ${change.right}")
}
```

#### 3. Shadow 조회 (과거 객체 복원)

과거 시점의 객체를 도메인 모델로 복원

```kotlin
// 특정 Entity의 모든 Shadow 조회
val shadows: List<Shadow<Company>> = javers.findShadows(
  QueryBuilder.byInstanceId(entityId, Company::class.java).build()
)

// Shadow에서 실제 도메인 객체 추출
shadows.forEach { shadow ->
  val company: Company = shadow.get()
  println("과거 시점: ${shadow.commitMetadata.commitDate}")
  println("회사명: ${company.name}")
  println("직원 수: ${company.employees.size}")
}

// 특정 커밋 시점의 객체 복원
val shadowAtCommit = javers.findShadows(
  QueryBuilder.byInstanceId(entityId, Company::class.java)
    .withCommitId(commitId)
    .build()
).firstOrNull()?.get()

// 연관관계까지 포함하여 복원
val shadowsWithRelations = javers.findShadows(
  QueryBuilder.byInstanceId(entityId, Company::class.java)
    .withScopeDeep()  // 연관된 객체까지 복원
    .build()
)
```

#### 조회 옵션 조합

```kotlin
// 복합 조건 예시: 특정 기간의 변경만 조회
val changesInPeriod = javers.findChanges(
  QueryBuilder.byInstanceId(entityId, Company::class.java)
    .from(startDate)
    .to(endDate)
    .withChangedProperty("ceoName")
    .build()
)

// 특정 Author의 변경만 조회
val changesByAuthor = javers.findChanges(
  QueryBuilder.byClass(Company::class.java)
    .byAuthor("admin")
    .build()
)

// 페이징
val pagedSnapshots = javers.findSnapshots(
  QueryBuilder.byClass(Company::class.java)
    .skip(20)
    .limit(10)
    .build()
)
```

### Changes vs Snapshots 선택

목록 조회에는 Changes, 상세 조회에는 Snapshots를 사용

```kotlin
// 목록 조회: Changes 사용 (가벼움)
val changes = javers.findChanges(...)
val grouped = changes.groupByCommit()  // 트랜잭션 단위 그룹핑

// 상세 조회: Snapshots 사용 (전체 상태 포함)
val snapshots = javers.findSnapshots(...)
```

| 구분             | Changes            | Snapshots      |
|----------------|--------------------|----------------|
| 데이터 크기         | 변경된 속성만 (가벼움)      | 전체 객체 상태 (무거움) |
| commitMetadata | 자동 포함              | 별도 조회 필요       |
| 그룹핑            | groupByCommit() 내장 | 수동 구현 필요       |
| 용도             | 목록 조회              | 상세 조회          |
| 성능             | 빠름                 | 느림             |


## JQL (Javers Query Language) 한계

- 목록 조회의 경우 Commit 단위의 변경 이력을 조회하고 싶으나 지원 안됨
- 페이지별 CommitID 조회 후 javers.findSnapshots(commitId)로 추가 조회
> [JaVersService](src/main/kotlin/tutorials/javers/service/JaVersService.kt).searchAuditHistory 참조

## 참고 자료

- [JaVers 공식 문서](https://javers.org/)
- [JaVers GitHub](https://github.com/javers/javers)
