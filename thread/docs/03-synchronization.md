# 동기화 메커니즘

## Mutex (Mutual Exclusion)

### 개념
한 번에 하나의 Thread만 임계 영역 진입을 보장하는 락 기반 메커니즘

### Kotlin 구현: ReentrantLock
```kotlin
private val lock = ReentrantLock()

fun criticalSection() {
    lock.lock()
    try {
        // critical section
    } finally {
        lock.unlock() // 반드시 finally에서 해제
    }
}
```

### 주요 특징
- **Reentrant**: 같은 Thread가 재진입 가능 (카운터 증가)
- **소유권**: Lock을 획득한 Thread만 해제 가능
- **이진 상태**: locked / unlocked

### tryLock으로 Deadlock 방지
```kotlin
if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {
    try {
        // work
    } finally {
        lock.unlock()
    }
} else {
    // timeout handling
}
```

## Semaphore

### 개념
카운터 기반으로 동시 접근 Thread 수를 제한하는 신호 메커니즘

### 사용 사례: Connection Pool
```kotlin
private val semaphore = Semaphore(10) // 최대 10개 동시 접근

fun useConnection() {
    semaphore.acquire() // 허가증 획득 (카운터 감소)
    try {
        // use connection
    } finally {
        semaphore.release() // 허가증 반환 (카운터 증가)
    }
}
```

### Mutex와의 차이

| 항목 | Mutex | Semaphore |
|-----|-------|-----------|
| 목적 | 상호 배제 | 접근 제한 제어 |
| 동시 접근 | 1개 Thread | N개 Thread |
| 소유권 | 필요 (획득한 Thread만 해제) | 불필요 (아무 Thread나 release) |
| 사용 사례 | 단일 리소스 보호 | 리소스 풀 관리 |

### Binary Semaphore vs Counting Semaphore
- **Binary Semaphore**: 카운터 0 또는 1 (Mutex와 유사하지만 소유권 없음)
- **Counting Semaphore**: 카운터 N개

## Monitor

### 개념
Mutex + Condition Variable을 결합한 고수준 동기화 메커니즘

### Kotlin 구현: synchronized
```kotlin
private val monitor = Object()

fun waitingThread() {
    synchronized(monitor) {
        while (!condition) {
            monitor.wait() // lock 해제 후 대기
        }
        // work
    }
}

fun notifyingThread() {
    synchronized(monitor) {
        condition = true
        monitor.notify() // 대기 중인 Thread 하나
    }
}
```

### 모든 Java 객체는 Monitor
- `synchronized` 블록/메서드는 객체의 내재 락(intrinsic lock) 사용
- `wait()`, `notify()`, `notifyAll()`는 synchronized 블록 내에서만 호출 가능

### wait vs sleep

| 항목 | wait() | sleep() |
|-----|--------|---------|
| 클래스 | Object | Thread |
| Lock 해제 | O | X |
| 호출 조건 | synchronized 블록 내 | 어디서나 |
| 깨우는 방법 | notify() | 시간 경과 |

## synchronized vs ReentrantLock

### 성능 비교
- **Java 6 이후**: JVM 최적화로 대부분 상황에서 성능 차이 미미
  - Biased locking
  - Lock coarsening
  - Adaptive locking
- **High contention**: ReentrantLock이 약간 유리할 수 있음

### synchronized 장점
```kotlin
@Synchronized
fun method() {
    // 간결한 문법
    // 자동으로 lock 해제 (예외 발생 시에도)
}
```
- 문법 간결
- 자동 lock 해제 (finally 불필요)
- JVM 레벨 최적화

### ReentrantLock 장점
```kotlin
val lock = ReentrantLock()

// 1. Timeout 지원
if (lock.tryLock(1, TimeUnit.SECONDS)) { }

// 2. Lock Polling (SpinLock 패턴)
// 짧은 대기 시간 예상 시, blocking 대신 busy-wait으로 성능 향상
while (!lock.tryLock()) {
    // 다른 작업 수행 가능
    // Thread.onSpinWait() // Java 9+: CPU에게 힌트 제공
}
try {
    // critical section
} finally {
    lock.unlock()
}

// 3. Interruptible
lock.lockInterruptibly() // 대기 중 interrupt 가능

// 4. Fair lock
val fairLock = ReentrantLock(true)

// 5. Condition 여러 개 사용
val notFull = lock.newCondition()
val notEmpty = lock.newCondition()

// 6. 락 상태 조회
if (lock.isLocked) { }
```

### tryLock() SpinLock 패턴 사용 시점

**유리한 경우:**
- 임계 영역이 매우 짧음 (수십 나노초)
- Lock 경쟁이 낮음
- Context switching 비용이 큼

**불리한 경우:**
- 임계 영역이 긺
- Lock 경쟁이 높음 (CPU 낭비)
- 다른 작업 처리 필요

### 실무 가이드
- 기본은 **synchronized** 사용 (간결하고 안전)
- 다음이 필요하면 **ReentrantLock** 사용:
  - Timeout
  - Lock Polling (SpinLock) - 짧은 임계 영역
  - Interruptible wait
  - Fair lock
  - 여러 Condition
  - 락 상태 조회

## Fair Lock vs Unfair Lock

### Unfair Lock (기본)
```kotlin
val lock = ReentrantLock() // unfair by default
```
- Lock 해제 시, 대기 큐 순서 무시하고 경쟁
- **성능 우수** (context switching 적음)
- Starvation 가능성

### Fair Lock
```kotlin
val lock = ReentrantLock(true)
```
- 대기 큐 순서대로 Lock 획득 (FIFO)
- **공정성 보장**
- 성능 저하 (context switching 많음)

### 실무 선택 기준
- **기본은 Unfair Lock** (성능 우선)
- Lock 요청 간격이 길거나 공정성이 중요하면 Fair Lock

## ReadWriteLock

### 사용 사례: 읽기 많고 쓰기 적은 캐시
```kotlin
private val rwLock = ReentrantReadWriteLock()
private val readLock = rwLock.readLock()
private val writeLock = rwLock.writeLock()
private val cache = HashMap<String, String>()

fun get(key: String): String? {
    readLock.lock()
    try {
        return cache[key] // 여러 Thread 동시 읽기 가능
    } finally {
        readLock.unlock()
    }
}

fun put(key: String, value: String) {
    writeLock.lock()
    try {
        cache[key] = value // 쓰기는 배타적
    } finally {
        writeLock.unlock()
    }
}
```

### 특징
- **Read lock**: 여러 Thread 동시 획득 가능
- **Write lock**: 배타적 (다른 read/write lock 모두 차단)
- Read:Write 비율이 높을 때 유리

## StampedLock

### 개념
Java 8에서 도입된 ReadWriteLock의 성능 개선 버전으로, **Optimistic Read** 모드를 지원하는 고성능 락

### 세 가지 모드

#### 1. Write Lock (배타적)
```kotlin
val stamp = lock.writeLock()
try {
    // 쓰기 작업
} finally {
    lock.unlockWrite(stamp)
}
```

#### 2. Read Lock (공유)
```kotlin
val stamp = lock.readLock()
try {
    // 읽기 작업
} finally {
    lock.unlockRead(stamp)
}
```

#### 3. Optimistic Read (핵심 기능)
```kotlin
var stamp = lock.tryOptimisticRead() // lock 획득 안 함
var x = sharedData.x // 데이터 읽기
if (!lock.validate(stamp)) { // 쓰기가 발생했는지 검증
    // 쓰기 발생 → Read Lock으로 재시도
    stamp = lock.readLock()
    try {
        x = sharedData.x
    } finally {
        lock.unlockRead(stamp)
    }
}
return x
```

### 사용 사례: 고성능 좌표 추적
```kotlin
private val lock = StampedLock()
private var x: Double = 0.0
private var y: Double = 0.0

fun move(deltaX: Double, deltaY: Double) {
    val stamp = lock.writeLock()
    try {
        x += deltaX
        y += deltaY
    } finally {
        lock.unlockWrite(stamp)
    }
}

fun distanceFromOrigin(): Double {
    var stamp = lock.tryOptimisticRead() // lock 없이 읽기 시도
    var currentX = x
    var currentY = y

    if (!lock.validate(stamp)) { // 쓰기 발생 여부 확인
        stamp = lock.readLock() // 실패 시 Read Lock 획득
        try {
            currentX = x
            currentY = y
        } finally {
            lock.unlockRead(stamp)
        }
    }

    return Math.sqrt(currentX * currentX + currentY * currentY)
}
```

### Optimistic Read 동작 원리

1. `tryOptimisticRead()`: stamp만 반환, **lock 획득 안 함**
2. 데이터 읽기 (다른 Thread가 쓰기 가능)
3. `validate(stamp)`: 읽는 동안 쓰기 발생 여부 확인
   - true: 데이터 일관성 보장
   - false: 쓰기 발생 → Read Lock으로 재시도

### ReadWriteLock vs StampedLock

| 항목 | ReadWriteLock | StampedLock |
|-----|---------------|-------------|
| Optimistic Read | X | O (핵심 기능) |
| 성능 | 기본 | ReadWriteLock 대비 50% 빠름 (read-heavy) |
| Reentrant | O | X (재진입 불가) |
| Condition | O | X |
| 복잡도 | 낮음 | 높음 (stamp 관리) |
| 사용 사례 | 일반적인 read/write | 읽기 >> 쓰기, 고성능 필요 |

### 주의사항

#### 1. Reentrant 미지원
```kotlin
val stamp = lock.readLock()
try {
    val stamp2 = lock.readLock() // Deadlock 발생!
} finally {
    lock.unlockRead(stamp)
}
```

#### 2. validate() 필수
```kotlin
// 잘못된 예
val stamp = lock.tryOptimisticRead()
return data // validate 없음 → 데이터 불일치 가능

// 올바른 예
val stamp = lock.tryOptimisticRead()
val snapshot = data
if (!lock.validate(stamp)) {
    // 재시도 로직
}
return snapshot
```

#### 3. Lock 변환 복잡성
```kotlin
// 가능하지만 복잡함
var stamp = lock.readLock()
val ws = lock.tryConvertToWriteLock(stamp) // 쓰기로 업그레이드
if (ws != 0L) {
    stamp = ws
    // 쓰기 작업
} else {
    lock.unlockRead(stamp)
    stamp = lock.writeLock()
}
```

### 실무 선택 기준

**StampedLock 사용:**
- 읽기가 쓰기보다 압도적으로 많음 (90% 이상)
- 쓰기 충돌이 드묾
- 최대 성능이 중요
- Reentrant 불필요

**ReadWriteLock 사용:**
- 일반적인 read/write 비율
- 코드 간결성 중요
- Condition 필요
- Reentrant 필요

## 주요 개념 정리

### Mutex vs Semaphore

| 구분 | Mutex | Semaphore |
|-----|-------|-----------|
| 기반 | Lock | Signal |
| 동시 접근 | 1개 Thread | N개 Thread |
| 소유권 | 있음 (획득한 Thread만 해제) | 없음 (아무 Thread나 release) |
| 목적 | 상호 배제 (Mutual Exclusion) | 접근 제어 (Access Control) |
| 사용 사례 | 단일 리소스 보호 | 리소스 풀 관리 |

### synchronized 블록에서 wait() 동작 흐름

1. 현재 Thread가 WAITING 상태로 전환
2. 가지고 있던 monitor lock 해제
3. notify() 또는 notifyAll() 호출 시까지 대기
4. 깨어나면 lock 재획득 후 진행

### ReentrantLock 사용 시점

**다음 기능이 필요한 경우 ReentrantLock 사용:**
- tryLock으로 타임아웃 지정
- lockInterruptibly()로 대기 중 interrupt 가능
- Fair lock으로 공정성 보장
- 여러 개의 Condition 사용
- 락 상태 조회 (isLocked 등)

**그 외에는 synchronized 사용 권장** (간결하고 안전)
