# InMemory MessageConsumer (Auto Configuration)

단일 프로세스 내에서 도메인 이벤트를 수신하고 처리하는 Consumer 구현체입니다.
**Auto Configuration**이 적용되어 의존성만 추가하면 자동으로 활성화됩니다.

## 🚀 **빠른 시작**

### 1. 의존성 추가

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.clroot.ball:messaging-consumer-inmemory:2.0.0")
}
```

### 2. 이벤트 핸들러 구현

```kotlin
@Component
class UserEventHandler : DomainEventHandler<UserCreatedEvent> {
    override suspend fun handle(event: UserCreatedEvent) {
        // 이벤트 처리 로직
        println("User created: ${event.userId}")
    }
}
```

### 3. 완료! 🎉

더 이상 설정할 것이 없습니다. Auto Configuration이 모든 것을 자동으로 처리합니다.

## ⚙️ **Auto Configuration 특징**

### **자동 활성화 조건**
✅ `DomainEventHandler` 클래스가 클래스패스에 존재  
✅ `ball.event.consumer.inmemory.enabled=true` (기본값)  
✅ 필요한 Bean들이 없을 때 자동 생성

### **자동 생성되는 Bean들**
- `DomainEventHandlerRegistry`: Spring context의 모든 DomainEventHandler 자동 등록
- `InMemoryEventListener`: ApplicationEvent 수신 및 처리
- `eventTaskExecutor`: 비동기 이벤트 처리용 스레드 풀

### **자동 스캔**
모든 `@Component` DomainEventHandler들이 자동으로 발견되고 등록됩니다.

## 🔧 **설정 옵션**

### application.yml
```yaml
ball:
  event:
    consumer:
      inmemory:
        enabled: true           # Auto Configuration 활성화 (기본값: true)
        async: true            # 비동기 처리 (기본값: true)
        parallel: true         # 병렬 처리 (기본값: true)
        max-concurrency: 10    # 최대 동시 실행 수
        timeout-ms: 5000       # 처리 타임아웃
        enable-retry: false    # 재시도 활성화
        max-retry-attempts: 3  # 최대 재시도 횟수
        retry-delay-ms: 1000   # 재시도 간격
        enable-debug-logging: false  # 디버그 로깅
```

### **IDE 자동완성 지원**
Configuration properties metadata가 포함되어 있어 IDE에서 자동완성과 문서를 제공합니다.

## 📋 **사용 예시**

### **기본 사용법**
```kotlin
// 1. Handler 구현 (자동 등록됨)
@Component
class OrderEventHandler : DomainEventHandler<OrderCompletedEvent> {
    override suspend fun handle(event: OrderCompletedEvent) {
        // 주문 완료 처리
        emailService.sendConfirmation(event.customerId)
        inventoryService.updateStock(event.items)
    }
}

// 2. Application Service에서 이벤트 발행
@Service
class OrderService {
    fun completeOrder(orderId: String) {
        // 비즈니스 로직...
        
        // DomainEventWrapper를 통해 이벤트 발행
        applicationEventPublisher.publishEvent(
            DomainEventWrapper(OrderCompletedEvent(orderId))
        )
        // → OrderEventHandler.handle() 자동 호출됨!
    }
}
```

### **다중 Handler 지원**
```kotlin
@Component
class UserEventHandler : 
    DomainEventHandler<UserCreatedEvent>,
    DomainEventHandler<UserUpdatedEvent> {
    
    override suspend fun handle(event: UserCreatedEvent) {
        // 사용자 생성 처리
    }
    
    override suspend fun handle(event: UserUpdatedEvent) {
        // 사용자 수정 처리  
    }
}

@Component
class AuditEventHandler : DomainEventHandler<UserCreatedEvent> {
    override suspend fun handle(event: UserCreatedEvent) {
        // 감사 로그 기록 (같은 이벤트를 여러 핸들러가 처리 가능)
    }
}
```

## 🎛️ **고급 설정**

### **조건부 Handler 활성화**
```kotlin
@Component
@ConditionalOnProperty("feature.welcome-email.enabled", havingValue = "true")
class WelcomeEmailHandler : DomainEventHandler<UserCreatedEvent> {
    override suspend fun handle(event: UserCreatedEvent) {
        // 조건부로만 활성화되는 핸들러
    }
}
```

### **Custom ExecutorService**
```kotlin
@Configuration
class CustomEventConfiguration {
    
    @Bean("eventTaskExecutor")
    @Primary
    fun customEventTaskExecutor(): Executor {
        // Auto Configuration의 기본 ExecutorService 대체
        return createCustomExecutor()
    }
}
```

### **Auto Configuration 비활성화**
```yaml
ball:
  event:
    consumer:
      inmemory:
        enabled: false  # 전체 비활성화
```

또는 특정 Bean만 교체:
```kotlin
@Component
@Primary
class CustomInMemoryEventListener : InMemoryEventListener {
    // Auto Configuration Bean 대체
}
```

## 🧪 **테스트**

### **통합 테스트**
```kotlin
@SpringBootTest
class EventProcessingTest {
    
    @Autowired
    private lateinit var applicationEventPublisher: ApplicationEventPublisher
    
    @Autowired
    private lateinit var myEventHandler: MyEventHandler
    
    @Test
    fun `should process events automatically`() {
        // Given
        val event = MyDomainEvent("test-data")
        
        // When
        applicationEventPublisher.publishEvent(DomainEventWrapper(event))
        
        // Then
        await().until { myEventHandler.processedEvents.size == 1 }
    }
}
```

### **테스트 전용 설정**
```yaml
# application-test.yml
ball:
  event:
    consumer:
      inmemory:
        async: false  # 테스트에서는 동기 처리
        enable-debug-logging: true
```

## 🚀 **장점**

### **Zero Configuration**
- ✅ 의존성 추가만으로 즉시 동작
- ✅ Handler 구현만 하면 자동 등록
- ✅ 복잡한 설정 불필요

### **Spring Boot Native**
- ✅ Spring Boot Auto Configuration 표준 준수
- ✅ IDE 자동완성 지원
- ✅ Configuration Properties 메타데이터 제공

### **유연한 설정**
- ✅ 필요한 Bean만 선택적 대체 가능
- ✅ 조건부 활성화 지원
- ✅ 환경별 설정 가능

## 🔧 **트러블슈팅**

### **Handler가 자동 등록되지 않는 경우**
1. `@Component` 어노테이션 확인
2. Component Scan 범위 확인
3. Auto Configuration 활성화 여부 확인:
   ```bash
   # 활성화된 Auto Configuration 확인
   --debug 옵션으로 애플리케이션 실행
   ```

### **이벤트가 처리되지 않는 경우**
1. `DomainEventWrapper`로 이벤트 발행했는지 확인
2. Handler의 제네릭 타입과 이벤트 타입 일치 확인
3. 로그 레벨을 DEBUG로 설정하여 처리 과정 확인

### **성능 최적화**
```yaml
ball:
  event:
    consumer:
      inmemory:
        parallel: true          # 병렬 처리 활성화
        max-concurrency: 20     # 동시 실행 수 증가
        async: true             # 비동기 처리 활성화
```

## 📊 **모니터링**

Auto Configuration은 다음 정보를 로그로 제공합니다:
- 등록된 Handler 목록
- 이벤트 처리 시간
- 실패한 이벤트 정보
- 재시도 현황

```yaml
logging:
  level:
    io.clroot.ball.adapter.inbound.messaging.consumer.inmemory: DEBUG
```
