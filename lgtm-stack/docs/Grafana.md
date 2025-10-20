# Grafana

Grafana는 오픈소스 관측성 및 데이터 시각화 플랫폼으로, Logs, Metrics, Traces를 통합하여 조회하고 분석할 수 있는 UI를 제공합니다.

## 역할

- **데이터 시각화**: Loki, Mimir, Tempo의 데이터를 통합 대시보드로 표시
- **Explore**: 애드혹 쿼리 및 데이터 탐색
- **Alerting**: 메트릭 기반 알림 설정
- **Datasource 연동**: Logs ↔ Traces ↔ Metrics 간 자동 연결

## 아키텍처

```
┌────────────────────────────────────────────────────────────┐
│                         Grafana UI                         │
│                      http://localhost:3000                 │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Explore    │  │  Dashboards  │  │   Alerting   │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
│         │                 │                 │              │
└─────────┼─────────────────┼─────────────────┼──────────────┘
          │                 │                 │
          │ Query           │ Query           │ Query
          ▼                 ▼                 ▼
┌────────────────────────────────────────────────────────────┐
│                      Datasources                           │
│                                                            │
│    ┌──────────┐    ┌──────────┐    ┌──────────┐            │
│    │   Loki   │    │   Mimir  │    │   Tempo  │            │
│    │  (Logs)  │    │(Metrics) │    │ (Traces) │            │
│    └────┬─────┘    └────┬─────┘    └────┬─────┘            │
│         │               │               │                  │
│         │ LogQL         │ PromQL        │ TraceQL          │
└─────────┼───────────────┼───────────────┼──────────────────┘
          │               │               │
          ▼               ▼               ▼
    ┌─────────┐     ┌─────────┐     ┌─────────┐
    │  Loki   │     │  Mimir  │     │  Tempo  │
    │  :3100  │     │  :9009  │     │  :3200  │
    └─────────┘     └─────────┘     └─────────┘
```

## Grafana 설정

### Docker Compose 설정

```yaml
grafana:
  image: grafana/grafana:11.5.3
  ports:
    - "3000:3000"
  volumes:
    - ./docker/grafana/datasource.yml:/etc/grafana/provisioning/datasources/datasources.yml
  environment:
    - GF_AUTH_ANONYMOUS_ENABLED=true        # 익명 접근 허용
    - GF_AUTH_ANONYMOUS_ORG_ROLE=Admin      # 익명 사용자도 Admin 권한
    - GF_AUTH_DISABLE_LOGIN_FORM=true       # 로그인 폼 비활성화
```

**개발 환경 설정:**
- 로그인 없이 바로 접근 가능
- 모든 사용자가 Admin 권한

### Datasource Provisioning

**파일 위치**: `docker/grafana/datasource.yml`

```yaml
apiVersion: 1

datasources:
  # 1. Mimir (Metrics)
  - uid: mimir
    name: Mimir
    type: prometheus
    url: http://mimir:9009/prometheus
    isDefault: true
    jsonData:
      exemplarTraceIdDestinations:
        - name: trace_id
          datasourceUid: tempo
    editable: true

  # 2. Loki (Logs)
  - uid: loki
    name: Loki
    type: loki
    url: http://loki:3100
    jsonData:
      maxLines: 1000
      derivedFields:
        - datasourceUid: tempo
          matcherRegex: '\[([0-9a-f]{32}),([0-9a-f]{16})\]'
          name: traceId
          url: '$${__value.raw}'
          urlDisplayLabel: 'View Trace'
    editable: true

  # 3. Tempo (Traces)
  - uid: tempo
    name: Tempo
    type: tempo
    url: http://tempo:3200
    jsonData:
      httpMethod: GET
      tracesToLogsV2:
        datasourceUid: 'loki'
        spanStartTimeShift: '-1h'
        spanEndTimeShift: '1h'
        filterByTraceID: true
        filterBySpanID: false
        tags:
          - key: 'service.name'
            value: 'service'
      tracesToMetrics:
        datasourceUid: 'mimir'
      serviceMap:
        datasourceUid: 'mimir'
      nodeGraph:
        enabled: true
    editable: true
```

## Datasource 연동 설정

### 1. Mimir → Tempo (Exemplar)

```yaml
jsonData:
  exemplarTraceIdDestinations:
    - name: trace_id          # Exemplar의 레이블 이름
      datasourceUid: tempo    # 연결할 Datasource
```

**동작:**
- Mimir의 메트릭 쿼리 결과에 Exemplar 점 표시
- Exemplar 점 클릭 → `trace_id` 추출 → Tempo 조회

**사용 예:**
```
Mimir Query: rate(http_server_requests_seconds_count[5m])
    ↓
Graph with Exemplar dots (파란 점)
    ↓ 점 클릭
Query with Tempo
    ↓
Tempo Trace View
```

### 2. Loki → Tempo (Derived Fields)

```yaml
jsonData:
  derivedFields:
    - datasourceUid: tempo
      matcherRegex: '\[([0-9a-f]{32}),([0-9a-f]{16})\]'
      name: traceId
      url: '$${__value.raw}'
      urlDisplayLabel: 'View Trace'
```

**동작:**
- Loki 로그에서 `[traceId,spanId]` 패턴 검색
- 매칭되면 "View Trace" 링크 표시
- 링크 클릭 → Tempo 조회

**정규식 설명:**
- `\[`: `[` 문자 (escaped)
- `([0-9a-f]{32})`: 32자리 16진수 traceId (Capture Group 1)
- `,`: 구분자
- `([0-9a-f]{16})`: 16자리 16진수 spanId (Capture Group 2)
- `\]`: `]` 문자 (escaped)

**로그 예시:**
```
2025-01-15 10:30:45 INFO : [a1b2c3d4...ab,1234...cdef] User login success
                              ↑                   ↑
                              traceId              spanId
```

### 3. Tempo → Loki (Traces to Logs)

```yaml
jsonData:
  tracesToLogsV2:
    datasourceUid: 'loki'
    spanStartTimeShift: '-1h'
    spanEndTimeShift: '1h'
    filterByTraceID: true
    tags:
      - key: 'service.name'
        value: 'service'
```

**동작:**
- Tempo Trace에서 "Logs for this span" 버튼 표시
- 버튼 클릭 → Loki에서 다음 쿼리 실행:
  ```logql
  {service="lgtm-stack"} |= "a1b2c3d4e5f6789012345678901234ab"
  ```
- TraceID를 포함한 로그만 필터링하여 표시

**시간 범위:**
- Span 시작 1시간 전 ~ 종료 1시간 후
- 로그와 Trace의 시간 차이 보정

### 4. Tempo → Mimir (Traces to Metrics)

```yaml
jsonData:
  tracesToMetrics:
    datasourceUid: 'mimir'
  serviceMap:
    datasourceUid: 'mimir'
  nodeGraph:
    enabled: true
```

**동작:**
- Tempo Trace에서 "Metrics" 탭 표시
- Tempo가 생성한 Span Metrics 자동 조회:
  ```promql
  rate(traces_spanmetrics_calls_total{service_name="lgtm-stack"}[5m])
  ```

**Service Map:**
- 서비스 간 의존성 다이어그램 표시
- Mimir의 `traces_service_graph_*` 메트릭 사용

**Node Graph:**
- Trace의 Span 관계를 그래프로 시각화

## Explore 사용법

### Loki (Logs)

1. **Explore 메뉴** 선택
2. **Datasource: Loki** 선택
3. **Query 입력**:
   ```logql
   {service_name="lgtm-stack"}
   ```
4. **Run Query** 실행
5. **로그 확인**:
   - 로그 라인에 파란색 아이콘 표시 (TraceID 링크)
   - 아이콘 클릭 → "View Trace"

### Mimir (Metrics)

1. **Explore 메뉴** 선택
2. **Datasource: Mimir** 선택
3. **Query 입력**:
   ```promql
   rate(http_server_requests_seconds_count{job="lgtm-stack-app"}[5m])
   ```
4. **Run Query** 실행
5. **그래프 확인**:
   - 파란색 점(Exemplar) 표시
   - 점 클릭 → "Query with Tempo"

### Tempo (Traces)

1. **Explore 메뉴** 선택
2. **Datasource: Tempo** 선택
3. **Search 옵션**:
   - **Service Name**: `lgtm-stack`
   - **Span Name**: (선택 사항)
   - **Duration**: `> 100ms` (느린 요청만)
4. **Run Query** 실행
5. **Trace 선택** → 상세 보기:
   - Span 타임라인
   - "Logs for this span" 버튼
   - "Metrics" 탭

## 통합 워크플로우

### 시나리오 1: 느린 API 디버깅

```
1. Explore → Mimir
   Query: histogram_quantile(0.99,
            rate(http_server_requests_seconds_bucket[5m]))
   → P99가 높은 엔드포인트 발견

2. Exemplar 점 클릭
   → 해당 느린 요청의 Trace 확인

3. Tempo Trace View
   → 어느 Span이 느린지 확인 (예: DB 쿼리 45ms)

4. "Logs for this span" 클릭
   → 해당 시점의 로그 확인

5. 로그에서 에러 메시지 발견
   → 근본 원인 파악
```

### 시나리오 2: 에러 로그 추적

```
1. Explore → Loki
   Query: {service_name="lgtm-stack", level="ERROR"}
   → 에러 로그 발견

2. "View Trace" 클릭
   → 에러가 발생한 Trace 확인

3. Tempo Trace View
   → 전체 요청 흐름 파악
   → 어느 단계에서 실패했는지 확인

4. "Metrics" 탭 클릭
   → 해당 엔드포인트의 에러율 확인
   → 일시적인 문제인지, 지속적인 문제인지 판단
```

### 시나리오 3: 서비스 의존성 분석

```
1. Explore → Tempo
   Service Name: lgtm-stack

2. "Service Graph" 탭 클릭
   → lgtm-stack → mariadb 호출 관계 확인
   → 호출 빈도, 에러율 확인

3. Mimir로 전환
   Query: traces_service_graph_request_total{
            client="lgtm-stack",
            server="mariadb"
          }
   → 시간에 따른 호출 변화 분석
```

## Dashboard 생성

### 기본 Dashboard

1. **Dashboards** 메뉴 → **New** → **New Dashboard**
2. **Add Visualization** 클릭
3. **Datasource 선택** (Mimir, Loki, Tempo)
4. **Query 입력**
5. **Visualization 설정** (Graph, Table, Heatmap 등)

### Panel 예시

**HTTP 요청 수 (Mimir)**:
```promql
sum(rate(http_server_requests_seconds_count{job="lgtm-stack-app"}[5m]))
  by (uri, method, status)
```

**에러 로그 (Loki)**:
```logql
sum(count_over_time({service_name="lgtm-stack", level="ERROR"}[5m]))
```

**P95 레이턴시 (Tempo/Mimir)**:
```promql
histogram_quantile(0.95,
  rate(traces_spanmetrics_latency_bucket{service_name="lgtm-stack"}[5m])
)
```

## Alerting

### Alert Rule 생성

1. **Alerting** 메뉴 → **Alert rules** → **New alert rule**
2. **Query 설정**:
   ```promql
   rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.1
   ```
3. **Condition**: 5xx 에러율이 10% 초과
4. **Evaluation**: 1분마다 평가
5. **Notification**: Email, Slack 등 설정

### 예시 Alert Rules

**높은 에러율**:
```promql
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
/
sum(rate(http_server_requests_seconds_count[5m]))
> 0.05
```

**느린 응답 시간**:
```promql
histogram_quantile(0.99,
  rate(http_server_requests_seconds_bucket[5m])
) > 1
```

**로그 에러 급증**:
```logql
sum(rate({service_name="lgtm-stack", level="ERROR"}[5m])) > 10
```

## 설정 최적화

### Query 성능

**나쁜 예**:
```promql
# 전체 데이터 스캔
http_server_requests_seconds_count
```

**좋은 예**:
```promql
# 레이블 필터링 + rate
rate(http_server_requests_seconds_count{job="lgtm-stack-app"}[5m])
```

### Panel 새로고침 간격

```
개발: 5s - 10s (빠른 피드백)
프로덕션: 30s - 1m (서버 부하 감소)
```

### Time Range 설정

```
Explore: Last 1 hour (기본값)
Dashboard: Last 6 hours ~ Last 24 hours
```

## 접근 주소

- **Grafana UI**: http://localhost:3000
- **Admin API**: http://localhost:3000/api/admin
- **Health Check**: http://localhost:3000/api/health

## 트러블슈팅

### 1. Datasource 연결 실패

**확인:**
```bash
# Grafana 컨테이너에서 다른 서비스 접근 확인
docker exec grafana ping loki
docker exec grafana ping mimir
docker exec grafana ping tempo
```

**해결:**
- `compose.yml`에서 모든 서비스가 같은 네트워크에 있는지 확인
- 서비스 이름이 올바른지 확인 (`loki:3100`, `mimir:9009`, `tempo:3200`)

### 2. Exemplar 버튼이 표시되지 않는 경우

**확인:**
1. Datasource 설정에서 `exemplarTraceIdDestinations` 확인
2. Mimir 쿼리 결과에 Exemplar가 포함되어 있는지 확인
3. Histogram 메트릭인지 확인 (Counter/Gauge는 Exemplar 없음)

### 3. "View Trace" 버튼이 표시되지 않는 경우

**확인:**
1. Datasource 설정에서 `derivedFields` 정규식 확인
2. 로그에 실제로 `[traceId,spanId]` 형식이 있는지 확인
3. Tempo에 해당 TraceID가 존재하는지 확인

## 참고 자료

- [Grafana 공식 문서](https://grafana.com/docs/grafana/latest/)
- [Datasource 설정 가이드](https://grafana.com/docs/grafana/latest/datasources/)
- [Dashboard 생성 가이드](https://grafana.com/docs/grafana/latest/dashboards/)
- [Alerting 가이드](https://grafana.com/docs/grafana/latest/alerting/)
- [Grafana dashboards](https://grafana.com/grafana/dashboards/)
