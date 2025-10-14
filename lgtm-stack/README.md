# LGTM Stack Observability Platform

Grafana LGTM 스택(Loki, Grafana, Tempo, Mimir)과 Alloy를 활용한 Full-Stack Observability 플랫폼

## 개요

이 프로젝트는 Spring Boot 애플리케이션에서 생성된 **Logs, Metrics, Traces**를 수집, 저장, 시각화하며, **Exemplar**를 통해 각 신호를 연결하는 통합 관측성 시스템입니다.

### 주요 특징

- **통합 관측성**: Logs, Metrics, Traces가 하나의 플랫폼에서 연동
- **Exemplar 기반 연동**: Metrics → Traces, Logs → Traces 자동 연결
- **단일 인스턴스 구성**: 로컬 개발 환경에 최적화된 간소화된 설정
- **자동 수집**: Alloy를 통한 자동화된 데이터 파이프라인
- **Grafana 통합**: 모든 데이터를 하나의 UI에서 조회 및 분석

## 아키텍처

### 전체 구성도

```
┌────────────────────────────────────────────────────────────────────┐
│                        Spring Boot Application                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐  │
│  │   Metrics    │  │    Traces    │  │         Logs             │  │
│  │ (Prometheus) │  │    (OTLP)    │  │   (Loki4j Appender)      │  │
│  └──────┬───────┘  └──────┬───────┘  └───────────┬──────────────┘  │
│         │                 │                      │                 │
└─────────┼─────────────────┼──────────────────────┼─────────────────┘
          │                 │                      │
          │ HTTP Pull       │ OTLP HTTP            │ HTTP Push
          │ (30s)           │ Push                 │ Direct
          │                 │                      │
┌─────────▼─────────────────▼──────────────────────▼─────────────────┐
│                           Grafana Alloy                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐  │
│  │  Prometheus  │  │     OTLP     │  │    Loki Source           │  │
│  │   Scraper    │  │   Receiver   │  │  (Docker Logs)           │  │
│  └──────┬───────┘  └──────┬───────┘  └───────────┬──────────────┘  │
│         │                 │                      │                 │
│         │ + Exemplar      │                      │                 │
└─────────┼─────────────────┼──────────────────────┼─────────────────┘
          │                 │                      │
          │ Remote Write    │ OTLP gRPC            │ Remote Write
          ▼                 ▼                      ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐
│    Mimir     │  │    Tempo     │  │          Loki            │
│  (Metrics)   │  │   (Traces)   │  │         (Logs)           │
│              │  │              │  │                          │
│ + Exemplars  │◄─┤ Generates    │  │                          │
│              │  │ Exemplars    │  │                          │
└──────┬───────┘  └──────┬───────┘  └───────────┬──────────────┘
       │                 │                      │
       │                 │                      │
       └─────────────────┼──────────────────────┘
                         │
                         ▼
                ┌─────────────────┐
                │     Grafana     │
                │  (Visualization)│
                │                 │
                │  - Explore      │
                │  - Dashboards   │
                │  - Alerting     │
                └─────────────────┘
```

## 데이터 흐름

### 1. Metrics 흐름

```
Spring Boot App
    │
    │ GET /actuator/prometheus (30초마다)
    │ OpenMetrics Format + Exemplars
    ▼
Grafana Alloy (Prometheus Scraper)
    │
    │ Exemplar 파싱: trace_id 추출
    │ Remote Write Protocol
    ▼
Grafana Mimir
    │
    │ Exemplar → Tempo 연동 설정
    ▼
Grafana (Metrics 조회)
    │
    │ Exemplar 점 클릭
    ▼
Grafana (Tempo로 자동 이동)
```

**핵심**: Histogram 메트릭의 Exemplar에 `trace_id`가 포함되어 있어, 특정 메트릭 값이 발생한 순간의 Trace를 바로 확인 가능

### 2. Traces 흐름

```
Spring Boot App (Micrometer Tracing)
    │
    │ OTLP HTTP (Port 4318)
    │ OpenTelemetry Protocol
    ▼
Grafana Alloy (OTLP Receiver)
    │
    │ OTLP gRPC (Port 4317)
    ▼
Grafana Tempo
    │
    │ Metrics Generator 활성화
    │ span_metrics, service_graphs 생성
    ▼
Mimir (Span Metrics + Exemplars)
```

**핵심**: Tempo가 Trace를 받으면서 동시에 Span 기반 메트릭을 생성하고 Exemplar를 Mimir에 전송

### 3. Logs 흐름

```
┌─ Spring Boot App ───────────┐
│  Logback + Loki4j Appender  │
│  Pattern: [traceId,spanId]  │
│     │                       │
│     │ HTTP Push (직접)       │
│     ▼                       │
└─────────────────────────────┘
      │
      ▼
Grafana Loki
      │
      │ Derived Fields: traceId 추출
      │ Regex: \[([0-9a-f]{32}),([0-9a-f]{16})\]
      ▼
Grafana (Logs 조회)
      │
      │ "View Trace" 버튼 클릭
      ▼
Grafana (Tempo로 자동 이동)
```

**핵심**: 로그에 `[traceId,spanId]` 형식으로 Trace Context가 포함되어 있어, 로그에서 해당 Trace로 즉시 이동 가능

## 컴포넌트 상세

각 컴포넌트의 상세한 설정 및 역할은 아래 문서를 참고하세요:

- [Grafana Loki](./docs/Loki.md) - 로그 집계 및 저장
- [Grafana Tempo](./docs/Tempo.md) - 분산 추적 (Distributed Tracing)
- [Grafana Mimir](./docs/Mimir.md) - 메트릭 저장소 (Prometheus 호환)
- [Grafana Alloy](./docs/Alloy.md) - 통합 데이터 수집 파이프라인
- [Grafana](./docs/Grafana.md) - 시각화 및 데이터소스 연동

## 기술 스택

### Backend
- **Language**: Kotlin
- **Framework**: Spring Boot 3.5.6
- **JDK**: OpenJDK 21

### Observability
- **Metrics**: Micrometer Prometheus Registry
- **Traces**: Micrometer Tracing + OpenTelemetry
- **Logs**: Loki4j Logback Appender

### LGTM Stack
- **Loki**: 로그 저장소
- **Grafana**: 시각화
- **Tempo**: 분산 추적
- **Mimir**: 메트릭 저장소
- **Alloy**: 데이터 수집 파이프라인 (Prometheus 호환)

## 참고 자료

- [Grafana LGTM Stack 공식 문서](https://grafana.com/docs/)
- [Grafana Alloy 문서](https://grafana.com/docs/alloy/latest/)
- [OpenTelemetry 공식 문서](https://opentelemetry.io/)
- [Micrometer 문서](https://micrometer.io/)
- [Exemplars 설명](https://grafana.com/docs/grafana/latest/fundamentals/exemplars/)
