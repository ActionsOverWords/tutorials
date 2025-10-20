# Grafana Mimir

Grafana Mimir는 Prometheus를 위한 확장 가능하고 고성능의 장기 메트릭 저장소입니다. Prometheus와 완벽하게 호환되며, 멀티테넌시, 수평 확장, 장기 보존을 지원합니다.

## 역할

- **Metrics 저장**: Prometheus Remote Write 프로토콜로 메트릭 수신 및 저장
- **Exemplar 저장**: Metrics와 Traces를 연결하는 Exemplar 데이터 저장
- **PromQL 쿼리**: Prometheus 쿼리 언어로 메트릭 조회
- **장기 보존**: Time-series 데이터의 효율적인 압축 및 장기 저장

## 아키텍처

```
┌────────────────────────────────────────────────────────┐
│                  Data Sources                          │
├────────────────────────────────────────────────────────┤
│                                                        │
│  ┌────────────────┐        ┌────────────────┐          │
│  │ Grafana Alloy  │        │ Grafana Tempo  │          │
│  │ (Prometheus)   │        │ (Span Metrics) │          │
│  └───────┬────────┘        └───────┬────────┘          │
│          │                         │                   │
│          │ Remote Write            │ Remote Write      │
│          │ + Exemplars             │ + Exemplars       │
└──────────┼─────────────────────────┼───────────────────┘
           │                         │
           ▼                         ▼
┌────────────────────────────────────────────────────────┐
│              Grafana Mimir (Single Instance)           │
│                                                        │
│  ┌───────────────┐                                     │
│  │  Distributor  │  ◄─── Remote Write Requests         │
│  └───────┬───────┘                                     │
│          │                                             │
│          ▼                                             │
│  ┌───────────────┐                                     │
│  │   Ingester    │  ◄─── Metrics + Exemplars           │
│  │               │       Memory → WAL → Block          │
│  └───────┬───────┘                                     │
│          │                                             │
│          ▼                                             │
│  ┌───────────────┐                                     │
│  │   Compactor   │  ◄─── Block Compaction              │
│  └───────────────┘                                     │
│                                                        │
│  ┌───────────────────────────────────┐                 │
│  │ Storage (Filesystem)              │                 │
│  │  - Blocks: /data/mimir/blocks     │                 │
│  │  - Compactor: /data/mimir/compactor│                │
│  │  - Ruler: /data/mimir/ruler       │                 │
│  └───────────────────────────────────┘                 │
└──────────────────────────┬─────────────────────────────┘
                           │
                           │ PromQL Query
                           ▼
                   ┌─────────────────┐
                   │    Grafana      │
                   │  (Visualization)│
                   └─────────────────┘
```

## Mimir 설정 (`mimir.yml`)

### Single Instance 설정

```yaml
# Mimir configuration for single-instance mode
target: all,alertmanager

multitenancy_enabled: false

server:
  http_listen_port: 9009
  log_level: info

# Storage 설정
blocks_storage:
  backend: filesystem
  filesystem:
    dir: /data/mimir/blocks
  tsdb:
    retention_period: 24h

# Compactor: 블록 압축 및 정리
compactor:
  data_dir: /data/mimir/compactor

# Ingester: 메트릭 수신 및 저장
ingester:
  ring:
    replication_factor: 1  # Single instance: 복제본 1개

# Ruler: Alerting/Recording rules 저장
ruler_storage:
  backend: filesystem
  filesystem:
    dir: /data/mimir/ruler

# Limits & Exemplar
limits:
  max_query_lookback: 0
  max_global_exemplars_per_user: 100000
```

### 주요 설정 설명

#### 1. Target

```yaml
target: all,alertmanager
```

**의미:**
- `all`: 모든 Mimir 컴포넌트를 단일 프로세스로 실행
  - Distributor (메트릭 수신)
  - Ingester (메트릭 저장)
  - Querier (쿼리 처리)
  - Compactor (블록 압축)
  - Store Gateway (장기 저장소 접근)
- `alertmanager`: Alertmanager 기능 포함 (알림 관리)

**Single vs Microservices:**
- **Single Instance** (현재): 모든 컴포넌트가 하나의 프로세스
- **Microservices**: 각 컴포넌트가 별도 프로세스/컨테이너

#### 2. Multitenancy

```yaml
multitenancy_enabled: false
```

**의미:**
- 멀티테넌시 비활성화 (단일 조직만 사용)
- `X-Scope-OrgID` 헤더 불필요
- 더 간단한 설정

**활성화 시:**
```yaml
multitenancy_enabled: true
```
- 각 요청에 `X-Scope-OrgID: team-a` 같은 헤더 필요
- 데이터가 조직별로 격리됨

#### 3. Blocks Storage

```yaml
blocks_storage:
  backend: filesystem
  filesystem:
    dir: /data/mimir/blocks
  tsdb:
    retention_period: 24h
```

**역할:**
- **Backend**: 저장소 타입 (filesystem, s3, gcs 등)
- **Directory**: 메트릭 블록 저장 경로
- **Retention**: 데이터 보존 기간 (24시간)

**Block 구조:**
```
/data/mimir/blocks/
├── 01234567890ABCDEF/  # Block ID
│   ├── chunks/
│   │   └── 000001      # Time-series 데이터
│   ├── index           # 인덱스 파일
│   └── meta.json       # 메타데이터
└── ...
```

#### 4. Ingester

```yaml
ingester:
  ring:
    replication_factor: 1
```

**역할:**
- 메트릭을 메모리에 버퍼링
- 주기적으로 디스크에 flush (Block 생성)
- `replication_factor: 1`: Single instance이므로 복제본 1개만

**동작 방식:**
```
Remote Write Request
    ↓
Distributor
    ↓
Ingester (Memory Buffer)
    ↓ (주기적으로)
Flush to Disk (Block)
```

#### 5. Exemplar Limits

```yaml
limits:
  max_global_exemplars_per_user: 100000
```

**의미:**
- 한 사용자(tenant)가 저장할 수 있는 전체 Exemplar 최대 개수
- 100,000개 초과 시 오래된 Exemplar부터 삭제 (LRU)

**권장값:**
- 개발/테스트: 100,000 (현재 설정)
- 소규모 프로덕션: 100,000 ~ 500,000
- 대규모 프로덕션: 1,000,000+

## 데이터 수신 방식

### 1. Prometheus Remote Write from Alloy

**Alloy 설정** (`alloy.river`):
```river
prometheus.remote_write "mimir" {
  endpoint {
    url = "http://mimir:9009/api/v1/push"
    send_exemplars = true  # Exemplar 전송
  }
}
```

**수신 데이터:**
- Spring Boot Actuator에서 수집한 메트릭
- HTTP 요청 duration, count
- JVM 메트릭 (heap, threads, GC 등)
- **Exemplar**: 각 histogram bucket에 trace_id 포함

### 2. Tempo Metrics Generator

**Tempo 설정** (`tempo.yml`):
```yaml
metrics_generator:
  storage:
    remote_write:
      - url: http://mimir:9009/api/v1/push
        send_exemplars: true
```

**수신 데이터:**
- Span에서 생성된 메트릭:
  - `traces_spanmetrics_latency_bucket`: Span duration histogram
  - `traces_spanmetrics_calls_total`: Span 호출 수
  - `traces_service_graph_request_total`: 서비스 간 호출
- **Exemplar**: 각 메트릭에 trace_id 포함

## Exemplar 동작 원리

### Exemplar란?

Exemplar은 메트릭 데이터 포인트에 첨부된 "샘플 예제"로, 특정 메트릭 값이 발생한 순간의 추가 정보(예: trace_id)를 담고 있습니다.

### Exemplar 저장

**Histogram Bucket + Exemplar:**
```
http_server_requests_seconds_bucket{le="0.1", job="lgtm-stack-app"} 42
# {trace_id="a1b2c3d4e5f6789012345678901234ab"} 0.085 1705290000
```

**구조:**
- **Metric**: `http_server_requests_seconds_bucket{le="0.1"} 42`
- **Exemplar**:
  - `trace_id`: Tempo에서 조회 가능한 Trace ID
  - `value`: 실제 duration 값 (0.085초)
  - `timestamp`: 발생 시각

### Grafana에서 Exemplar 사용

1. **Mimir에서 메트릭 조회**:
   ```promql
   rate(http_server_requests_seconds_count{job="lgtm-stack-app"}[5m])
   ```

2. **그래프에 Exemplar 점(파란 점) 표시**

3. **점 클릭** → 팝업에서 "Query with Tempo" 버튼

4. **Tempo로 자동 이동** → 해당 Trace 확인

## PromQL 쿼리

### 기본 메트릭 조회

```promql
# HTTP 요청 수
http_server_requests_seconds_count{job="lgtm-stack-app"}

# 초당 HTTP 요청 수 (rate)
rate(http_server_requests_seconds_count{job="lgtm-stack-app"}[5m])

# P95 레이턴시
histogram_quantile(0.95,
  rate(http_server_requests_seconds_bucket{job="lgtm-stack-app"}[5m])
)

# 평균 레이턴시
rate(http_server_requests_seconds_sum{job="lgtm-stack-app"}[5m])
/
rate(http_server_requests_seconds_count{job="lgtm-stack-app"}[5m])
```

### Span Metrics (Tempo 생성)

```promql
# Span 호출 수
rate(traces_spanmetrics_calls_total{service_name="lgtm-stack"}[5m])

# Span P99 레이턴시
histogram_quantile(0.99,
  rate(traces_spanmetrics_latency_bucket{service_name="lgtm-stack"}[5m])
)

# 에러율
sum(rate(traces_spanmetrics_calls_total{
  service_name="lgtm-stack",
  status_code="STATUS_CODE_ERROR"
}[5m]))
/
sum(rate(traces_spanmetrics_calls_total{
  service_name="lgtm-stack"
}[5m]))
```

### Service Graph (Tempo 생성)

```promql
# 서비스 간 호출 수
traces_service_graph_request_total{
  client="lgtm-stack",
  server="mariadb"
}

# 서비스 간 에러율
rate(traces_service_graph_request_failed_total{
  client="lgtm-stack"
}[5m])
/
rate(traces_service_graph_request_total{
  client="lgtm-stack"
}[5m])
```

## 데이터 보존 및 압축

### Block Lifecycle

```
1. Ingester (Memory)
   ↓ (2시간마다)
2. Flush to Block (Initial)
   ↓
3. Compactor (압축)
   ├── 2시간 블록 → 12시간 블록
   ├── 12시간 블록 → 24시간 블록
   └── 24시간 블록 → 7일 블록
   ↓ (retention_period 초과)
4. 삭제
```

### Retention 설정

```yaml
blocks_storage:
  tsdb:
    retention_period: 24h  # 24시간 후 삭제
```

**프로덕션 권장값:**
- 단기 데이터: 7d ~ 15d
- 장기 데이터: 90d ~ 1y

## Grafana 연동

### Datasource 설정 (`datasource.yml`)

```yaml
- uid: mimir
  name: Mimir
  type: prometheus
  url: http://mimir:9009/prometheus
  isDefault: true
  jsonData:
    # Exemplar → Tempo 연동
    exemplarTraceIdDestinations:
      - name: trace_id
        datasourceUid: tempo
```

**설정 설명:**
- `type: prometheus`: Prometheus 호환 API 사용
- `url`: Mimir의 Prometheus 엔드포인트 (`/prometheus` prefix)
- `exemplarTraceIdDestinations`: Exemplar의 `trace_id` 레이블로 Tempo 조회

## 성능 최적화

### 1. Cardinality 관리

**나쁜 예** (높은 카디널리티):
```promql
http_requests{user_id="12345", session_id="abc123", request_id="xyz789"}
```
- 레이블 조합이 무한대로 증가 → 성능 저하

**좋은 예** (낮은 카디널리티):
```promql
http_requests{method="GET", status="200", endpoint="/api/users"}
```
- 레이블 조합이 제한적 → 효율적

### 2. Query 최적화

```promql
# 나쁜 예: 전체 데이터 스캔
sum(http_requests)

# 좋은 예: 레이블 필터링
sum(http_requests{job="lgtm-stack-app", method="GET"})

# 더 좋은 예: 시간 범위 제한 + rate
sum(rate(http_requests{job="lgtm-stack-app"}[5m]))
```

### 3. Ingester Tuning

```yaml
ingester:
  ring:
    replication_factor: 1
  # Flush 주기 설정
  chunk_idle_period: 30m      # 30분간 업데이트 없으면 flush
  chunk_target_size: 1572864  # 1.5MB
```

## 모니터링

### Mimir 자체 메트릭

```bash
curl http://localhost:9009/metrics
```

**주요 메트릭:**
- `cortex_ingester_memory_series`: 메모리에 있는 시계열 수
- `cortex_ingester_active_series`: 활성 시계열 수
- `cortex_distributor_received_samples_total`: 받은 샘플 수
- `cortex_exemplar_exemplars_in_storage`: 저장된 Exemplar 수

### Grafana Dashboard

Mimir는 기본 Grafana Dashboard를 제공합니다:
- [Mimir Writes Dashboard](https://grafana.com/grafana/dashboards/15983)
- [Mimir Reads Dashboard](https://grafana.com/grafana/dashboards/15984)

## 트러블슈팅

### 1. Exemplar가 표시되지 않는 경우

**확인:**
```bash
# Mimir에 Exemplar가 저장되었는지 확인
curl 'http://localhost:9009/prometheus/api/v1/query?query=http_server_requests_seconds_bucket' \
  | jq '.data.result[0].exemplars'
```

**가능한 원인:**
- Alloy/Tempo에서 `send_exemplars: true` 설정 누락
- Histogram 메트릭이 아님 (Counter/Gauge에는 Exemplar 없음)
- `max_global_exemplars_per_user` 제한 초과

### 2. "too many outstanding requests" 에러

**원인**: Ingester가 요청을 처리하지 못함

**해결:**
```yaml
limits:
  max_global_series_per_user: 150000  # 기본값 증가
  ingestion_rate: 10000               # 초당 샘플 수 증가
```

### 3. 디스크 공간 부족

**확인:**
```bash
du -sh /data/mimir/*
```

**해결:**
- `retention_period` 단축
- 오래된 블록 수동 삭제
- Compactor 로그 확인 (압축 작동 여부)

## API 엔드포인트

### Prometheus 호환 API

```bash
# 쿼리
curl -G http://localhost:9009/prometheus/api/v1/query \
  --data-urlencode 'query=up'

# Range 쿼리
curl -G http://localhost:9009/prometheus/api/v1/query_range \
  --data-urlencode 'query=rate(http_requests[5m])' \
  --data-urlencode 'start=2024-01-01T00:00:00Z' \
  --data-urlencode 'end=2024-01-01T01:00:00Z' \
  --data-urlencode 'step=60s'

# Label 조회
curl http://localhost:9009/prometheus/api/v1/labels

# Label 값 조회
curl http://localhost:9009/prometheus/api/v1/label/job/values
```

### Remote Write

```bash
# Prometheus Remote Write Protocol
curl -X POST http://localhost:9009/api/v1/push \
  -H 'Content-Type: application/x-protobuf' \
  --data-binary @metrics.pb
```

## 참고 자료

- [Mimir 공식 문서](https://grafana.com/docs/mimir/latest/)
- [PromQL 쿼리 가이드](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [Exemplars 설명](https://grafana.com/docs/grafana/latest/fundamentals/exemplars/)
- [Cardinality 최적화](https://grafana.com/docs/mimir/latest/manage/monitor-grafana-mimir/about-grafana-mimir-architecture/#cardinality)
