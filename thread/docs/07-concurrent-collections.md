# Concurrent Collections

## 일반 Collection의 문제점

### Thread-unsafe
```kotlin
// 안티패턴
val map = HashMap()

// Thread A: map.put("key", "value1")
// Thread B: map.put("key", "value2")
// 결과: 데이터 손실, 무한 루프, NullPointerException 가능
```

### Collections.synchronizedMap의 한계
```kotlin
val map = Collections.synchronizedMap(HashMap())
```
- 모든 메서드에 synchronized 적용 → 성능 저하
- 복합 연산은 여전히 불안전:
```kotlin
if (!map.containsKey(key)) { // (1)
    map[key] = value          // (2)
}
// (1)과 (2) 사이에 다른 Thread가 끼어들 수 있음
```

---

## ConcurrentHashMap

### 특징
- **세그먼트 락 (Segment Locking)**: 테이블을 여러 세그먼트로 분할, 각 세그먼트에 독립적인 락
- **읽기는 락 없음**: 대부분의 읽기 연산은 블로킹 없음
- **부분 락킹**: 쓰기는 해당 세그먼트만 락
- **원자적 복합 연산** 제공

**장점:**
- 높은 동시성 (여러 Thread가 다른 세그먼트 동시 수정)
- 읽기 성능 우수 (락 불필요)

### 기본 사용
```kotlin
val map = ConcurrentHashMap<String, Int>()

// Thread-safe 읽기/쓰기
map["count"] = 1
val count = map["count"]
```

### 주요 메서드

#### putIfAbsent - 중복 방지
```kotlin
// 키가 없을 때만 추가
val prev = map.putIfAbsent("key", 1)
if (prev == null) {
    // 새로 추가됨
}
```

#### computeIfAbsent - 캐시 패턴
```kotlin
// 값이 없으면 계산 후 저장 (원자적)
val value = map.computeIfAbsent(key) { k ->
    expensiveComputation(k)
}
```

#### merge - 카운터 증가
```kotlin
// 키가 없으면 1, 있으면 기존 값 + 1
map.merge("count", 1) { old, new -> old + new }
```

#### replace - 조건부 교체
```kotlin
// 기존 값이 oldValue일 때만 newValue로 교체
val replaced = map.replace("key", oldValue, newValue)
```

#### 벌크 연산
```kotlin
val map = ConcurrentHashMap<String, Int>()
map["A"] = 1
map["B"] = 2
map["C"] = 3

// forEach: 각 엔트리 처리
map.forEach(1) { key, value ->
    println("$key: $value")
}

// search: 조건 만족하는 첫 번째 값 반환
val result = map.search(1) { key, value ->
    if (value > 2) key else null
} // "C"

// reduce: 모든 값 합산
val sum = map.reduce(1,
    { _, value -> value },
    { v1, v2 -> v1 + v2 }
) // 6
```

> 첫 번째 인자는 **parallelism threshold**: 엔트리 수가 이보다 많으면 병렬 처리

#### putIfAbsent vs computeIfAbsent

| 메서드             | 값 계산 시점    | 효율성        | 사용 사례 |
|-----------------|------------|------------|-------|
| putIfAbsent     | 미리 계산      | 낮음 (항상 계산) | 단순 값  |
| computeIfAbsent | 키 없을 때만 계산 | 높음 (지연 계산) | 비싼 계산 |

**예시:**
```kotlin
// putIfAbsent: 항상 계산
map.putIfAbsent(key, expensiveComputation()) // 매번 실행

// computeIfAbsent: 필요할 때만 계산
map.computeIfAbsent(key) { k -> expensiveComputation() } // 키 없을 때만
```

**스레드 안전성:**
```kotlin
// 안전하지 않음
if (!map.containsKey(key)) {
    map[key] = compute() // Race condition!
}

// 안전함
map.computeIfAbsent(key) { compute() } // 원자적
```

### 사용 사례
- **캐시**: computeIfAbsent로 효율적 구현
- **카운터**: merge로 원자적 증가
- **공유 상태**: 여러 Thread가 읽기/쓰기하는 데이터
- **세션 관리**: 사용자 세션 저장

---

## CopyOnWriteArrayList

### 특징
- **쓰기 시 복사 (Copy-On-Write)**: 수정 시 내부 배열 전체 복사 (O(n))
- **읽기는 락 없음**: 반복 중에도 동시 수정 가능 (ConcurrentModificationException 없음)
- **불변 스냅샷**: iterator는 생성 시점의 데이터 사용

### 동작 방식
```kotlin
val list = CopyOnWriteArrayList<String>()

// add() 시 내부 동작
1. 내부 배열 복사
2. 복사본에 요소 추가
3. 원본 배열 참조를 복사본으로 교체 (원자적)
```

### 기본 사용
```kotlin
val list = CopyOnWriteArrayList<String>()

// 쓰기: 배열 복사 발생
list.add("A")
list.add("B")

// 읽기: 락 없음
for (s in list) {
    println(s) // 반복 중 다른 Thread가 add해도 안전
}
```

### 주요 메서드

#### 기본 연산
```kotlin
val list = CopyOnWriteArrayList<String>()

// 추가
list.add("item")           // 끝에 추가
list.add(0, "first")       // 특정 위치에 추가
list.addAll(listOf("A", "B")) // 여러 요소 추가

// 읽기
val item = list[0]         // 인덱스 접근
val size = list.size       // 크기 조회
val contains = list.contains("item") // 포함 여부

// 제거
list.remove("item")        // 요소 제거
list.removeAt(0)           // 인덱스로 제거
```

#### 반복 중 수정 안전
```kotlin
val list = CopyOnWriteArrayList(listOf("A", "B", "C"))

// Thread 1: 반복
for (s in list) {
    println(s) // "A", "B", "C" 출력
    Thread.sleep(100)
}

// Thread 2: 동시에 수정
list.add("D") // ConcurrentModificationException 발생 안 함
```

### 성능 특성
- **읽기**: O(1), 락 없음 → 매우 빠름
- **쓰기**: O(n), 배열 복사 → 느림
- **메모리**: 수정마다 배열 복사 → 메모리 소비 많음

---

## BlockingQueue

### 특징
- **Producer-Consumer 패턴**: 생산자-소비자 간 안전한 데이터 전달
- **자동 블로킹**: 큐가 가득 차거나 비어있을 때 자동 대기
- **Thread-safe**: 동시 접근 안전

### 동작 방식

#### 주요 구현체

**ArrayBlockingQueue**
```kotlin
// 고정 크기, 배열 기반
val queue = ArrayBlockingQueue<String>(10)
```
- 크기 고정
- FIFO 순서 보장
- 공정성 옵션 (fair/unfair)

**LinkedBlockingQueue**
```kotlin
val queue = LinkedBlockingQueue<String>()       // 무제한
val queue2 = LinkedBlockingQueue<String>(100)   // 제한
```
- 크기 설정 가능
- 읽기/쓰기 각각 독립적인 락
- 처리량 높음

**PriorityBlockingQueue**
```kotlin
// 우선순위 기반, 무제한
val queue = PriorityBlockingQueue<Task>()
```
- 우선순위 순서 (Comparable 또는 Comparator)
- 무제한 크기

### 기본 사용
```kotlin
val queue = ArrayBlockingQueue<String>(10)

// Producer Thread
executor.submit {
    for (i in 0 until 100) {
        queue.put("item-$i") // 큐가 가득 차면 자동 대기
    }
}

// Consumer Thread
executor.submit {
    while (true) {
        val item = queue.take() // 큐가 비면 자동 대기
        process(item)
    }
}
```

### 주요 메서드

| 동작 | 예외 던짐     | 특수 값 반환  | 블로킹    | 타임아웃           |
|----|-----------|----------|--------|----------------|
| 추가 | add(e)    | offer(e) | put(e) | offer(e, time) |
| 제거 | remove()  | poll()   | take() | poll(time)     |
| 검사 | element() | peek()   | -      | -              |

#### 메서드 특성
- **예외 던짐**: 실패 시 예외 (IllegalStateException, NoSuchElementException)
- **특수 값 반환**: 실패 시 null/false 반환 (예외 없음)
- **블로킹**: 성공할 때까지 대기
- **타임아웃**: 지정 시간 동안 블로킹

#### 사용 예시
```kotlin
val queue = ArrayBlockingQueue<String>(2)

// 1. add() - 예외 던짐
queue.add("A")
queue.add("B")
queue.add("C") // IllegalStateException

// 2. offer() - 특수 값 반환
if (queue.offer("C")) {
    println("추가 성공")
} else {
    println("큐가 가득 참") // false 반환
}

// 3. put() - 블로킹
queue.put("C") // 공간 생길 때까지 대기

// 4. offer() with timeout - 타임아웃
val success = queue.offer("C", 1, TimeUnit.SECONDS) // 1초 대기
```

```kotlin
// 제거 메서드
val item1 = queue.poll()            // 즉시 반환 (null 가능)
val item2 = queue.take()            // 요소 있을 때까지 대기
val item3 = queue.poll(1, TimeUnit.SECONDS) // 1초 대기

// 검사 메서드
val head = queue.peek()             // 제거 안 함
```

### 성능 특성
- **ArrayBlockingQueue**: 고정 크기, 단일 락, 적은 메모리
- **LinkedBlockingQueue**: 유연한 크기, 읽기/쓰기 독립 락, 높은 처리량
- **PriorityBlockingQueue**: 우선순위 정렬, O(log n) 추가/제거

---

## ConcurrentLinkedQueue

### 특징
- **Non-blocking**: Lock-free 알고리즘 (CAS 기반)
- **무제한 크기**: 메모리 허용 범위 내 무제한
- **높은 동시성**: 락 경합 없음
- 실패 시 재시도 (Spinning)

### 동작 방식
```kotlin
// CAS (Compare-And-Swap) 기반
1. 현재 값 읽기
2. 새 값 계산
3. 원자적으로 비교 후 교체
4. 실패 시 재시도
```

### 기본 사용
```kotlin
val queue = ConcurrentLinkedQueue<String>()

// 추가 (항상 성공, 무제한)
queue.offer("item1")
queue.add("item2") // offer()와 동일

// 제거 (비블로킹, 즉시 반환)
val item = queue.poll() // 비어 있으면 null

// 검사
val head = queue.peek() // 제거 안 함
val isEmpty = queue.isEmpty()
val size = queue.size // O(n) - 주의!
```

### 주요 메서드

#### 기본 연산
```kotlin
val queue = ConcurrentLinkedQueue<String>()

// 추가 (블로킹 없음)
queue.offer("item")    // 추가, 항상 true 반환
queue.add("item")      // offer()와 동일

// 제거 (블로킹 없음)
val item = queue.poll()      // 제거 후 반환, 비어있으면 null
val head = queue.peek()      // 제거 안 함, 비어있으면 null

// 검사
val isEmpty = queue.isEmpty()
val size = queue.size        // 주의: O(n) 순회 필요!
```

#### BlockingQueue와 비교
```kotlin
// BlockingQueue: 블로킹
val blockingQueue = LinkedBlockingQueue<String>()
val item1 = blockingQueue.take() // 요소 있을 때까지 대기

// ConcurrentLinkedQueue: 비블로킹
val queue = ConcurrentLinkedQueue<String>()
val item2 = queue.poll() // 즉시 null 반환
```

### 성능 특성
- **추가/제거**: O(1) amortized, Lock-free
- **검사 (peek)**: O(1)
- **크기 (size)**: O(n) - 전체 순회 필요, 사용 지양
- **메모리**: 링크 노드 오버헤드
- **적합**: 높은 동시성, 블로킹 불필요, 짧은 작업

---

### 자료구조 비교

#### ConcurrentHashMap vs CopyOnWriteArrayList

| 항목     | ConcurrentHashMap | CopyOnWriteArrayList |
|--------|-------------------|----------------------|
| 쓰기 방식  | 세그먼트 락            | 전체 배열 복사             |
| 읽기 락   | 없음                | 없음                   |
| 쓰기 비용  | 낮음 (락만)           | 높음 (복사)              |
| 적합한 비율 | 읽기/쓰기 균형          | 읽기 >> 쓰기             |
| 사용 사례  | 캐시, 공유 상태         | 리스너, 설정              |

#### BlockingQueue vs ConcurrentLinkedQueue

| 항목     | BlockingQueue     | ConcurrentLinkedQueue |
|--------|-------------------|-----------------------|
| 블로킹    | O (take/put)      | X                     |
| 크기 제한  | 가능                | 불가 (무제한)              |
| 동시성 제어 | Lock 기반           | Lock-free (CAS)       |
| 사용 사례  | Producer-Consumer | 높은 동시성 필요             |
| 대기     | 조건 만족까지 대기        | 즉시 반환                 |

#### 전체 비교표

| 자료구조                  | 동시성 제어        | 읽기 성능 | 쓰기 성능 | 메모리 | 적합 패턴             |
|-----------------------|---------------|-------|-------|-----|-------------------|
| ConcurrentHashMap     | 세그먼트 락        | 우수    | 양호    | 중간  | 읽기/쓰기 균형          |
| CopyOnWriteArrayList  | 배열 복사         | 우수    | 나쁨    | 많음  | 읽기 >> 쓰기          |
| BlockingQueue         | ReentrantLock | 양호    | 양호    | 적음  | Producer-Consumer |
| ConcurrentLinkedQueue | Lock-free     | 우수    | 우수    | 중간  | 높은 동시성            |

---

## 실무 선택 가이드

### 읽기/쓰기 비율에 따른 선택

#### 일반 웹 애플리케이션 (읽기 70% / 쓰기 30%)
```kotlin
// ✓ 권장: ConcurrentHashMap
val cache = ConcurrentHashMap<String, User>()

// ✗ 비권장: CopyOnWriteArrayList
// 쓰기 30%는 배열 복사 오버헤드가 너무 큼
```

**사용 사례:**
- 사용자 세션 캐시
- API 응답 캐시
- 설정 값 조회/수정
- 공유 데이터 저장소

#### 읽기 집약적 시스템 (읽기 90% 이상 / 쓰기 10% 미만)
```kotlin
// ✓ 권장: CopyOnWriteArrayList
val listeners = CopyOnWriteArrayList<EventListener>()
val config = CopyOnWriteArrayList<ConfigEntry>()
```

**사용 사례:**
- 이벤트 리스너 목록 (등록/해제 드묾, 호출 빈번)
- 애플리케이션 설정 (변경 드묾, 조회 빈번)
- 불변 참조 데이터 (국가 코드, 상수 등)
- 라우팅 규칙 (업데이트 드묾, 매칭 빈번)

#### 쓰기 집약적 시스템 (쓰기 50% 이상)
```kotlin
// ✓ 권장: ConcurrentHashMap 또는 BlockingQueue
val writeCache = ConcurrentHashMap<String, Data>()
val writeQueue = LinkedBlockingQueue<LogEntry>()

// ✗ 비권장: CopyOnWriteArrayList
// 쓰기마다 배열 복사로 성능 급격히 저하
```

**사용 사례:**
- 로깅 시스템
- 실시간 데이터 수집
- 쓰기 버퍼
- 메트릭 수집

### 성능 비교 (10 Thread, 10,000 반복 기준)

| Collection            | 읽기/쓰기 비율  | 예상 성능  | 평가          |
|-----------------------|-----------|--------|-------------|
| ConcurrentHashMap     | 70% / 30% | ~30ms  | 안정적         |
| CopyOnWriteArrayList  | 90% / 10% | ~10ms  | 효율적         |
| CopyOnWriteArrayList  | 70% / 30% | ~300ms | 비효율적 (30배↑) |
| BlockingQueue         | 50% / 50% | ~40ms  | 블로킹 포함      |
| ConcurrentLinkedQueue | 50% / 50% | ~20ms  | Lock-free   |

### 사용 패턴별 선택

#### Map이 필요한 경우
- **읽기/쓰기 균형**: `ConcurrentHashMap`
  ```kotlin
  val sessionCache = ConcurrentHashMap<String, Session>()
  ```
- **읽기만**: `Collections.unmodifiableMap(HashMap())`
  ```kotlin
  val constants = Collections.unmodifiableMap(mapOf("KEY" to "VALUE"))
  ```
- **캐시 패턴**: `ConcurrentHashMap` + computeIfAbsent
  ```kotlin
  val userCache = ConcurrentHashMap<Long, User>()
  val user = userCache.computeIfAbsent(userId) { db.loadUser(it) }
  ```

#### List가 필요한 경우
- **읽기 90% 이상**: `CopyOnWriteArrayList`
  ```kotlin
  val eventListeners = CopyOnWriteArrayList<Listener>()
  ```
- **읽기/쓰기 균형**: `Collections.synchronizedList` 또는 락 직접 관리
  ```kotlin
  val list = Collections.synchronizedList(ArrayList<String>())
  ```
- **순차 접근만**: `ConcurrentLinkedQueue` + 리스트로 변환
  ```kotlin
  val queue = ConcurrentLinkedQueue<String>()
  val list = queue.toList() // 필요 시 변환
  ```

#### Queue가 필요한 경우
- **Producer-Consumer**: `BlockingQueue`
  ```kotlin
  // 블로킹 필요 (대기 가능)
  val taskQueue = LinkedBlockingQueue<Task>()
  ```
- **높은 동시성**: `ConcurrentLinkedQueue`
  ```kotlin
  // 블로킹 불필요 (즉시 처리)
  val eventQueue = ConcurrentLinkedQueue<Event>()
  ```
- **우선순위**: `PriorityBlockingQueue`
  ```kotlin
  // 우선순위 기반 처리
  val priorityTasks = PriorityBlockingQueue<Task>()
  ```

### 의사 결정 플로우차트

```
동시성 컬렉션 필요?
├─ Map 타입?
│  └─ ConcurrentHashMap 사용
│
├─ List 타입?
│  ├─ 읽기 90% 이상? → CopyOnWriteArrayList
│  └─ 쓰기 10% 이상? → Collections.synchronizedList 또는 락 관리
│
└─ Queue 타입?
   ├─ 블로킹 필요? → BlockingQueue
   │  ├─ 고정 크기? → ArrayBlockingQueue
   │  ├─ 유연한 크기? → LinkedBlockingQueue
   │  └─ 우선순위? → PriorityBlockingQueue
   │
   └─ 비블로킹? → ConcurrentLinkedQueue
```

---

## 요약 및 Best Practices

### 핵심 원칙

#### 1. 읽기/쓰기 비율 확인
- **읽기 90% 이상** → CopyOnWriteArrayList
- **균형 잡힌 비율 (70/30)** → ConcurrentHashMap
- **쓰기 집약적 (50% 이상)** → BlockingQueue

#### 2. 동시성 수준 고려
- **높은 동시성 필요** → ConcurrentHashMap, ConcurrentLinkedQueue
- **블로킹 필요** → BlockingQueue
- **순차 처리** → Collections.synchronized*

#### 3. 메모리 vs 성능
- **CopyOnWriteArrayList**: 메모리 많이 사용, 읽기 빠름
- **ConcurrentHashMap**: 메모리 효율적, 균형 잡힌 성능
- **ConcurrentLinkedQueue**: 링크 노드 오버헤드, Lock-free

#### 4. 블로킹 여부
- **블로킹 가능**: BlockingQueue (대기 시간 허용)
- **블로킹 불가**: ConcurrentLinkedQueue (즉시 처리 필요)

### 피해야 할 패턴

#### 안티패턴 1: 쓰기 많은 상황에서 CopyOnWriteArrayList
```kotlin
// ✗ 비권장: 쓰기가 많은 상황
val list = CopyOnWriteArrayList<String>()
for (i in 0..10000) {
    list.add("item-$i") // 매번 배열 복사! (매우 느림)
}

// ✓ 권장: Queue 또는 Map 사용
val queue = ConcurrentLinkedQueue<String>()
for (i in 0..10000) {
    queue.offer("item-$i") // Lock-free, 빠름
}
```

#### 안티패턴 2: synchronizedMap에서 복합 연산
```kotlin
// ✗ 비권장: Race condition
val map = Collections.synchronizedMap(HashMap<String, Int>())
if (!map.containsKey("count")) { // (1)
    map["count"] = 0              // (2)
}
// (1)과 (2) 사이에 다른 Thread가 끼어들 수 있음!

// ✓ 권장: 원자적 연산 사용
val map = ConcurrentHashMap<String, Int>()
map.putIfAbsent("count", 0) // 원자적, 안전
```

#### 안티패턴 3: size() 메서드 빈번 호출
```kotlin
// ✗ 비권장: ConcurrentLinkedQueue의 size() 호출
val queue = ConcurrentLinkedQueue<String>()
while (queue.size > 0) { // O(n) 순회! 매번 전체 탐색
    queue.poll()
}

// ✓ 권장: isEmpty() 또는 poll() 결과 확인
while (true) {
    val item = queue.poll() ?: break // null이면 종료
    process(item)
}
```

#### 안티패턴 4: 일반 Collection을 synchronized로 감싸기
```kotlin
// ✗ 비권장: 성능 저하
val map = Collections.synchronizedMap(HashMap<String, User>())

// ✓ 권장: 처음부터 동시성 컬렉션 사용
val map = ConcurrentHashMap<String, User>()
```
