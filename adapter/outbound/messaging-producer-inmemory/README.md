# InMemory DomainEventPublisher

단일 프로세스 내에서 도메인 이벤트를 발행하고 처리하는 기본 구현체입니다.

## 🎯 특징

- **외부 의존성 없음**: Kafka, Redis 등 없이도 이벤트 기반 아키텍처 구현
- **동기/비동기 선택**: 설정으로 처리 방식 선택 가능
- **Spring 통합**: ApplicationEventPublisher와 연동
- **기본 구현체**: 다른 구현체가 없을 때 자동 활성화

## 🔧 설정

### application.yml
```yaml
ball:
  event:
    publisher:
      type: inmemory  # inmemory, kafka, redis 등
      inmemory:
        async: true              # 비동기 처리 (기본값: true)
        enable-retry: false      # 재시도 기능 (기본값: false)  
        max-retry-attempts: 3    # 최대 재시도 횟수
        retry-delay-ms: 1000     # 재시도 간격
        timeout-ms: 0            # 타임아웃 (0 = 무제한)
        enable-debug-logging: false  # 디버그 로깅
```

## 📋 사용 예시

### 1. Application Service에서 사용
```kotlin
@Service
class UserService(
    private val userRepository: UserRepository,
    private val domainEventPublisher: DomainEventPublisher  // InMemoryEventPublisher 주입됨
) {
    fun createUser(command: CreateUserCommand): User {
        val user = User.create(command.email, command.name)
        userRepository.save(user)
        
        // 이벤트 발행 (InMemory에서 처리됨)
        domainEventPublisher.publish(user.domainEvents)
        user.clearEvents()
        
        return user
    }
}
```

### 2. Spring EventListener와 연동
```kotlin
@Component
class UserEventListener {
    
    @EventListener
    @Async
    fun handleUserEvent(wrapper: DomainEventWrapper) {
        when (val event = wrapper.domainEvent) {
            is UserCreatedEvent -> handleUserCreated(event)
            is UserUpdatedEvent -> handleUserUpdated(event)
        }
    }
    
    private fun handleUserCreated(event: UserCreatedEvent) {
        // 사용자 생성 후 처리 로직
        println("User created: ${event.userId}")
    }
}
```

## 🔄 환경별 설정

### 개발 환경 (application-dev.yml)
```yaml
ball:
  event:
    publisher:
      type: inmemory
      inmemory:
        async: false  # 개발 시 동기 처리로 디버깅 용이
        enable-debug-logging: true
```

### 운영 환경 (application-prod.yml)
```yaml
ball:
  event:
    publisher:
      type: kafka  # 운영에서는 Kafka 사용
```

### 테스트 환경
```kotlin
@TestPropertySource(properties = [
    "ball.event.publisher.type=inmemory",
    "ball.event.publisher.inmemory.async=false"
])
class UserServiceTest {
    // 테스트 코드
}
```

## 🚀 장점

1. **빠른 시작**: 외부 시스템 설치 없이 이벤트 기반 개발 가능
2. **단순함**: 복잡한 메시징 설정 불필요
3. **테스트 친화적**: 단위 테스트에서 쉽게 검증 가능
4. **점진적 확장**: 추후 Kafka 등으로 쉽게 전환 가능

## ⚠️ 제한사항

1. **단일 프로세스**: 여러 인스턴스 간 이벤트 공유 불가
2. **영속성 없음**: 프로세스 재시작 시 이벤트 손실
3. **확장성**: 대용량 처리에는 부적합

## 🔧 확장 방법

추후 외부 메시징 시스템으로 전환하려면:

1. **의존성 추가**: `messaging-producer-kafka` 모듈 추가
2. **설정 변경**: `ball.event.publisher.type: kafka`
3. **코드 변경 없음**: 동일한 `DomainEventPublisher` 인터페이스 사용
