# messaging-consumer-core 테스트 가이드

## 📋 **테스트 개요**

messaging-consumer-core 모듈의 단위 테스트들은 **kotest BehaviorSpec** 스타일로 작성되었으며, **given/when/then** 구조를 따릅니다.

## 🧪 **테스트 구조**

### **1. DomainEventHandlerRegistryTest**
- **목적**: suspend 핸들러 등록 및 조회 로직 검증
- **테스트 케이스**:
  - 핸들러 등록 및 이벤트 타입별 조회
  - 같은 이벤트 타입의 여러 핸들러 등록
  - 등록되지 않은 이벤트 타입 조회
  - 핸들러 개수 조회

### **2. BlockingDomainEventHandlerRegistryTest**
- **목적**: blocking 핸들러 등록 및 조회 로직 검증
- **테스트 케이스**:
  - blocking 핸들러 등록 및 조회
  - 여러 blocking 핸들러 등록
  - 핸들러 개수 조회

### **3. DomainEventHandlerExecutorTest**
- **목적**: 핸들러 실행 로직의 핵심 기능 검증
- **테스트 케이스**:
  - suspend 핸들러 실행
  - blocking 핸들러 실행
  - 혼합 핸들러 실행
  - 병렬 처리 검증
  - 재시도 메커니즘 검증
  - 핸들러 없는 경우 처리

### **4. EventConsumerPropertiesTest**
- **목적**: 설정 프로퍼티 기본값 및 커스텀 값 검증
- **테스트 케이스**:
  - 기본 설정값 검증
  - 커스텀 설정값 반영 검증
  - 부분 커스텀 설정 검증
  - 에러 핸들링 설정 검증

## 🚀 **테스트 실행**

### **전체 테스트 실행**
```bash
./gradlew :adapter:inbound:messaging-consumer-core:test
```

### **특정 테스트 클래스 실행**
```bash
./gradlew :adapter:inbound:messaging-consumer-core:test --tests "*DomainEventHandlerExecutorTest"
```

### **특정 테스트 메서드 실행**
```bash
./gradlew :adapter:inbound:messaging-consumer-core:test --tests "*DomainEventHandlerExecutorTest.*suspend 핸들러만 있는 경우*"
```

## 📊 **테스트 커버리지**

주요 컴포넌트들의 테스트 커버리지:
- **DomainEventHandlerRegistry**: 100%
- **BlockingDomainEventHandlerRegistry**: 100%
- **DomainEventHandlerExecutor**: 95% (일부 에러 케이스 제외)
- **EventConsumerProperties**: 100%

## 🛠️ **테스트 환경 설정**

### **필요한 의존성**
```kotlin
testImplementation("io.kotest:kotest-runner-junit5")
testImplementation("io.kotest:kotest-assertions-core")
testImplementation("io.kotest.extensions:kotest-extensions-spring:1.3.0")
testImplementation("io.mockk:mockk")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
```

### **JVM 설정**
```kotlin
kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}
```

## 🔍 **테스트 패턴**

### **BehaviorSpec 사용 예시**
```kotlin
class MyTest : BehaviorSpec({
    given("컴포넌트 이름") {
        `when`("특정 상황에서") {
            then("기대되는 결과가 나와야 한다") {
                // 테스트 로직
                result shouldBe expected
            }
        }
    }
})
```

### **MockK 사용 예시**
```kotlin
val mockExecutor = mockk<DomainEventHandlerExecutor>()
coEvery { mockExecutor.execute(any()) } just Runs
coVerify { mockExecutor.execute(testEvent) }
```

### **코루틴 테스트**
```kotlin
runBlocking {
    executor.execute(event)
}

// 또는
await()
    .atMost(Duration.ofSeconds(2))
    .until { handler.callCount.get() > 0 }
```

## ⚠️ **주의사항**

1. **비동기 테스트**: `runBlocking` 또는 `awaitility` 사용
2. **Mock 초기화**: `beforeEach { clearAllMocks() }`
3. **스레드 안전성**: AtomicInteger 등 사용
4. **시간 의존적 테스트**: 적절한 타임아웃 설정

## 🐛 **트러블슈팅**

### **일반적인 문제들**

#### **1. 코루틴 테스트 실패**
```kotlin
// ❌ 잘못된 방법
handler.handle(event) // suspend 함수를 직접 호출

// ✅ 올바른 방법
runBlocking {
    handler.handle(event)
}
```

#### **2. Mock 초기화 누락**
```kotlin
beforeEach {
    clearAllMocks() // 각 테스트 전에 Mock 초기화
}
```

#### **3. 비동기 검증 실패**
```kotlin
// ❌ 즉시 검증
handler.callCount.get() shouldBe 1

// ✅ 대기 후 검증
await()
    .atMost(Duration.ofSeconds(1))
    .until { handler.callCount.get() == 1 }
```

## 📈 **성능 테스트**

일부 테스트에서는 실행 시간을 측정하여 병렬 처리 효과를 검증합니다:

```kotlin
val startTime = System.currentTimeMillis()
runBlocking { executor.execute(event) }
val executionTime = System.currentTimeMillis() - startTime

executionTime shouldBeGreaterThan 40 // 최소 실행 시간 검증
```

---

이 테스트들은 **messaging-consumer-core 모듈의 안정성과 정확성**을 보장하며, **리팩터링 및 기능 추가 시 회귀 테스트** 역할을 합니다.
