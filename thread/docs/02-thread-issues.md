# Thread 동시성 문제

## Race Condition (경쟁 조건)

### 정의
여러 Thread가 공유 자원에 동시 접근할 때, 실행 순서에 따라 결과가 달라지는 상황

### 전형적인 예시
```kotlin
class Counter {
    private var count = 0

    fun increment() {
        count++ // 3단계: read → modify → write
    }
}
```
- Thread A: read(0) → interrupted
- Thread B: read(0) → modify(1) → write(1)
- Thread A: modify(1) → write(1) ← 예상은 2

## Critical Section (임계 영역)

### 정의
공유 자원에 접근하는 코드 영역. 한 번에 하나의 Thread만 실행해야 함

### 해결 요구사항
1. **상호 배제 (Mutual Exclusion)**: 한 Thread만 진입
2. **진행 (Progress)**: 진입 결정은 유한 시간 내
3. **한정 대기 (Bounded Waiting)**: 무한정 대기 방지

## Volatile과 가시성 문제

### 가시성 문제
```kotlin
class Task {
    private var running = true // CPU cache에 캐싱됨

    fun run() {
        while (running) { // 다른 Thread의 변경을 못 볼 수 있음
            // work
        }
    }

    fun stop() {
        running = false
    }
}
```

### Volatile 키워드
```kotlin
@Volatile
private var running = true
```

**효과:**
- 항상 main memory에서 읽고 씀 (CPU cache 우회)
- 가시성 보장

**주의:**
- **원자성은 보장 안 함** (count++ 같은 복합 연산은 불안전)
- 단순 플래그, 상태 체크용으로만 사용

## Happens-Before 관계

### 정의
한 작업의 결과가 다른 작업에서 보이는 것을 보장하는 순서 관계

### 주요 규칙
1. **Program Order**: 단일 Thread 내에서 코드 순서대로 실행
2. **Volatile Write-Read**: volatile 변수 write는 이후 read보다 먼저 발생
3. **Monitor Lock**: unlock은 이후 lock보다 먼저 발생
4. **Thread Start**: Thread.start()는 해당 Thread의 모든 작업보다 먼저 발생
5. **Thread Join**: Thread의 모든 작업은 join() 완료보다 먼저 발생

### 실무 예시
```kotlin
// Thread 1
x = 1              // (1)
@Volatile
flag = true        // (2)

// Thread 2
if (flag) {        // (3)
    print(x)       // (4) - 항상 1 출력 보장
}
```
(2) happens-before (3) → (1) happens-before (4)

## Deadlock (교착 상태)

### 발생 4가지 조건 (모두 만족 시 발생)
1. **상호 배제 (Mutual Exclusion)**: 자원은 한 번에 하나의 Thread만
2. **점유와 대기 (Hold and Wait)**: 자원을 가진 채 다른 자원 대기
3. **비선점 (No Preemption)**: 자원을 강제로 뺏을 수 없음
4. **순환 대기 (Circular Wait)**: Thread 간 자원 대기가 순환 구조

### 전형적인 예시
```java
Thread A: lock(R1) → lock(R2)
Thread B: lock(R2) → lock(R1)
```

### 예방 방법

#### 1. Lock 순서 통일 (Circular Wait 방지)
```kotlin
// 모든 Thread에서 동일한 순서로 락 획득
synchronized(lockA) {
    synchronized(lockB) {
        // work
    }
}
```

#### 2. Lock Timeout (Hold and Wait 제한)
```kotlin
if (lock.tryLock(1, TimeUnit.SECONDS)) {
    try {
        // work
    } finally {
        lock.unlock()
    }
}
```

#### 3. 락 획득 실패 시 모두 해제
```kotlin
while (true) {
    if (lockA.tryLock()) {
        if (lockB.tryLock()) {
            try {
                // work
                break
            } finally {
                lockB.unlock()
            }
        }
        lockA.unlock() // lockB 실패 시 lockA도 해제
    }
}
```

### 탐지 방법
- Thread dump 분석 (`jstack`)
- JConsole, VisualVM으로 deadlock 탐지
- JMX를 통한 프로그램 내 탐지

```kotlin
val bean = ManagementFactory.getThreadMXBean()
val deadlockedThreads = bean.findDeadlockedThreads()
```

## 주요 개념 비교

### synchronized vs volatile

| 구분 | synchronized | volatile |
|-----|-------------|----------|
| 가시성 | 보장 | 보장 |
| 원자성 | 보장 (mutual exclusion) | 보장 안 함 |
| 사용 | Critical Section 보호 | 단순 플래그/상태 체크 |
| 성능 | 상대적으로 무겁다 | 가볍다 |

### volatile long vs AtomicLong

| 구분 | volatile long | AtomicLong |
|-----|--------------|------------|
| 읽기/쓰기 | 안전 | 안전 |
| 복합 연산 (increment 등) | 불안전 | 안전 (CAS 기반) |
| 사용 | 단순 값 공유 | 원자적 연산이 필요한 경우 |