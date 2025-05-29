# Ball Framework - Domain Event Publisher

도메인 이벤트 발행을 위한 모듈입니다. Spring의 ApplicationEventPublisher를 사용하여 프로세스 내에서 즉시 처리되는 도메인 이벤트를 발행합니다.

## 📋 주요 특징

- **높은 성능**: 메모리 내 직접 처리로 최고의 성능
- **트랜잭션 통합**: 같은 트랜잭션 컨텍스트에서 처리
- **개발 친화적**: 복잡한 설정 없이 즉시 사용 가능
- **테스트 용이**: 단위 테스트에서 쉽게 검증 가능

## 🚀 사용법

### 1. 의존성 추가

```kotlin
dependencies {
    implementation("io.clroot.ball:event-publisher-domain")
}
```

### 2. 도메인 이벤트 정의

```kotlin
@DomainScope  // 도메인 이벤트임을 명시 (선택적)
data class UserPasswordChangedEvent(
    override val id: String = UUID.randomUUID().toString(),
    override val type: String = "UserPasswordChanged",
    override val occurredAt: Instant = Instant.now(),
    val userId: String,
    val changedAt: Instant
) : DomainEvent
```

### 3. 서비스에서 이벤트 발행

```kotlin
@Service
class UserService(
    private val domainEventPublisher: DomainEventPublisher,
    private val userRepository: UserRepository
) {
    
    @Transactional
    fun changePassword(userId: String, newPassword: String) {
        val user = userRepository.findById(userId)
            ?: throw UserNotFoundException(userId)
        
        user.changePassword(newPassword)
        userRepository.save(user)
        
        // 도메인 이벤트 발행 - 같은 트랜잭션 내에서 처리
        domainEventPublisher.publish(
            UserPasswordChangedEvent(
                userId = userId,
                changedAt = Instant.now()
            )
        )
    }
}
```

### 4. 이벤트 핸들러 작성

```kotlin
@Component
class UserDomainEventHandler {
    
    @EventListener  // Spring의 기본 이벤트 리스너
    fun handleUserPasswordChanged(wrapper: DomainEventWrapper) {
        val event = wrapper.domainEvent
        if (event is UserPasswordChangedEvent) {
            log.info("User password changed: ${event.userId}")
            
            // 도메인 로직 처리
            // - 패스워드 히스토리 저장
            // - 보안 알림 발송
            // - 감사 로그 기록 등
        }
    }
    
    // 또는 타입 안전한 핸들러
    @EventHandler  // Ball Framework의 타입 안전 핸들러 (향후 제공 예정)
    fun handlePasswordChanged(event: UserPasswordChangedEvent) {
        log.info("Password changed for user: ${event.userId}")
    }
}
```

## ⚙️ 설정

### application.yml
```yaml
ball:
  events:
    domain:
      enabled: true
      async: true              # 비동기 처리 여부
      enable-retry: true       # 재시도 활성화
      max-retry-attempts: 3    # 최대 재시도 횟수
      retry-delay-ms: 1000     # 재시도 간격
      timeout-ms: 5000         # 타임아웃
      enable-debug-logging: false  # 디버그 로깅
      enable-metrics: true     # 메트릭 수집
      validation:
        strict: true           # 엄격한 검증
        required-fields:       # 필수 필드
          - "id"
          - "type"
          - "occurredAt"
```

### 환경별 설정
```yaml
# 개발 환경
---
spring:
  profiles: dev
ball:
  events:
    domain:
      async: false           # 디버깅을 위해 동기 처리
      enable-debug-logging: true

# 운영 환경  
---
spring:
  profiles: prod
ball:
  events:
    domain:
      async: true
      timeout-ms: 3000       # 더 짧은 타임아웃
      enable-metrics: true
```

## 📊 모니터링

### 메트릭
- `domain.events.published`: 발행된 이벤트 수
- `domain.events.publish.errors`: 발행 실패 수
- `domain.events.processing.duration`: 처리 시간

### 헬스체크
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: always
```

## 🧪 테스트

### 단위 테스트 예시
```kotlin
@Test
fun `should publish domain event when password changed`() {
    // given
    val userId = "user-123"
    val newPassword = "newPassword123"
    
    // when
    userService.changePassword(userId, newPassword)
    
    // then
    verify(exactly = 1) { 
        domainEventPublisher.publish(any<UserPasswordChangedEvent>()) 
    }
}
```

### 통합 테스트 예시
```kotlin
@SpringBootTest
@Transactional
class UserServiceIntegrationTest {
    
    @Autowired
    private lateinit var userService: UserService
    
    @MockBean
    private lateinit var domainEventPublisher: DomainEventPublisher
    
    @Test
    fun `should handle password change with event`() {
        // given & when
        userService.changePassword("user-123", "newPassword")
        
        // then
        verify { domainEventPublisher.publish(any<UserPasswordChangedEvent>()) }
    }
}
```

## 🔄 vs Integration Events

| 특징 | Domain Events | Integration Events |
|------|---------------|-------------------|
| **목적** | 도메인 내부 처리 | 서비스 간 통신 |
| **범위** | 프로세스 내부 | 프로세스 간 |
| **성능** | 매우 높음 | 높음 |
| **안정성** | 트랜잭션 의존 | 메시지 영속성 |
| **복잡도** | 낮음 | 중간 |
| **사용 시점** | 비즈니스 로직 내부 | 마이크로서비스 통합 |

## 🚨 주의사항

1. **트랜잭션 롤백**: 트랜잭션이 롤백되면 이벤트도 함께 취소됩니다.
2. **JVM 종료**: 프로세스 종료 시 처리 중인 이벤트가 손실될 수 있습니다.
3. **메모리 사용**: 대량의 이벤트 발행 시 메모리 사용량을 모니터링하세요.
4. **순환 참조**: 이벤트 핸들러에서 다시 이벤트를 발행할 때 순환 참조를 주의하세요.

## 📈 성능 최적화

### 비동기 처리 설정
```yaml
ball:
  events:
    domain:
      async: true

spring:
  task:
    execution:
      pool:
        core-size: 5
        max-size: 20
        queue-capacity: 100
```

### 배치 처리
```kotlin
// 여러 이벤트를 한번에 발행
domainEventPublisher.publish(listOf(event1, event2, event3))
```

## 🔧 고급 설정

### 커스텀 검증
```kotlin
@Component
class CustomEventValidator : DomainEventValidator {
    override fun validate(event: DomainEvent) {
        // 커스텀 검증 로직
    }
}
```

### 커스텀 에러 핸들링
```kotlin
@Component
class CustomEventErrorHandler : DomainEventErrorHandler {
    override fun handleError(event: DomainEvent, error: Exception) {
        // 커스텀 에러 처리
    }
}
```
