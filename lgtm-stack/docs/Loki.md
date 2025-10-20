# Grafana Loki

Grafana Loki는 로그 집계 시스템으로, Prometheus에서 영감을 받아 설계되었습니다. 로그 내용을 인덱싱하는 대신 레이블만 인덱싱하여 효율적인 로그 저장 및 검색을 제공합니다.

## 역할

- **로그 수집**: Spring Boot 애플리케이션 및 Docker 컨테이너의 로그 수집
- **레이블 기반 인덱싱**: 로그를 레이블로 분류하여 효율적인 검색 제공
- **Trace 연동**: 로그의 TraceID를 추출하여 Tempo와 연동

## 아키텍처

```
┌──────────────────────┐
│  Spring Boot App     │
│                      │
│  Loki4j Appender     │
└──────────┬───────────┘
           │ HTTP Push
           │ Protobuf
           ▼
┌──────────────────────┐     ┌──────────────────────┐
│   Grafana Alloy      │     │   Grafana Loki       │
│                      │     │                      │
│  Loki Docker Source  ├────►│  - Distributor       │
│  (Container Logs)    │     │  - Ingester          │
└──────────────────────┘     │  - Querier           │
                             │  - Compactor         │
                             └──────────┬───────────┘
                                        │
                                        ▼
                              ┌──────────────────────┐
                              │   Grafana            │
                              │   (LogQL Query)      │
                              └──────────────────────┘
```

## 로그 수집 방식

### 1. Spring Boot 애플리케이션 로그 (직접 전송)

**Logback 설정** (`logback-spring.xml`):
```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <labels>
        service_name = ${appName}
        host = ${HOSTNAME}
        level = %level
    </labels>
    <message>
        <pattern>
            %d{yyyy-MM-dd'T'HH:mm:ss.SS} %5p --- [%15.15t] %-40.40logger{39} :
            [%X{traceId:-},%X{spanId:-}] %m%n
        </pattern>
    </message>
    <http>
        <url>http://localhost:3100/loki/api/v1/push</url>
    </http>
</appender>
```

**핵심 요소:**
- **Labels**: `service_name`, `host`, `level` - 로그를 분류하는 레이블
- **Pattern**: `[traceId,spanId]` - Trace Context 포함 (Tempo 연동용)
- **URL**: Loki로 직접 HTTP Push

**장점:**
- 애플리케이션에서 Loki로 직접 전송 (중간 경유 없음)
- 구조화된 레이블로 효율적인 검색
- TraceID가 로그에 자동 포함

### 2. Docker 컨테이너 로그 (Alloy 경유)

**Alloy 설정** (`alloy.river`):
```river
loki.source.docker "docker_logs" {
  host = "unix:///var/run/docker.sock"
  targets = discovery.docker.docker_containers.targets
  forward_to = [loki.relabel.docker_logs.receiver]
}

loki.relabel "docker_logs" {
  forward_to = [loki.write.default.receiver]

  rule {
    source_labels = ["__meta_docker_container_name"]
    regex = "/(.*)"
    target_label = "service_name"
  }
}
```

**수집 대상:**
- Mimir, Tempo, Grafana, Alloy 등 Docker 컨테이너의 로그
- 자동으로 `service_name` 레이블 추가

## Loki 설정

### 기본 구성 (Single Instance)

Loki는 별도 설정 파일 없이 기본 설정으로 실행됩니다:

```yaml
# compose.yml
loki:
  image: grafana/loki:3.4.1
  command: -config.file=/etc/loki/local-config.yaml  # 내장 설정
  ports:
    - "3100:3100"
```

### 기본 설정 내용 (내장)

```yaml
auth_enabled: false  # 인증 비활성화

server:
  http_listen_port: 3100

ingester:
  lifecycler:
    ring:
      kvstore:
        store: inmemory
      replication_factor: 1
  chunk_idle_period: 5m
  chunk_retain_period: 30s

schema_config:
  configs:
    - from: 2020-10-24
      store: boltdb-shipper
      object_store: filesystem
      schema: v11
      index:
        prefix: index_
        period: 24h

storage_config:
  boltdb_shipper:
    active_index_directory: /loki/boltdb-shipper-active
    cache_location: /loki/boltdb-shipper-cache
  filesystem:
    directory: /loki/chunks

limits_config:
  reject_old_samples: true
  reject_old_samples_max_age: 168h
```

## Grafana 연동

### Datasource 설정

**Grafana Datasource** (`datasource.yml`):
```yaml
- uid: loki
  name: Loki
  type: loki
  url: http://loki:3100
  jsonData:
    maxLines: 1000
    # 로그에서 traceId를 추출하여 Tempo로 연동
    derivedFields:
      - datasourceUid: tempo
        matcherRegex: '\[([0-9a-f]{32}),([0-9a-f]{16})\]'
        name: traceId
        url: '$${__value.raw}'
        urlDisplayLabel: 'View Trace'
```

**Derived Fields 설명:**
- **matcherRegex**: 로그에서 `[traceId,spanId]` 패턴 검색
- **Capture Group 1**: 32자리 16진수 traceId 추출
- **Capture Group 2**: 16자리 16진수 spanId 추출
- **datasourceUid**: 추출한 traceId로 Tempo 조회
- **urlDisplayLabel**: Grafana UI에 "View Trace" 버튼 표시

### Logs → Traces 연동 예시

**로그 내용:**
```
2025-01-15T10:30:45.123 INFO --- [nio-8080-exec-1] c.e.UserController :
[a1b2c3d4e5f6789012345678901234ab,1234567890abcdef] User created: john@example.com
```

**Grafana 동작:**
1. Loki에서 로그 조회
2. Regex가 `[a1b2c3d4e5f6789012345678901234ab,1234567890abcdef]` 매칭
3. TraceID `a1b2c3d4e5f6789012345678901234ab` 추출
4. "View Trace" 버튼 표시
5. 클릭 시 Tempo에서 해당 Trace 조회

## LogQL 쿼리

### 기본 쿼리

```logql
# 특정 서비스의 모든 로그
{service_name="lgtm-stack"}

# 에러 레벨 로그만
{service_name="lgtm-stack", level="ERROR"}

# 여러 서비스의 로그
{service_name=~"lgtm-stack|lgtm-stack-loki-1"}
```

### 필터링

```logql
# 특정 텍스트 포함
{service_name="lgtm-stack"} |= "User created"

# 특정 텍스트 제외
{service_name="lgtm-stack"} != "health check"

# 정규식 매칭
{service_name="lgtm-stack"} |~ "[0-9]{3}"

# JSON 로그 파싱
{service_name="lgtm-stack"} | json | status_code >= 400
```

### 집계 및 분석

```logql
# 로그 라인 수
count_over_time({service_name="lgtm-stack"}[5m])

# 에러 비율
sum(rate({service_name="lgtm-stack", level="ERROR"}[5m]))
/
sum(rate({service_name="lgtm-stack"}[5m]))

# 레이블별 그룹화
sum by (level) (count_over_time({service_name="lgtm-stack"}[5m]))
```

## 로그 레이블 구조

### Spring Boot 애플리케이션

```
Labels:
  service_name: "lgtm-stack"
  host: "...."
  level: "INFO" | "ERROR" | "WARN" | "DEBUG"
```

### Docker 컨테이너

```
Labels:
  service_name: "lgtm-stack-tempo-1"
  container: "lgtm-stack-tempo-1"
```

## 성능 최적화

### 1. 레이블 카디널리티 관리

**나쁜 예** (높은 카디널리티):
```xml
<labels>
    user_id = %X{userId}        <!-- 수천~수만 가지 값 -->
    request_id = %X{requestId}  <!-- 매 요청마다 다른 값 -->
</labels>
```

**좋은 예** (낮은 카디널리티):
```xml
<labels>
    service_name = ${appName}   <!-- 고정 값 -->
    level = %level              <!-- 5가지 값 (ERROR, WARN, INFO, DEBUG, TRACE) -->
    environment = ${env}        <!-- 고정 값 (dev, prod) -->
</labels>
```

**이유**: Loki는 레이블 조합마다 별도 스트림을 생성. 너무 많은 스트림은 성능 저하

### 2. Batch 설정

```xml
<batch>
    <maxItems>100</maxItems>      <!-- 100개 로그 모아서 전송 -->
    <timeoutMs>10000</timeoutMs>  <!-- 10초마다 전송 -->
</batch>
```

### 3. 로그 보존 기간

```yaml
limits_config:
  retention_period: 168h  # 7일
```

## 모니터링

### Loki 메트릭

Loki는 자체 메트릭을 `/metrics` 엔드포인트로 노출:

```bash
curl http://localhost:3100/metrics
```

**주요 메트릭:**
- `loki_ingester_streams`: 활성 로그 스트림 수
- `loki_ingester_chunks_created_total`: 생성된 청크 수
- `loki_request_duration_seconds`: 요청 처리 시간

## 참고 자료

- [Loki 공식 문서](https://grafana.com/docs/loki/latest/)
- [LogQL 쿼리 가이드](https://grafana.com/docs/loki/latest/logql/)
- [Loki4j GitHub](https://github.com/loki4j/loki-logback-appender)
