# InMemory DomainEventPublisher (Auto Configuration)

단일 프로세스 내에서 도메인 이벤트를 발행하는 Publisher 구현체입니다.
**Auto Configuration**이 적용되어 의존성만 추가하면 자동으로 활성화됩니다.

## 🚀 **빠른 시작**

### 1. 의존성 추가

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.clroot.ball:messaging-producer-inmemory:2.0.0")
}
```

### 2. Application Service에서 사용

```kotlin
@Service
class UserService(
    private val userRepository: UserRepository,
    private val domainEventPublisher: DomainEventPublisher  // 자동 주입됨
) {
    fun createUser(command: CreateUserCommand): User {
        val user = User.create(command.email, command.name)
        userRepository.save(user)
        
        // 이벤트 발행 (InMemory로 처리됨)
        domainEventPublisher.publish(user.domainEvents)
        user.clearEvents()
        
        return user
    }
}
```

### 3. 완료! 🎉

더 이상 설정할 것이 없습니다. Auto Configuration이 모든 것을 자동으로 처리합니다.

## ⚙️ **Auto Configuration 특징**

### **자동 활성화 조건**
✅ `DomainEventPublisher` 클래스가 클래스패스에 존재  
✅ `ball.event.publisher.type=inmemory` 또는 다른 publisher가 없을 때 (기본값)  
✅ `DomainEventPublisher` Bean이 없을 때 자동 생성

### **자동 생성되는 Bean들**
- `InMemoryEventPublisher`: DomainEventPublisher 구현체
- `DefaultDomainEventDispatcher`: 기본 이벤트 디스패처 (필요시)

### **우선순위 기반 활성화**
다른 DomainEventPublisher 구현체(Kafka, Redis 등)가 있으면 자동으로 비활성화됩니다.

## 🔧 **설정 옵션**

### application.yml
```yaml
ball:
  event:
    publisher:
      type: inmemory              # Publisher 타입 명시 (기본값: inmemory)
      inmemory:
        async: true               # 비동기 발행 (기본값: true)
        enable-retry: false       # 재시도 활성화 (기본값: false)
        max-retry-attempts: 3     # 최대 재시도 횟수
        retry-delay-ms: 1000      # 재시도 간격
        timeout-ms: 0             # 발행 타임아웃 (0 = 무제한)
        enable-debug-logging: false  # 디버그 로깅
```

### **IDE 자동완성 지원**
Configuration properties metadata가 포함되어 있어 IDE에서 자동완성과 문서를 제공합니다.

## 📋 **사용 예시**

### **기본 사용법**
```kotlin
@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val domainEventPublisher: DomainEventPublisher  // 자동 주입
) {
    fun processOrder(command: ProcessOrderCommand): Order {
        val order = Order.create(command)
        orderRepository.save(order)
        
        // 이벤트 발행 (Consumer가 있으면 자동 처리됨)
        domainEventPublisher.publish(order.domainEvents)
        order.clearEvents()
        
        return order
    }
}
```

### **직접 이벤트 발행**
```kotlin
@Component
class SomeBusinessService(
    private val domainEventPublisher: DomainEventPublisher
) {
    fun doSomething() {
        // 직접 이벤트 생성 및 발행
        val event = SomethingHappenedEvent("data")
        domainEventPublisher.publish(event)
    }
}
```

### **여러 이벤트 발행**
```kotlin
// List<DomainEvent> 발행
domainEventPublisher.publish(listOf(event1, event2, event3))

// 또는 개별 발행
order.domainEvents.forEach { domainEventPublisher.publish(it) }
```

## 🎛️ **Publisher 타입 전환**

### **InMemory → Kafka 전환**
```yaml
# 개발 환경 (application-dev.yml)
ball:
  event:
    publisher:
      type: inmemory    # InMemory 사용

# 운영 환경 (application-prod.yml)  
ball:
  event:
    publisher:
      type: kafka       # Kafka 사용
```

코드 변경 없이 설정만으로 Publisher 전환 가능!

### **다중 Publisher 환경**
```kotlin
// 특정 상황에서만 다른 Publisher 사용
@Service
class CriticalEventService(
    @Qualifier("kafkaEventPublisher") 
    private val criticalEventPublisher: DomainEventPublisher,
    
    @Qualifier("inMemoryEventPublisher")
    private val normalEventPublisher: DomainEventPublisher
) {
    fun publishCriticalEvent(event: DomainEvent) {
        criticalEventPublisher.publish(event)  // Kafka로 발행
    }
    
    fun publishNormalEvent(event: DomainEvent) {
        normalEventPublisher.publish(event)    // InMemory로 발행
    }
}
```

## 🧪 **테스트**

### **통합 테스트**
```kotlin
@SpringBootTest
class EventPublishingTest {
    
    @Autowired
    private lateinit var domainEventPublisher: DomainEventPublisher
    
    @Test
    fun `should publish events automatically`() {
        // Given
        val event = TestDomainEvent("test-data")
        
        // When
        domainEventPublisher.publish(event)
        
        // Then
        // Consumer가 있으면 자동으로 처리됨
        // 없으면 ApplicationEvent로만 발행됨
    }
}
```

### **테스트 전용 설정**
```yaml
# application-test.yml
ball:
  event:
    publisher:
      inmemory:
        async: false              # 테스트에서는 동기 처리
        enable-debug-logging: true
```

### **Mock Publisher 사용**
```kotlin
@TestConfiguration
class TestEventConfiguration {
    
    @Bean
    @Primary
    fun mockEventPublisher(): DomainEventPublisher = mockk<DomainEventPublisher>()
}
```

## 🔧 **고급 설정**

### **Auto Configuration 비활성화**
```yaml
ball:
  event:
    publisher:
      type: none  # InMemory Publisher 비활성화
```

### **Custom Publisher 구현체 사용**
```kotlin
@Component
@Primary
class CustomDomainEventPublisher : DomainEventPublisher {
    override fun publish(event: DomainEvent) {
        // 커스텀 구현
    }
}
// Auto Configuration의 InMemoryEventPublisher는 자동으로 비활성화됨
```

### **Conditional Bean 등록**
```kotlin
@Component
@ConditionalOnProperty("feature.advanced-events.enabled", havingValue = "true")
class AdvancedEventPublisher : DomainEventPublisher {
    // 조건부로만 활성화되는 Publisher
}
```

## 🔄 **Consumer와 연동**

### **Producer + Consumer 조합**
```kotlin
// 1. Producer 의존성
implementation("io.clroot.ball:messaging-producer-inmemory")

// 2. Consumer 의존성  
implementation("io.clroot.ball:messaging-consumer-inmemory")

// 3. Handler 구현
@Component
class MyEventHandler : DomainEventHandler<MyEvent> {
    override suspend fun handle(event: MyEvent) {
        // Publisher가 발행한 이벤트를 자동으로 수신하여 처리
    }
}

// 4. Application Service에서 발행
@Service
class MyService(private val publisher: DomainEventPublisher) {
    fun doSomething() {
        publisher.publish(MyEvent("data"))  // → MyEventHandler 자동 호출
    }
}
```

### **완전한 InMemory Event Bus**
```yaml
ball:
  event:
    publisher:
      type: inmemory
      inmemory:
        async: true
    consumer:
      inmemory:
        enabled: true
        async: true
        parallel: true
```

## 🚀 **장점**

### **Zero Configuration**
- ✅ 의존성 추가만으로 즉시 동작
- ✅ DomainEventPublisher 인터페이스 자동 구현
- ✅ 복잡한 설정 불필요

### **Spring Boot Native**
- ✅ Spring Boot Auto Configuration 표준 준수
- ✅ IDE 자동완성 지원
- ✅ Configuration Properties 메타데이터 제공

### **유연한 확장**
- ✅ 설정만으로 다른 Publisher로 전환 가능
- ✅ 커스텀 구현체로 쉽게 교체 가능
- ✅ 다중 Publisher 환경 지원

## 🔧 **트러블슈팅**

### **Publisher가 자동 등록되지 않는 경우**
1. Auto Configuration 활성화 여부 확인:
   ```bash
   # 활성화된 Auto Configuration 확인
   --debug 옵션으로 애플리케이션 실행
   ```
2. 다른 DomainEventPublisher Bean 존재 여부 확인
3. `ball.event.publisher.type` 설정 확인

### **이벤트가 발행되지 않는 경우**
1. DomainEventPublisher 주입 확인
2. 이벤트 객체가 DomainEvent 인터페이스 구현했는지 확인
3. 로그 레벨을 DEBUG로 설정하여 발행 과정 확인

### **성능 최적화**
```yaml
ball:
  event:
    publisher:
      inmemory:
        async: true               # 비동기 발행으로 성능 향상
        enable-debug-logging: false  # 운영에서는 디버그 로깅 비활성화
```

## 📊 **모니터링**

Auto Configuration은 다음 정보를 로그로 제공합니다:
- 발행된 이벤트 정보
- 발행 실패 이벤트
- 재시도 현황 (활성화 시)

```yaml
logging:
  level:
    io.clroot.ball.adapter.outbound.messaging.producer.inmemory: DEBUG
```

## 🌐 **다른 Publisher와 비교**

| 특징 | InMemory | Kafka | Redis |
|------|----------|--------|--------|
| **설정 복잡도** | ⭐ (매우 간단) | ⭐⭐⭐ | ⭐⭐ |
| **외부 의존성** | 없음 | Kafka 필요 | Redis 필요 |
| **확장성** | 단일 프로세스 | 분산 시스템 | 중간 규모 |
| **개발 편의성** | 최고 | 중간 | 중간 |
| **운영 복잡도** | 최소 | 높음 | 중간 |

**권장 사용 시나리오:**
- 🚀 **개발/테스트**: InMemory (빠른 개발)
- 🏢 **소규모 운영**: InMemory (단순한 구조)  
- 📈 **대규모 운영**: Kafka (높은 확장성)
- 🔄 **중간 규모**: Redis (적당한 성능과 복잡도)
