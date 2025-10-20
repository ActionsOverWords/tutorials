# Grafana Tempo

Grafana Tempo는 대규모 분산 추적(Distributed Tracing) 백엔드로, 비용 효율적이고 확장 가능한 trace 저장소입니다. Jaeger, Zipkin과 호환되며 OpenTelemetry를 네이티브로 지원합니다.

## 역할

- **Trace 저장**: OpenTelemetry 프로토콜로 받은 trace 데이터 저장
- **Span Metrics 생성**: Trace에서 메트릭을 자동 생성하여 Mimir에 전송
- **Exemplar 생성**: Metrics와 Traces를 연결하는 Exemplar 데이터 생성
- **Service Graph**: 서비스 간 의존성 자동 생성

## 아키텍처

```
┌────────────────────────────────────────────────────────┐
│              Spring Boot Application                   │
│                                                        │
│    Micrometer Tracing + OpenTelemetry                  │
└────────────────────┬───────────────────────────────────┘
                     │
                     │ OTLP HTTP (Port 4318)
                     ▼
┌────────────────────────────────────────────────────────┐
│                  Grafana Alloy                         │
│                                                        │
│  OTLP Receiver (HTTP + gRPC)                           │
└────────────────────┬───────────────────────────────────┘
                     │
                     │ OTLP gRPC (Port 4317)
                     ▼
┌────────────────────────────────────────────────────────┐
│                  Grafana Tempo                         │
│                                                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ Distributor  │  │   Ingester   │  │   Querier    │  │
│  └──────┬───────┘  └──────┬───────┘  └──────────────┘  │
│         │                 │                            │
│         │                 ▼                            │
│         │         ┌──────────────┐                     │
│         │         │   Storage    │                     │
│         │         │ (Filesystem) │                     │
│         │         └──────────────┘                     │
│         │                                              │
│         ▼                                              │
│  ┌──────────────────┐                                  │
│  │ Metrics Generator│                                  │
│  │ - Span Metrics   │                                  │
│  │ - Service Graph  │                                  │
│  │ + Exemplars      │                                  │
│  └────────┬─────────┘                                  │
└───────────┼────────────────────────────────────────────┘
            │
            │ Remote Write + Exemplars
            ▼
┌────────────────────────────────────────────────────────┐
│                  Grafana Mimir                         │
│                                                        │
│   Span Metrics + Exemplars (trace_id)                  │
└────────────────────────────────────────────────────────┘
```

## Tempo 설정 (`tempo.yml`)

### 전체 설정

```yaml
server:
  http_listen_port: 3200

# OTLP Receiver: Spring Boot -> Alloy -> Tempo
distributor:
  receivers:
    otlp:
      protocols:
        grpc:
          endpoint: 0.0.0.0:4317  # Alloy에서 접근
        http:
          endpoint: 0.0.0.0:4318  # 직접 접근 가능 (사용 안 함)

# Trace Storage
storage:
  trace:
    backend: local
    local:
      path: /tmp/tempo/blocks

# Metrics Generator: Trace -> Metrics 변환
metrics_generator:
  registry:
    external_labels:
      source: tempo
      cluster: lgtm-stack
  storage:
    path: /tmp/tempo/generator/wal
    remote_write:
      - url: http://mimir:9009/api/v1/push
        send_exemplars: true  # Exemplar 전송 활성화

  # Span Metrics: 개별 Span 기반 메트릭 생성
  processor:
    service_graphs:
      dimensions:
        - service.name
        - service.namespace
    span_metrics:
      dimensions:
        - service.name
        - http.method
        - http.status_code
        - http.route
        - status.code
        - db.system
        - db.name
        - db.operation

# Metrics Generator 프로세서 활성화
overrides:
  metrics_generator_processors: [service-graphs, span-metrics]
```

### 주요 설정 설명

#### 1. Distributor - OTLP Receiver

```yaml
distributor:
  receivers:
    otlp:
      protocols:
        grpc:
          endpoint: 0.0.0.0:4317
        http:
          endpoint: 0.0.0.0:4318
```

**역할:**
- OpenTelemetry 프로토콜로 trace 데이터 수신
- gRPC와 HTTP 프로토콜 모두 지원
- 이 프로젝트에서는 Alloy가 gRPC(4317)로 전송

#### 2. Metrics Generator

```yaml
metrics_generator:
  storage:
    remote_write:
      - url: http://mimir:9009/api/v1/push
        send_exemplars: true
```

**역할:**
- Trace 데이터에서 자동으로 메트릭 생성
- Prometheus Remote Write 프로토콜로 Mimir에 전송
- **Exemplar 포함**: 각 메트릭에 trace_id 포함

**생성되는 메트릭 예시:**
```promql
# HTTP 요청 기간 (Histogram)
traces_spanmetrics_latency_bucket{
  service_name="lgtm-stack",
  http_method="GET",
  http_route="/test",
  http_status_code="200"
}

# HTTP 요청 수 (Counter)
traces_spanmetrics_calls_total{
  service_name="lgtm-stack",
  http_method="GET"
}

# 서비스 간 호출 (Service Graph)
traces_service_graph_request_total{
  client="lgtm-stack",
  server="mariadb"
}
```

#### 3. Span Metrics Dimensions

```yaml
span_metrics:
  dimensions:
    - service.name
    - http.method
    - http.status_code
    - http.route
    - status.code
    - db.system
    - db.name
    - db.operation
```

**의미:**
- Span의 attribute를 메트릭 레이블로 변환
- 각 dimension은 메트릭의 레이블이 됨
- 너무 많은 dimension은 카디널리티 증가 → 성능 저하

**예시:**
```
Span Attributes:
  service.name = "lgtm-stack"
  http.method = "GET"
  http.route = "/user/{id}"
  http.status_code = "200"

→ Metrics Labels:
  service_name="lgtm-stack"
  http_method="GET"
  http_route="/user/{id}"
  http_status_code="200"
```

## Trace 데이터 흐름

### 1. Trace 생성 (Spring Boot)

**Spring Boot 설정** (`application.yml`):
```yaml
management:
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces  # Alloy OTLP HTTP
  tracing:
    sampling:
      probability: 1.0  # 모든 요청 추적 (100%)

otel:
  resource:
    attributes:
      service.name: lgtm-stack
      service.namespace: tutorials
```

**생성되는 Span 구조:**
```
TraceID: a1b2c3d4e5f6789012345678901234ab
└── Span: GET /test
    ├── service.name: lgtm-stack
    ├── http.method: GET
    ├── http.route: /test
    ├── http.status_code: 200
    └── duration: 125ms
    └── Child Span: SQL Query
        ├── db.system: mariadb
        ├── db.name: tutorials
        ├── db.operation: SELECT
        └── duration: 45ms
```

### 2. Tempo → Mimir Exemplar 생성

Tempo의 Metrics Generator가 Span을 처리하면서:

```
Span (TraceID: abc123, Duration: 125ms)
    ↓
Metrics Generator
    ↓
Histogram Bucket 업데이트 + Exemplar 생성
    ↓
Remote Write to Mimir:
  traces_spanmetrics_latency_bucket{le="0.1"} 1
  # {trace_id="abc123"} 0.125 1234567890
```

**Exemplar 구조:**
- **trace_id**: `abc123...` (Trace로 이동 가능)
- **value**: `0.125` (실제 duration 값)
- **timestamp**: `1234567890` (발생 시각)

## Grafana 연동

### Datasource 설정 (`datasource.yml`)

```yaml
- uid: tempo
  name: Tempo
  type: tempo
  url: http://tempo:3200
  jsonData:
    httpMethod: GET

    # Tempo에서 Loki로 연동 (Trace -> Logs)
    tracesToLogsV2:
      datasourceUid: 'loki'
      spanStartTimeShift: '-1h'
      spanEndTimeShift: '1h'
      filterByTraceID: true
      filterBySpanID: false
      tags:
        - key: 'service.name'
          value: 'service'

    # Tempo에서 Mimir로 연동 (Trace -> Metrics)
    tracesToMetrics:
      datasourceUid: 'mimir'

    # Service Map 표시
    serviceMap:
      datasourceUid: 'mimir'

    # Node Graph 활성화
    nodeGraph:
      enabled: true
```

### Traces → Logs 연동

**동작 방식:**
1. Grafana에서 Trace 확인
2. 특정 Span 선택
3. "Logs for this span" 버튼 클릭
4. Loki에서 다음 쿼리 실행:
   ```logql
   {service_name="lgtm-stack"} |= "a1b2c3d4e5f6789012345678901234ab"
   ```
5. 해당 TraceID를 포함한 로그만 필터링하여 표시

**시간 범위:**
- `spanStartTimeShift: '-1h'`: Span 시작 1시간 전부터
- `spanEndTimeShift: '1h'`: Span 종료 1시간 후까지
- 로그가 Trace 시간 범위를 벗어나도 검색 가능

### Traces → Metrics 연동

**동작 방식:**
1. Trace에서 "Metrics" 탭 클릭
2. Tempo가 자동으로 Mimir 쿼리 생성:
   ```promql
   rate(traces_spanmetrics_calls_total{
     service_name="lgtm-stack",
     http_route="/test"
   }[5m])
   ```
3. 해당 엔드포인트의 호출 빈도, 에러율 등 표시

## TraceQL 쿼리

Tempo는 TraceQL이라는 쿼리 언어를 지원합니다.

### 기본 검색

```traceql
# 서비스 이름으로 검색
{ service.name = "lgtm-stack" }

# HTTP 메서드로 검색
{ http.method = "POST" }

# 상태 코드로 검색
{ http.status_code = 500 }

# Duration으로 검색 (느린 요청)
{ duration > 1s }
```

### 복합 조건

```traceql
# 느린 에러 요청
{ service.name = "lgtm-stack" && http.status_code = 500 && duration > 500ms }

# DB 쿼리가 포함된 Trace
{ resource.service.name = "lgtm-stack" } && { span.db.system = "mariadb" }

# 특정 사용자의 요청
{ user.id = "john@example.com" }
```

### 집계

```traceql
# 평균 duration
{ service.name = "lgtm-stack" } | avg(duration)

# 최대 duration
{ service.name = "lgtm-stack" } | max(duration)

# P95 duration
{ service.name = "lgtm-stack" } | quantile(duration, 0.95)
```

## Service Graph

Service Graph는 서비스 간 의존성을 자동으로 시각화합니다.

### 생성 메트릭

```promql
# 서비스 간 요청 수
traces_service_graph_request_total{
  client="lgtm-stack",
  server="mariadb"
}

# 서비스 간 요청 실패 수
traces_service_graph_request_failed_total{
  client="lgtm-stack",
  server="mariadb"
}

# 서비스 간 요청 duration
traces_service_graph_request_server_seconds{
  client="lgtm-stack",
  server="mariadb"
}
```

### Grafana에서 확인

1. Explore → Tempo 선택
2. "Service Graph" 탭 클릭
3. 서비스 다이어그램 표시:
   ```
   ┌──────────────┐      ┌──────────────┐
   │  lgtm-stack  │─────►│   mariadb    │
   │              │      │              │
   │  100 req/s   │      │  95 req/s    │
   └──────────────┘      └──────────────┘
   ```

## 성능 최적화

### 1. Sampling (샘플링)

**프로덕션 환경:**
```yaml
management:
  tracing:
    sampling:
      probability: 0.1  # 10%만 추적
```

**개발 환경:**
```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% 추적
```

### 2. Metrics Generator Dimensions 최소화

```yaml
span_metrics:
  dimensions:
    - service.name  # 필수
    - http.method   # 필수
    - http.status_code  # 선택적
    # http.route는 카디널리티가 높으므로 제거 고려
```

### 3. Retention Period

```yaml
storage:
  trace:
    backend: local
    local:
      path: /tmp/tempo/blocks
    # WAL과 Block 보존 기간 설정
    wal:
      path: /tmp/tempo/wal
    block:
      retention: 168h  # 7일
```

## 모니터링

### Tempo 자체 메트릭

```bash
curl http://localhost:3200/metrics
```

**주요 메트릭:**
- `tempo_distributor_spans_received_total`: 받은 span 수
- `tempo_ingester_traces_created_total`: 생성된 trace 수
- `tempo_metrics_generator_spans_total`: 처리된 span 수 (Metrics Generator)

## 참고 자료

- [Tempo 공식 문서](https://grafana.com/docs/tempo/latest/)
- [TraceQL 쿼리 가이드](https://grafana.com/docs/tempo/latest/traceql/)
- [OpenTelemetry 문서](https://opentelemetry.io/docs/)
- [Span Metrics 개념](https://grafana.com/docs/tempo/latest/metrics-generator/span_metrics/)
