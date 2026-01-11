# ProxySQL Tutorial

MariaDB Master-Slave 환경에서 ProxySQL을 활용한 Read/Write 쿼리 분기 샘플 프로젝트

## ProxySQL이란?

MySQL/MariaDB를 위한 고성능 프록시 서버로, 애플리케이션과 데이터베이스 사이에서 **쿼리를 분석하고 자동으로 라우팅**합니다.

- **SELECT 쿼리** → Slave 서버 (Read)
- **INSERT/UPDATE/DELETE** → Master 서버 (Write)
- **SELECT FOR UPDATE** → Master 서버 (Lock 필요)

## 왜 ProxySQL을 사용하는가?

### 1. 데이터베이스 부하 분산
- 읽기 작업(70-90%)을 Slave로 분산
- Master는 쓰기 작업에만 집중
- 전체 시스템 성능 향상

### 2. 애플리케이션 코드 단순화
```kotlin
// ProxySQL 없이: 복잡한 다중 데이터소스 관리 필요
@Primary @Bean fun masterDataSource() { ... }
@Bean fun slaveDataSource() { ... }

// ProxySQL 사용: 단일 데이터소스만 설정
@Bean fun dataSource() {
  return DataSource("jdbc:mariadb://localhost:6033/mydatabase")
}
```

### 3. 유연한 확장
- Slave 서버 추가 시 애플리케이션 코드 변경 불필요
- ProxySQL 설정만 수정하면 즉시 반영

## 아키텍처

```
Spring Boot App
     ↓ (jdbc:mariadb://localhost:6033)
ProxySQL (Query Router)
     ├─→ Master (hostgroup 0) - Write
     └─→ Slave (hostgroup 1)  - Read
              ↑ Replication
```

## 주요 설정

### application.yml
```yaml
spring:
  datasource:
    url: jdbc:mariadb://localhost:6033/mydatabase  # ProxySQL 포트
    username: myuser
    password: secret
```

### proxysql.cnf
```cnf
# 서버 정의
mysql_servers = (
  { address="mariadb-master", hostgroup=0 },  # Write
  { address="mariadb-slave",  hostgroup=1 }   # Read
)

# 쿼리 라우팅 규칙
mysql_query_rules = (
  { match_digest="^SELECT.*FOR UPDATE$", destination_hostgroup=0 },
  { match_digest="^SELECT",              destination_hostgroup=1 }
)
```

## ProxySQL 쿼리 라우팅 규칙

### 기본 라우팅 방식

#### 1. match_digest - 쿼리 패턴 매칭
쿼리를 정규화(파라미터를 `?`로 치환)하여 패턴 매칭합니다.

```sql
-- 실제 쿼리들
SELECT * FROM users WHERE id = 1
SELECT * FROM users WHERE id = 999

-- 모두 동일한 digest로 인식
-- "SELECT * FROM users WHERE id = ?"

-- 규칙 예시
INSERT INTO mysql_query_rules (rule_id, match_digest, destination_hostgroup, apply)
VALUES (1, '^SELECT', 1, 1);  -- 모든 SELECT → hostgroup 1
```

**match_digest vs match_pattern:**
- `match_digest`: 정규화된 쿼리 패턴 매칭 (권장)
- `match_pattern`: 실제 쿼리 텍스트 매칭 (특수 케이스용)

#### 2. destination_hostgroup - 라우팅 대상
쿼리를 전송할 hostgroup을 지정합니다.

```sql
destination_hostgroup=0  -- Master (Write)
destination_hostgroup=1  -- Slave (Read)
```

#### 3. apply - 규칙 체인 종료
- `apply=1`: 이 규칙이 마지막, 더 이상 규칙 평가 안 함
- `apply=0`: 다음 규칙 계속 평가 (규칙 체이닝)

### 고급 라우팅 방식

#### 1. flagIN / flagOUT - 규칙 체이닝
복잡한 라우팅 로직을 위한 규칙 체인을 구성합니다.

```sql
-- 예시: username과 쿼리 패턴을 조합한 라우팅
INSERT INTO mysql_query_rules (rule_id, username, flagIN, flagOUT, apply)
VALUES (1, 'admin', 0, 100, 0);  -- admin 사용자 → flag 100으로

INSERT INTO mysql_query_rules (rule_id, match_digest, flagIN, destination_hostgroup, apply)
VALUES (2, '^SELECT', 100, 2, 1);  -- flag 100인 SELECT → hostgroup 2
```

**동작 방식:**
- 초기 flagIN=0에서 시작
- 규칙 매칭 시 flagOUT으로 플래그 설정
- flagOUT이 설정되면 해당 flag를 가진 규칙들로 재평가

#### 2. username / schemaname / client_addr - 조건부 라우팅
사용자, 스키마, 클라이언트 IP 기반 라우팅입니다.

```sql
-- 특정 사용자만 master로
INSERT INTO mysql_query_rules (rule_id, username, destination_hostgroup, apply)
VALUES (1, 'poweruser', 0, 1);

-- 특정 스키마는 별도 hostgroup으로
INSERT INTO mysql_query_rules (rule_id, schemaname, destination_hostgroup, apply)
VALUES (2, 'analytics', 3, 1);

-- 특정 IP는 전용 서버로
INSERT INTO mysql_query_rules (rule_id, client_addr, destination_hostgroup, apply)
VALUES (3, '192.168.1.100', 4, 1);
```

#### 3. cache_ttl - 쿼리 결과 캐싱
자주 실행되는 쿼리 결과를 캐시합니다 (밀리초 단위).

```sql
-- 특정 쿼리 결과를 5초간 캐싱
INSERT INTO mysql_query_rules (rule_id, match_digest, cache_ttl, apply)
VALUES (1, '^SELECT.*FROM products WHERE category', 5000, 1);
```

**주의:** 실시간 데이터가 중요한 경우 사용 금지

#### 4. replace_pattern - 쿼리 재작성
쿼리를 변경하여 실행합니다.

```sql
-- 테이블명 자동 변경
INSERT INTO mysql_query_rules (rule_id, match_pattern, replace_pattern, destination_hostgroup, apply)
VALUES (1, 'sbtest[0-9]+', 'sbtest_new', 1, 1);

-- 실행: SELECT * FROM sbtest1 → SELECT * FROM sbtest_new
```

**활용 사례:**
- 테이블 마이그레이션 중 쿼리 리다이렉션
- 특정 패턴의 테이블명 일괄 변경

#### 5. mirror_hostgroup - 쿼리 미러링
쿼리를 복제하여 다른 hostgroup으로도 전송합니다 (결과는 무시).

```sql
-- 운영 서버(0)로 보내면서 테스트 서버(10)에도 미러링
INSERT INTO mysql_query_rules (rule_id, match_digest, destination_hostgroup, mirror_hostgroup, apply)
VALUES (1, '^SELECT', 0, 10, 1);
```

**활용 사례:**
- 새 서버의 성능 테스트
- 쿼리 부하 테스트
- 마이그레이션 전 검증

#### 6. mysql_query_rules_fast_routing - 고속 라우팅
username, schemaname, flagIN 기반의 해시 테이블 라우팅으로 성능을 최적화합니다.

```sql
-- 일반 라우팅 규칙보다 빠른 라우팅
INSERT INTO mysql_query_rules_fast_routing (username, schemaname, flagIN, destination_hostgroup)
VALUES ('appuser', 'mydb', 0, 1);
```

### 규칙 평가 순서

ProxySQL은 다음 순서로 규칙을 평가합니다:

1. **rule_id 오름차순**으로 규칙 평가
2. `apply=1`을 만나거나 매칭되는 규칙이 없을 때까지 계속
3. flagOUT이 설정되면 해당 flagIN 규칙들로 재평가

```sql
-- 예시: 정확한 평가 순서
rule_id=1: SELECT FOR UPDATE → hostgroup 0, apply=1 (종료)
rule_id=2: SELECT           → hostgroup 1, apply=1 (종료)
rule_id=3: INSERT/UPDATE    → hostgroup 0, apply=1 (종료)
```

**중요:** rule_id가 작을수록 먼저 평가되므로, **더 구체적인 규칙을 먼저** 배치해야 합니다.

## ProxySQL 관리

### Admin Console 접속
```bash
docker exec -it proxysql mysql -h127.0.0.1 -P6032 -uadmin -padmin
```

### 유용한 명령어
```sql
-- 서버 상태 확인
SELECT hostgroup_id, hostname, port, status FROM mysql_servers;

-- 쿼리 규칙 확인
SELECT rule_id, match_digest, destination_hostgroup FROM mysql_query_rules;

-- 쿼리 통계 확인 (hostgroup별 분기 확인)
SELECT hostgroup, digest_text, count_star
FROM stats_mysql_query_digest
WHERE digest_text LIKE '%notice%'
ORDER BY hostgroup, count_star DESC;

-- 통계 초기화
SELECT * FROM stats_mysql_query_digest_reset LIMIT 1;
```

## 주의사항

### 1. @Transactional 사용
- `@Transactional(readOnly = true)`: SELECT → Slave
- `@Transactional`: INSERT/UPDATE/DELETE → Master
- 같은 트랜잭션 내 모든 쿼리는 같은 서버로 라우팅됨

### 2. Replication Lag (복제 지연)
Master에 쓴 데이터가 Slave에 즉시 반영되지 않을 수 있음

```kotlin
// ❌ 문제 발생 가능
val saved = noticeService.save(notice)     // Master에 INSERT
val found = noticeService.findById(saved.id)  // Slave에서 SELECT → 없을 수 있음!

// ✅ 해결: 같은 트랜잭션 내에서 처리
@Transactional
fun createAndRead() {
  val saved = noticeRepository.save(notice)
  val found = noticeRepository.findById(saved.id)  // Master에서 조회
}
```
