# Thread Pool

## 개념과 필요성

### 문제점: Thread 직접 생성
```kotlin
// 안티패턴
for (i in 0 until 10000) {
    Thread { process() }.start() // 10,000개 Thread 생성!
}
```
- Thread 생성/소멸 비용 높음 (OS 커널 관여)
- 메모리 과다 사용 (Thread당 1~2MB)
- Context switching 오버헤드

### Thread Pool 장점
- Thread 재사용 → 생성/소멸 비용 절감
- 동시 실행 Thread 수 제한 → 자원 보호
- 작업 큐잉으로 부하 조절

## ExecutorService 종류

### 1. newFixedThreadPool
```kotlin
val executor = Executors.newFixedThreadPool(10)
```
- **고정 크기** Thread Pool
- 작업이 많으면 큐에 대기
- **사용 사례**: 일정한 부하의 작업 처리

### 2. newCachedThreadPool
```kotlin
val executor = Executors.newCachedThreadPool()
```
- Thread 수 **무제한** 증가 (필요 시 생성, 60초 유휴 시 제거)
- **위험**: 작업이 많으면 Thread 폭발
- **사용 사례**: 짧고 비동기적인 작업

### 3. newSingleThreadExecutor
```kotlin
val executor = Executors.newSingleThreadExecutor()
```
- **단일 Thread**로 순차 실행
- 작업 순서 보장
- **사용 사례**: 순차 처리가 필요한 작업

### 4. newScheduledThreadPool
```kotlin
val executor = Executors.newScheduledThreadPool(5)
executor.schedule(task, 10, TimeUnit.SECONDS) // 10초 후 실행
executor.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS) // 1초마다
```
- 지연 실행, 주기적 실행
- **사용 사례**: 배치 작업, 모니터링

## ThreadPoolExecutor 직접 설정

### 생성자 파라미터
```kotlin
val executor = ThreadPoolExecutor(
    5,                      // corePoolSize
    10,                     // maximumPoolSize
    60L,                    // keepAliveTime
    TimeUnit.SECONDS,       // unit
    LinkedBlockingQueue<Runnable>(100), // workQueue
    threadFactory,          // threadFactory
    ThreadPoolExecutor.AbortPolicy()       // handler
)
```

### 동작 방식
1. 작업 제출 시 **core thread** 수보다 적으면 새 Thread 생성
2. Core thread 모두 사용 중이면 **queue**에 추가
3. Queue 가득 차면 **maximum** 크기까지 Thread 증가
4. Maximum 초과 시 **RejectedExecutionHandler** 실행

### corePoolSize vs maximumPoolSize
```
작업 수 ≤ corePoolSize → Thread 생성
작업 수 > corePoolSize → Queue에 추가
Queue 가득 참 → maximumPoolSize까지 Thread 증가
```

## 적절한 Pool Size 계산

### CPU-bound 작업
```
Nthreads = Ncpu + 1
```
- CPU 코어 수만큼 (계산 중심 작업)
- +1은 페이지 폴트 등 대비

### I/O-bound 작업
```
Nthreads = Ncpu × Ucpu × (1 + W/C)
```
- Ncpu: CPU 코어 수
- Ucpu: CPU 사용률 목표 (0 ~ 1)
- W/C: Wait time / Compute time 비율

**예시**: 코어 8개, I/O 대기가 계산의 9배
```
Nthreads = 8 × 1.0 × (1 + 9/1) = 80
```

### 실무 접근
1. 공식으로 초기값 설정
2. 부하 테스트로 튜닝
3. 모니터링 지표 확인:
   - Thread 사용률
   - Queue 크기
   - Task 대기 시간

## RejectedExecutionHandler

### 4가지 전략

#### 1. AbortPolicy (기본)
```kotlin
new ThreadPoolExecutor.AbortPolicy()
```
- **RejectedExecutionException** 던짐
- 작업 손실 방지, 빠른 실패

#### 2. CallerRunsPolicy
```kotlin
new ThreadPoolExecutor.CallerRunsPolicy()
```
- 호출한 Thread에서 직접 실행 (Pool 아님)
- Back pressure 효과 (호출자 느려짐)
- 작업 손실 없음

#### 3. DiscardPolicy
```kotlin
new ThreadPoolExecutor.DiscardPolicy()
```
- 조용히 버림 (예외 없음)
- 작업 손실 허용 시

#### 4. DiscardOldestPolicy
```kotlin
new ThreadPoolExecutor.DiscardOldestPolicy()
```
- Queue에서 가장 오래된 작업 제거 후 재시도
- 최신 작업 우선

### 커스텀 Handler
```kotlin
RejectedExecutionHandler handler = (task, executor) {
    log.error("Task rejected: {}", task)
    // 별도 큐에 저장, 알림 발송 등
};
```

## 작업 제출 방법

### execute vs submit

#### execute: 반환값 없음
```kotlin
executor.execute { 
    println("Task")
}
```

#### submit: Future 반환
```kotlin
val future = executor.submit { 
    return 42
}

val result = future.get() // 블로킹
```

### invokeAll: 여러 작업 일괄 실행
```kotlin
val tasks = listOf(
    { 1,
    { 2,
    { 3
)

val results = executor.invokeAll(tasks)
```

### invokeAny: 가장 빠른 결과
```kotlin
val result = executor.invokeAny(tasks); // 하나만 완료되면 반환
```

## Shutdown

### 두 가지 방법
```kotlin
// 1. Graceful shutdown
executor.shutdown() // 새 작업 거부, 기존 작업 완료 대기

// 2. Immediate shutdown
List<Runnable> notExecuted = executor.shutdownNow(); // 실행 중 작업 interrupt
```

### 권장 패턴
```kotlin
executor.shutdown()
try {
    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
        executor.shutdownNow()
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
            log.error("Executor did not terminate")
        }
    }
} catch (InterruptedException e) {
    executor.shutdownNow()
    Thread.currentThread().interrupt()
}
```

## ForkJoinPool

### 개념
Java 7에서 도입된 **Divide and Conquer** 작업에 최적화된 특수 Thread Pool

### 특징
- **Work-stealing** 알고리즘: 유휴 Thread가 바쁜 Thread의 작업 훔쳐옴
- **Deque** 기반: 각 Thread가 자신의 작업 큐 보유
- Java 8+ **Parallel Stream**의 기본 Pool

### Work-Stealing 알고리즘

#### 동작 원리
```
Thread 1: [Task A] [Task B] [Task C]  ← head에서 꺼냄
                              ↑
Thread 2: [idle] ──────────────┘  tail에서 훔쳐옴
```

1. 각 Thread는 자신의 **Deque(양방향 큐)** 보유
2. 작업을 **head에서 꺼내** 실행 (LIFO)
3. 유휴 Thread는 다른 Thread의 **tail에서 훔쳐옴** (FIFO)
4. **Contention 최소화**: 양쪽 끝에서 접근

#### 일반 ThreadPool과의 차이

| 항목 | ThreadPool | ForkJoinPool |
|-----|-----------|--------------|
| 작업 큐 | 공유 큐 (경쟁 높음) | 각 Thread별 Deque |
| 작업 분배 | 중앙에서 할당 | Work-stealing (분산) |
| 적합 작업 | 독립적 작업 | 재귀적 분할 작업 |
| 경쟁 수준 | 높음 | 낮음 (tail에서만 충돌) |

### RecursiveTask vs RecursiveAction

| 구분 | RecursiveTask<V> | RecursiveAction |
|-----|------------------|-----------------|
| 반환값 | 있음 (제네릭 V) | 없음 (void) |
| 사용 사례 | 배열 합산, 검색 | 배열 정렬, 업데이트 |
| 결과 조합 | join()으로 결과 획득 | 결과 없음 |

### RecursiveTask 예제: 배열 합산
```kotlin
class SumTask extends RecursiveTask<Long> {
    private static final int THRESHOLD = 1000;
    private final long[] array;
    private final int start, end;

    @Override
    protected Long compute() {
        int length = end - start;

        if (length <= THRESHOLD) {
            // 작은 작업: 직접 계산
            long sum = 0;
            for (int i = start; i < end; i++) {
                sum += array[i];
            }
            return sum
        }

        // 큰 작업: 분할
        int mid = start + length / 2;
        SumTask leftTask = new SumTask(array, start, mid)
        SumTask rightTask = new SumTask(array, mid, end)

        leftTask.fork(); // 비동기 실행 (다른 Thread에 할당)
        long rightResult = rightTask.compute(); // 현재 Thread에서 실행
        long leftResult = leftTask.join(); // 완료 대기

        return leftResult + rightResult
    }
}

// 사용
ForkJoinPool pool = new ForkJoinPool()
long sum = pool.invoke(new SumTask(array, 0, array.length))
```

### RecursiveAction 예제: 배열 증가
```kotlin
class IncrementAction extends RecursiveAction {
    private static final int THRESHOLD = 1000;
    private final int[] array;
    private final int start, end;

    @Override
    protected void compute() {
        if (end - start <= THRESHOLD) {
            for (int i = start; i < end; i++) {
                array[i]++;
            }
        } else {
            int mid = start + (end - start) / 2;
            invokeAll(
                new IncrementAction(array, start, mid),
                new IncrementAction(array, mid, end)
            ); // 두 작업 모두 fork + join
        }
    }
}
```

### Common Pool
```kotlin
// 1. 전역 Common Pool 사용 (권장)
ForkJoinPool.commonPool().invoke(task)

// 2. Parallel Stream (내부적으로 Common Pool 사용)
long sum = Arrays.stream(array).parallel().sum()

// 3. Common Pool 크기 확인
int parallelism = ForkJoinPool.commonPool().getParallelism()
// = Runtime.getRuntime().availableProcessors() - 1
```

**Common Pool 장점:**
- JVM 전체에서 공유 → 메모리 절약
- 자동 크기 조정 (CPU 코어 수 - 1)
- Parallel Stream과 동일한 Pool 사용

### fork() vs compute() vs invokeAll()

#### 메서드 비교

| 메서드 | 동작 | Thread 사용 | 반환 시점 |
|-------|------|------------|----------|
| fork() | 비동기 실행 스케줄링 | 다른 Thread | 즉시 (결과 X) |
| compute() | 동기 실행 | 현재 Thread | 작업 완료 후 |
| join() | 완료 대기 | 현재 Thread | 결과 반환 시 |
| invokeAll() | fork + join 일괄 | 여러 Thread | 모두 완료 후 |

#### 패턴별 사용

```kotlin
// 패턴 1: fork + compute + join (비대칭) ✅ 권장
left.fork();                         // left를 다른 Thread에 스케줄
long rightResult = right.compute(); // right를 현재 Thread에서 실행
long leftResult = left.join();      // left 완료 대기

// 패턴 2: invokeAll (대칭) ✅ 권장
invokeAll(left, right);              // 둘 다 fork + 모두 join 대기
long result = left.join() + right.join()

// 패턴 3: fork + fork + join + join (비효율) ❌
left.fork()
right.fork()
long leftResult = left.join();      // ❌ 잘못된 순서!
long rightResult = right.join()

// 패턴 4: 단순 compute (직렬) ❌ 병렬화 없음
long result = left.compute() + right.compute()
```

#### join() 순서의 중요성

```kotlin
// ❌ 비효율적: a를 먼저 join
a.fork()
b.fork()
int resultA = a.join(); // a 완료까지 대기 (b도 완료되었을 수 있음)
int resultB = b.join()

// ✅ 효율적: 나중에 fork한 것을 먼저 join (innermost-first)
a.fork()
b.fork()
int resultB = b.join(); // 나중 작업 먼저
int resultA = a.join(); // 먼저 fork한 작업은 이미 완료되었을 가능성 높음
```

#### 실무 권장 패턴

**1. 소규모 분할 (2개):**
```kotlin
// fork + compute + join 패턴
left.fork()
long rightResult = right.compute(); // 현재 Thread 활용
long leftResult = left.join()
return leftResult + rightResult
```
- 현재 Thread를 낭비하지 않음
- 가장 효율적

**2. 다수 분할 (3개 이상):**
```kotlin
// invokeAll 패턴
invokeAll(task1, task2, task3, task4)
return task1.join() + task2.join() + task3.join() + task4.join()
```
- 간결하고 안전
- join 순서 걱정 불필요

**3. 피해야 할 패턴:**
```kotlin
// ❌ 양쪽 모두 fork → 현재 Thread 낭비
left.fork()
right.fork()
return left.join() + right.join()

// ❌ 양쪽 모두 compute → 직렬 실행
return left.compute() + right.compute()
```

### THRESHOLD 설정

**너무 작으면:**
- 작업 분할 오버헤드 증가
- Context switching 비용

**너무 크면:**
- 병렬화 이점 감소
- Load balancing 악화

**권장 범위:**
- 100 ~ 10,000 (작업 특성에 따라 조정)
- 부하 테스트로 최적값 찾기

### 실무 사용 시점

**ForkJoinPool 사용:**
- 재귀적 분할 가능 (배열, 트리)
- CPU-bound 작업
- 작업 간 의존성 없음
- 대량 데이터 처리 (수십만 개 이상)

**일반 ThreadPool 사용:**
- 독립적인 작업들
- I/O-bound 작업
- 고정된 수의 작업
- Blocking 호출 많음

### 주의사항

#### 1. Blocking 작업 금지
```kotlin
// 안티패턴
protected Long compute() {
    Thread.sleep(1000); // ForkJoinPool에서 blocking 금지!
    return result
}
```
- Work-stealing 알고리즘 무력화
- Thread 고갈 위험

#### 2. Common Pool 오염 방지
```kotlin
// 안티패턴: Blocking 작업을 Common Pool에서 실행
ForkJoinPool.commonPool().submit { 
    blockingDatabaseCall(); // 다른 Parallel Stream에 영향!
}

// 해결책: 별도 Pool 사용
ForkJoinPool customPool = new ForkJoinPool(10)
```

#### 3. 예외 처리
```kotlin
try {
    result = pool.invoke(task)
} catch (Exception e) {
    // compute()에서 발생한 예외는 여기서 catch
}
```

## 주요 개념 정리

### Thread Pool 크기 결정

**CPU-bound 작업:**
```
Nthreads = Ncpu + 1
```
- CPU 코어 수만큼 설정
- +1은 페이지 폴트 등 대비

**I/O-bound 작업:**
```
Nthreads = Ncpu × (1 + W/C)
```
- W/C: Wait time / Compute time 비율
- I/O 대기가 많을수록 Thread 수 증가

**실무 접근:**
- 공식으로 초기값 설정
- 부하 테스트로 최적화
- 모니터링으로 지속적 튜닝

### ExecutorService Shutdown의 중요성

**shutdown하지 않으면 발생하는 문제:**
- Non-daemon thread가 계속 살아있어 **JVM 종료 안 됨**
- 메모리 누수 가능성
- 리소스 해제 안 됨

**권장 패턴:**
```kotlin
try {
    executor.execute(task)
} finally {
    executor.shutdown()
}
```

### RejectedExecutionHandler 정책 비교

| 정책 | 동작 | 장점 | 사용 시점 |
|-----|-----|-----|---------|
| AbortPolicy | 예외 발생 | 빠른 실패, 작업 손실 방지 | 기본 선택 |
| CallerRunsPolicy | 호출자가 실행 | Back pressure, 작업 손실 없음 | 부하 조절 필요 시 |
| DiscardPolicy | 조용히 버림 | 예외 없음 | 작업 손실 허용 시 |
| DiscardOldestPolicy | 오래된 작업 제거 | 최신 작업 우선 | 최신 데이터 중요 시 |