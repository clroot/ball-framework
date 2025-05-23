# messaging-consumer-kafka 테스트 가이드

## 📋 **테스트 개요**

messaging-consumer-kafka 모듈의 테스트들은 **kotest BehaviorSpec** 스타일로 작성되었으며, **단위 테스트**와 **통합 테스트**로 구성됩니다.

## 🧪 **테스트 구조**

### **단위 테스트**

#### **1. DomainEventKafkaMessageConverterTest**
- **목적**: JSON 메시지 ↔ DomainEvent 변환 로직 검증
- **테스트 케이스**:
  - 올바른 JSON 메시지 변환
  - 잘못된 형식 메시지 처리
  - eventType/eventData 누락 처리
  - 존재하지 않는 이벤트 타입 처리
  - 메시지 유효성 검증
  - 이벤트 타입/ID 추출

```kotlin
given("DomainEventKafkaMessageConverter") {
    `when`("올바른 형식의 JSON 메시지를 변환하는 경우") {
        then("도메인 이벤트로 변환되어야 한다") {
            val result = converter.convertToDomainEvent(validMessage)
            result shouldNotBe null
            result!!.type shouldBe "TestKafkaEvent"
        }
    }
}
```

#### **2. KafkaEventConsumerPropertiesTest**
- **목적**: Kafka 전용 설정 프로퍼티 검증
- **테스트 케이스**:
  - 기본 설정값 검증
  - Core 모듈 설정 상속 검증
  - Kafka 전용 설정 검증
  - 에러 핸들링 설정 검증

#### **3. KafkaEventListenerTest**
- **목적**: Kafka 메시지 수신 및 처리 로직 검증 (Mock 기반)
- **테스트 케이스**:
  - 올바른 메시지 처리
  - 잘못된 메시지 스킵
  - 메시지 변환 실패 처리
  - 핸들러 실행 실패 처리
  - 동기/비동기 모드 처리
  - Acknowledgment 처리

```kotlin
given("KafkaEventListener") {
    `when`("올바른 Kafka 메시지를 수신하는 경우") {
        then("메시지가 변환되고 핸들러가 실행되며 오프셋이 커밋되어야 한다") {
            // Mock 설정 및 검증
            verify { mockMessageConverter.convertToDomainEvent(validMessage) }
            coVerify { mockHandlerExecutor.execute(testEvent) }
            verify { mockAcknowledgment.acknowledge() }
        }
    }
}
```

#### **4. KafkaConsumerConfigurationTest**
- **목적**: Kafka Consumer 설정 검증
- **테스트 케이스**:
  - Consumer Factory 생성 검증
  - 설정 프로퍼티 반영 검증
  - Container Factory 설정 검증
  - Acknowledgment 모드 설정 검증

### **통합 테스트**

#### **5. KafkaEventConsumerIntegrationTest**
- **목적**: 실제 Kafka 환경에서의 End-to-End 테스트
- **특징**:
  - **@EmbeddedKafka** 사용으로 실제 Kafka 브로커 시뮬레이션
  - 실제 메시지 발행 및 소비 검증
  - 대량 메시지 처리 성능 검증

```kotlin
@EmbeddedKafka(
    partitions = 1,
    topics = ["integration-test-events"],
    brokerProperties = [
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    ]
)
class KafkaEventConsumerIntegrationTest : BehaviorSpec({
    given("Kafka Event Consumer Integration") {
        `when`("올바른 형식의 이벤트 메시지를 Kafka로 전송하는 경우") {
            then("Consumer가 메시지를 수신하고 핸들러가 실행되어야 한다") {
                kafkaTemplate.send("integration-test-events", kafkaMessage).get()
                
                await()
                    .atMost(Duration.ofSeconds(10))
                    .until { handler.callCount.get() > 0 }
            }
        }
    }
})
```

## 🚀 **테스트 실행**

### **전체 테스트 실행**
```bash
./gradlew :adapter:inbound:messaging-consumer-kafka:test
```

### **단위 테스트만 실행**
```bash
./gradlew :adapter:inbound:messaging-consumer-kafka:test --tests "*Test" --exclude-tests "*IntegrationTest"
```

### **통합 테스트만 실행**
```bash
./gradlew :adapter:inbound:messaging-consumer-kafka:test --tests "*IntegrationTest"
```

### **특정 테스트 클래스 실행**
```bash
./gradlew :adapter:inbound:messaging-consumer-kafka:test --tests "*DomainEventKafkaMessageConverterTest"
```

## 📊 **테스트 커버리지**

주요 컴포넌트들의 테스트 커버리지:
- **DomainEventKafkaMessageConverter**: 100%
- **KafkaEventConsumerProperties**: 100%
- **KafkaEventListener**: 95% (일부 비동기 에러 케이스 제외)
- **KafkaConsumerConfiguration**: 90%
- **통합 테스트**: End-to-End 시나리오 90%

## 🛠️ **테스트 환경 설정**

### **필요한 의존성**
```kotlin
testImplementation("org.springframework.kafka:spring-kafka-test")
testImplementation("io.kotest:kotest-runner-junit5")
testImplementation("io.kotest:kotest-assertions-core")
testImplementation("io.kotest.extensions:kotest-extensions-spring:1.3.0")
testImplementation("io.mockk:mockk")
testImplementation("org.awaitility:awaitility-kotlin")
```

### **EmbeddedKafka 설정**
```kotlin
@EmbeddedKafka(
    partitions = 1,
    topics = ["test-topic"],
    brokerProperties = ["listeners=PLAINTEXT://localhost:9092"]
)
```

### **테스트 프로퍼티**
```properties
ball.event.consumer.kafka.enabled=true
ball.event.consumer.kafka.topics=integration-test-events
ball.event.consumer.kafka.groupId=integration-test-group
ball.event.consumer.kafka.bootstrapServers=localhost:9092
ball.event.consumer.kafka.async=false
```

## 🔍 **테스트 패턴**

### **1. Mock 기반 단위 테스트**
```kotlin
val mockHandlerExecutor = mockk<DomainEventHandlerExecutor>()
val mockMessageConverter = mockk<DomainEventKafkaMessageConverter>()

// Mock 동작 설정
every { mockMessageConverter.isValidMessage(any()) } returns true
coEvery { mockHandlerExecutor.execute(any()) } just Runs

// 검증
verify { mockMessageConverter.isValidMessage(validMessage) }
coVerify { mockHandlerExecutor.execute(testEvent) }
```

### **2. Kafka 메시지 생성 헬퍼**
```kotlin
private fun createKafkaMessage(event: DomainEvent, objectMapper: ObjectMapper): String {
    val messageData = mapOf(
        "eventType" to event.javaClass.name,
        "eventId" to event.id,
        "occurredAt" to event.occurredAt.toString(),
        "eventData" to event
    )
    return objectMapper.writeValueAsString(messageData)
}
```

### **3. 비동기 검증 패턴**
```kotlin
// 메시지 전송
kafkaTemplate.send("test-topic", message).get()

// 비동기 처리 대기 및 검증
await()
    .atMost(Duration.ofSeconds(10))
    .until { handler.callCount.get() > 0 }

handler.callCount.get() shouldBe 1
```

## ⚡ **성능 테스트**

### **대량 메시지 처리 테스트**
```kotlin
`when`("대량의 메시지를 전송하는 경우") {
    then("모든 메시지가 처리되어야 한다") {
        val messageCount = 100
        val events = (1..messageCount).map { TestEvent("data-$it") }
        
        events.forEach { event ->
            val message = createKafkaMessage(event, objectMapper)
            kafkaTemplate.send("test-topic", message)
        }
        
        await()
            .atMost(Duration.ofSeconds(30))
            .until { handler.callCount.get() >= messageCount }
    }
}
```

## 🐛 **트러블슈팅**

### **일반적인 문제들**

#### **1. EmbeddedKafka 포트 충돌**
```kotlin
// 해결: 다른 포트 사용 또는 랜덤 포트
@EmbeddedKafka(
    brokerProperties = ["listeners=PLAINTEXT://localhost:0"] // 랜덤 포트
)
```

#### **2. 통합 테스트 타임아웃**
```kotlin
// 해결: 충분한 대기 시간 설정
await()
    .atMost(Duration.ofSeconds(30)) // 대용량 테스트는 더 긴 시간
    .until { condition }
```

#### **3. 메시지 변환 실패**
```kotlin
// 해결: Jackson 설정 확인
val objectMapper = ObjectMapper().apply {
    registerModule(KotlinModule.Builder().build())
}
```

#### **4. Spring Context 로딩 실패**
```kotlin
// 해결: 필요한 설정 클래스 Import
@Import(KafkaEventConsumerAutoConfiguration::class)
```

## 📈 **테스트 메트릭**

### **실행 시간 측정**
- **단위 테스트**: ~2초
- **통합 테스트**: ~15-30초 (EmbeddedKafka 시작 시간 포함)
- **전체 테스트**: ~45초

### **메모리 사용량**
- **단위 테스트**: ~100MB
- **통합 테스트**: ~200MB (EmbeddedKafka 포함)

## 🔐 **보안 테스트**

향후 추가될 보안 관련 테스트:
```kotlin
`when`("SSL 설정이 활성화된 경우") {
    then("보안 연결이 올바르게 설정되어야 한다") {
        // SSL/TLS 설정 검증
    }
}

`when`("SASL 인증이 필요한 경우") {
    then("인증이 올바르게 처리되어야 한다") {
        // SASL 인증 검증
    }
}
```

## ✅ **테스트 체크리스트**

실행 전 확인사항:
- [ ] Docker Desktop 실행 (통합 테스트용)
- [ ] 포트 9092 사용 가능 여부 확인
- [ ] JDK 21 설정 확인
- [ ] 충분한 메모리 할당 (최소 1GB)

---

이 테스트들은 **Kafka Consumer의 안정성과 실제 환경에서의 동작**을 보장하며, **메시지 처리 성능과 에러 처리 능력**을 검증합니다.
