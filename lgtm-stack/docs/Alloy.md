# Grafana Alloy

Grafana Alloy는 OpenTelemetry Collector를 기반으로 한 통합 데이터 수집 파이프라인입니다. Logs, Metrics, Traces를 한 곳에서 수집하고 라우팅할 수 있습니다.

## 역할

- **Metrics 수집**: Prometheus scrape로 Spring Boot Actuator에서 메트릭 수집
- **Traces 수집**: OTLP 프로토콜로 OpenTelemetry trace 수신
- **Logs 수집**: Docker 컨테이너 로그 자동 수집
- **데이터 라우팅**: 수집한 데이터를 Loki, Mimir, Tempo로 전송
- **Exemplar 처리**: Metrics와 Traces를 연결하는 Exemplar 수집 및 전송

## 아키텍처

```
┌───────────────────────────────────────────────────────────┐
│                    Grafana Alloy                          │
│                   (Port 12345: UI)                        │
├───────────────────────────────────────────────────────────┤
│                                                           │
│  ┌─────────────────────────────────────────────────────┐  │
│  │         1. OTLP Receiver (Traces)                   │  │
│  │  ┌────────────────┐  ┌────────────────┐             │  │
│  │  │  gRPC :4317    │  │  HTTP :4318    │             │  │
│  │  └───────┬────────┘  └───────┬────────┘             │  │
│  │          │                   │                      │  │
│  │          └───────────┬───────┘                      │  │
│  │                      │                              │  │
│  │                      ▼                              │  │
│  │            otelcol.exporter.otlp                    │  │
│  └──────────────────────┬──────────────────────────────┘  │
│                         │ OTLP gRPC                       │
│                         ▼                                 │
│                    Tempo :4317                            │
│                                                           │
│  ┌─────────────────────────────────────────────────────┐  │
│  │         2. Prometheus Scraper (Metrics)             │  │
│  │                                                     │  │
│  │  prometheus.scrape                                  │  │
│  │    ↓ HTTP GET /actuator/prometheus (30s)            │  │
│  │  Spring Boot App (10.2.114.21:8080)                 │  │
│  │    ↓ OpenMetrics Format + Exemplars                 │  │
│  │  prometheus.remote_write                            │  │
│  └──────────────────────┬──────────────────────────────┘  │
│                         │ Remote Write + Exemplars        │
│                         ▼                                 │
│                    Mimir :9009                            │
│                                                           │
│  ┌─────────────────────────────────────────────────────┐  │
│  │         3. Loki Source (Docker Logs)                │  │
│  │                                                     │  │
│  │  discovery.docker                                   │  │
│  │    ↓ Docker Socket                                  │  │
│  │  loki.source.docker                                 │  │
│  │    ↓ Container Logs                                 │  │
│  │  loki.relabel                                       │  │
│  │    ↓ Add Labels (service_name, container)           │  │
│  │  loki.write                                         │  │
│  └──────────────────────┬──────────────────────────────┘  │
│                         │ Remote Write                    │
│                         ▼                                 │
│                    Loki :3100                             │
└───────────────────────────────────────────────────────────┘
```

## Alloy 설정 (`alloy.river`)

### 전체 파이프라인

```river
// ============================================
// 1. OTLP Receiver: Traces 수신
// ============================================
otelcol.receiver.otlp "default" {
  grpc {
    endpoint = "0.0.0.0:4317"
  }
  http {
    endpoint = "0.0.0.0:4318"
  }

  output {
    traces = [otelcol.exporter.otlp.tempo.input]
  }
}

// ============================================
// 2. OTLP Exporter: Traces 전송
// ============================================
otelcol.exporter.otlp "tempo" {
  client {
    endpoint = "tempo:4317"
    tls {
      insecure = true
    }
  }
}

// ============================================
// 3. Loki: Docker 로그 수집
// ============================================
discovery.docker "docker_containers" {
  host = "unix:///var/run/docker.sock"
}

loki.source.docker "docker_logs" {
  host = "unix:///var/run/docker.sock"
  targets = discovery.docker.docker_containers.targets
  forward_to = [loki.relabel.docker_logs.receiver]
  relabel_rules = loki.relabel.docker_logs.rules
}

loki.relabel "docker_logs" {
  forward_to = [loki.write.default.receiver]

  rule {
    source_labels = ["__meta_docker_container_name"]
    regex = "/(.*)"
    target_label = "service_name"
  }

  rule {
    source_labels = ["__meta_docker_container_name"]
    target_label = "container"
  }
}

loki.write "default" {
  endpoint {
    url = "http://loki:3100/loki/api/v1/push"
  }
}

// ============================================
// 4. Prometheus: Metrics 수집
// ============================================
prometheus.scrape "spring_boot" {
  targets = [{
    "__address__" = "10.2.114.21:8080",
    "job" = "lgtm-stack-app",
    "instance" = "spring-boot-app",
  }]
  forward_to = [prometheus.remote_write.mimir.receiver]
  scrape_interval = "30s"
  metrics_path = "/actuator/prometheus"

  scrape_classic_histograms = true
  enable_protobuf_negotiation = true
  honor_labels = true
}

prometheus.remote_write "mimir" {
  endpoint {
    url = "http://mimir:9009/api/v1/push"
    send_exemplars = true
  }
}
```

## 파이프라인 상세 설명

### 1. OTLP Receiver (Traces)

```river
otelcol.receiver.otlp "default" {
  grpc {
    endpoint = "0.0.0.0:4317"  // gRPC 수신 포트
  }
  http {
    endpoint = "0.0.0.0:4318"  // HTTP 수신 포트
  }

  output {
    traces = [otelcol.exporter.otlp.tempo.input]  // Tempo exporter로 연결
  }
}
```

**역할:**
- Spring Boot 애플리케이션의 OpenTelemetry trace 수신
- gRPC(4317)와 HTTP(4318) 프로토콜 모두 지원
- 이 프로젝트에서는 HTTP(4318) 사용

**데이터 흐름:**
```
Spring Boot (OTLP HTTP)
  → Alloy OTLP Receiver :4318
  → OTLP Exporter
  → Tempo :4317 (gRPC)
```

**사용 이유:**
- Spring Boot에서 Tempo로 직접 전송하지 않고 Alloy 경유
- Alloy에서 trace 샘플링, 필터링, 변환 가능 (현재는 pass-through)

### 2. OTLP Exporter (Traces)

```river
otelcol.exporter.otlp "tempo" {
  client {
    endpoint = "tempo:4317"  // Tempo gRPC 엔드포인트
    tls {
      insecure = true        // TLS 비활성화 (로컬 개발)
    }
  }
}
```

**역할:**
- 받은 trace를 Tempo로 전송
- OTLP gRPC 프로토콜 사용

**프로덕션 설정:**
```river
client {
  endpoint = "tempo.example.com:4317"
  tls {
    insecure = false
    ca_file = "/path/to/ca.crt"
  }
}
```

### 3. Loki Source (Docker Logs)

#### Discovery

```river
discovery.docker "docker_containers" {
  host = "unix:///var/run/docker.sock"
}
```

**역할:**
- Docker 소켓을 통해 실행 중인 컨테이너 자동 발견
- 새 컨테이너가 시작되면 자동으로 로그 수집 시작

#### Source

```river
loki.source.docker "docker_logs" {
  host = "unix:///var/run/docker.sock"
  targets = discovery.docker.docker_containers.targets
  forward_to = [loki.relabel.docker_logs.receiver]
  relabel_rules = loki.relabel.docker_logs.rules
}
```

**역할:**
- 발견된 컨테이너의 stdout/stderr 로그 수집
- Docker 메타데이터도 함께 수집 (컨테이너 이름, 이미지 등)

#### Relabel

```river
loki.relabel "docker_logs" {
  forward_to = [loki.write.default.receiver]

  // 컨테이너 이름에서 '/' 제거 후 service_name으로 설정
  rule {
    source_labels = ["__meta_docker_container_name"]
    regex = "/(.*)"             // "/" 제거
    target_label = "service_name"
  }

  // 원본 컨테이너 이름도 유지
  rule {
    source_labels = ["__meta_docker_container_name"]
    target_label = "container"
  }
}
```

**역할:**
- Docker 메타데이터를 Loki 레이블로 변환
- 불필요한 레이블 제거
- 레이블 이름 표준화

**변환 예시:**
```
Input:
  __meta_docker_container_name = "/lgtm-stack-tempo-1"

Output:
  service_name = "lgtm-stack-tempo-1"
  container = "/lgtm-stack-tempo-1"
```

#### Write

```river
loki.write "default" {
  endpoint {
    url = "http://loki:3100/loki/api/v1/push"
  }
}
```

**역할:**
- 수집한 로그를 Loki로 전송
- Prometheus Remote Write 프로토콜 사용

### 4. Prometheus Scraper (Metrics)

#### Scrape

```river
prometheus.scrape "spring_boot" {
  targets = [{
    "__address__" = "10.2.114.21:8080",  // Spring Boot 주소
    "job" = "lgtm-stack-app",            // 메트릭 레이블
    "instance" = "spring-boot-app",      // 메트릭 레이블
  }]
  forward_to = [prometheus.remote_write.mimir.receiver]
  scrape_interval = "30s"                // 30초마다 수집
  metrics_path = "/actuator/prometheus"  // 메트릭 엔드포인트

  scrape_classic_histograms = true       // Histogram 메트릭 수집
  enable_protobuf_negotiation = true     // OpenMetrics 협상
  honor_labels = true                    // 대상의 레이블 우선
}
```

**각 옵션 설명:**

**targets:**
- `__address__`: 스크랩 대상 주소 (Docker 컨테이너에서 호스트 접근용 IP)
- `job`, `instance`: 모든 메트릭에 자동으로 추가되는 레이블

**scrape_interval:**
- 메트릭 수집 주기
- 너무 짧으면: CPU/네트워크 부하 증가
- 너무 길면: 세밀한 데이터 수집 불가
- 권장: 15s ~ 60s

**scrape_classic_histograms:**
- Prometheus Classic Histogram 수집 활성화
- Histogram은 Exemplar를 포함할 수 있음

**enable_protobuf_negotiation:**
- OpenMetrics format 협상 활성화
- Spring Boot가 OpenMetrics를 지원하면 Exemplar 포함된 형식으로 응답

**honor_labels:**
- 대상이 반환한 레이블을 우선 사용
- 충돌 시 대상의 레이블 유지

#### Remote Write

```river
prometheus.remote_write "mimir" {
  endpoint {
    url = "http://mimir:9009/api/v1/push"
    send_exemplars = true  // Exemplar 전송 활성화
  }
}
```

**역할:**
- 수집한 메트릭을 Mimir로 전송
- **send_exemplars: true**: Histogram에 포함된 Exemplar도 함께 전송

**Exemplar 예시:**
```
http_server_requests_seconds_bucket{le="0.1"} 42
# {trace_id="a1b2c3d4e5f6789012345678901234ab"} 0.085 1705290000
```

## Docker 설정

### Compose 설정

```yaml
alloy:
  image: grafana/alloy:v1.11.0
  volumes:
    - ./docker/alloy/alloy.river:/etc/alloy/config.river
    - /var/run/docker.sock:/var/run/docker.sock:ro  # Docker 로그 수집용
  command:
    - "run"
    - "--server.http.listen-addr=0.0.0.0:12345"
    - "/etc/alloy/config.river"
  ports:
    - "12345:12345"  # Alloy UI
    - "4318:4318"    # OTLP HTTP for Spring Boot
```

**주요 설정:**
- **Docker Socket**: 컨테이너 로그 수집을 위해 마운트 (읽기 전용)
- **Config File**: `alloy.river` 파이프라인 설정
- **UI Port**: 12345번 포트로 Alloy 상태 확인 가능

## Alloy UI

### 접근

http://localhost:12345

### 주요 기능

1. **Component Graph**:
   - 파이프라인의 각 컴포넌트 시각화
   - 데이터 흐름 확인

2. **Component Details**:
   - 각 컴포넌트의 설정 및 상태
   - 처리된 데이터 통계

3. **Health**:
   - Alloy 서비스 상태
   - 각 컴포넌트의 health check

4. **Clustering**:
   - 클러스터 구성 시 노드 상태 (Single instance에서는 미사용)

## 데이터 처리 통계

### Alloy Metrics

Alloy는 자체 메트릭을 `/metrics` 엔드포인트로 노출:

```bash
curl http://localhost:12345/metrics
```

**주요 메트릭:**

**OTLP Receiver:**
```
# 받은 Span 수
otelcol_receiver_accepted_spans_total{receiver="otlp"}

# 거부된 Span 수
otelcol_receiver_refused_spans_total{receiver="otlp"}
```

**Prometheus Scraper:**
```
# 스크랩한 샘플 수
prometheus_target_scrapes_sample_scraped{job="lgtm-stack-app"}

# 스크랩 duration
prometheus_target_interval_length_seconds{job="lgtm-stack-app"}
```

**Loki Source:**
```
# 전송한 로그 라인 수
loki_write_sent_entries_total

# 전송 실패 수
loki_write_dropped_entries_total
```

## 파이프라인 확장

### 샘플링 추가

```river
// Trace 샘플링 (10%만 전송)
otelcol.processor.probabilistic_sampler "sampler" {
  sampling_percentage = 10

  output {
    traces = [otelcol.exporter.otlp.tempo.input]
  }
}

otelcol.receiver.otlp "default" {
  // ...
  output {
    traces = [otelcol.processor.probabilistic_sampler.sampler.input]
  }
}
```

### 메트릭 필터링

```river
// 특정 메트릭만 전송
prometheus.relabel "filter" {
  forward_to = [prometheus.remote_write.mimir.receiver]

  rule {
    source_labels = ["__name__"]
    regex = "http_.*"       // http_로 시작하는 메트릭만
    action = "keep"
  }
}

prometheus.scrape "spring_boot" {
  // ...
  forward_to = [prometheus.relabel.filter.receiver]
}
```

### 로그 필터링

```river
// 특정 레벨 이상만 전송
loki.process "filter" {
  forward_to = [loki.write.default.receiver]

  stage.match {
    selector = '{level=~"ERROR|WARN"}'  // ERROR, WARN만
    action = "keep"
  }
}

loki.relabel "docker_logs" {
  forward_to = [loki.process.filter.receiver]
  // ...
}
```

## 트러블슈팅

### 1. Spring Boot 메트릭이 수집되지 않는 경우

**확인:**
```bash
# Alloy 컨테이너에서 Spring Boot 접근 확인
docker exec alloy curl http://10.2.114.21:8080/actuator/prometheus

# Alloy UI에서 prometheus.scrape 상태 확인
curl http://localhost:12345/api/v1/metrics/targets
```

**가능한 원인:**
- IP 주소가 잘못됨 (호스트 IP 확인 필요)
- Spring Boot가 실행되지 않음
- 방화벽 차단

### 2. Docker 로그가 수집되지 않는 경우

**확인:**
```bash
# Docker 소켓 권한 확인
ls -l /var/run/docker.sock

# Alloy가 Docker 소켓에 접근할 수 있는지 확인
docker logs alloy | grep docker
```

**해결:**
```yaml
# compose.yml
alloy:
  volumes:
    - /var/run/docker.sock:/var/run/docker.sock:ro
  user: "0"  # root로 실행 (권한 문제 시)
```

### 3. Exemplar가 전송되지 않는 경우

**확인:**
1. Spring Boot가 OpenMetrics 형식으로 응답하는지 확인:
   ```bash
   curl -H "Accept: application/openmetrics-text" \
     http://10.2.114.21:8080/actuator/prometheus \
     | grep trace_id
   ```

2. Alloy 설정 확인:
   ```river
   enable_protobuf_negotiation = true  # 필수
   ```

3. Mimir 설정 확인:
   ```river
   send_exemplars = true  # 필수
   ```

## 성능 최적화

### 메모리 사용량 조절

```river
prometheus.scrape "spring_boot" {
  // ...
  enable_compression = true  // gzip 압축 활성화
}
```

### Batch 처리

```river
loki.write "default" {
  endpoint {
    url = "http://loki:3100/loki/api/v1/push"

    // Batch 설정
    batch_wait = "1s"    // 1초 동안 모아서 전송
    batch_size = 1048576 // 1MB씩 전송
  }
}
```

### 버퍼링

```river
prometheus.remote_write "mimir" {
  endpoint {
    url = "http://mimir:9009/api/v1/push"

    // Queue 설정
    queue_config {
      capacity = 10000
      max_shards = 50
      min_shards = 1
    }
  }
}
```

## 참고 자료

- [Alloy 공식 문서](https://grafana.com/docs/alloy/latest/)
- [River 언어 문법](https://grafana.com/docs/alloy/latest/reference/river/)
- [Component Reference](https://grafana.com/docs/alloy/latest/reference/components/)
- [OpenTelemetry Collector](https://opentelemetry.io/docs/collector/)
