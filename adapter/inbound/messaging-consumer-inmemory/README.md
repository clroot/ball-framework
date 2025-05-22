# InMemory MessageConsumer

단일 프로세스 내에서 도메인 이벤트를 수신하고 처리하는 Consumer 구현체입니다.

## 🎯 특징

- **자동 Handler 발견**: `DomainEventHandler` 구현체들을 자동으로 스캔하고 등록
- **타입 안전한 라우팅**: 이벤트 타입에 따라 적절한 핸들러로 자동 라우팅
- **동기/비동기 처리**: 설정으로 처리 방식 선택 가능
- **병렬 처리**: 여러 핸들러를 병렬로 실행 가능
- **재시도 메커니즘**: 실패한 이벤트 자동 재시도
- **Spring 통합**: ApplicationEvent와 완벽 통합

## 🔧 설정

### application.yml
```yaml
ball:
  event:
    consumer:
      inmemory:
        enabled: true           # Consumer 활성화 (기본값: true)
        async: true            # 비동기 처리 (기본값: true)
        parallel: true         # 병렬 처리 (기본값: true)
        max-concurrency: 10    # 최대 동시 실행 수
        timeout-ms: 5000       # 처리 타임아웃
        enable-retry: false    # 재시도 활성화
        max-retry-attempts: 3  # 최대 재시도 횟수
        retry-delay-ms: 1000   # 재시도 간격
```

## 📋 사용 예시

### 1. Event Handler 구현
```kotlin
@Component
class UserEventHandler : DomainEventHandler<UserCreatedEvent> {
    
    private val log = LoggerFactory.getLogger(javaClass)
    
    override suspend fun handle(event: UserCreatedEvent) {
        log.info("Processing user created: ${event.userId}")
        
        // 비즈니스 로직 처리
        sendWelcomeEmail(event.email)
        updateUserStatistics()
        createUserProfile(event.userId)
    }
}
```

### 2. 다중 이벤트 타입 처리
```kotlin
@Component
class AuditEventHandler : 
    DomainEventHandler<UserCreatedEvent>,
    DomainEventHandler<UserUpdatedEvent>,
    DomainEventHandler<UserDeletedEvent> {
    
    override suspend fun handle(event: UserCreatedEvent) {
        auditService.logUserCreation(event)
    }
    
    override suspend fun handle(event: UserUpdatedEvent) {
        auditService.logUserUpdate(event)
    }
    
    override suspend fun handle(event: UserDeletedEvent) {
        auditService.logUserDeletion(event)
    }
}
```

### 3. 에러 처리가 있는 Handler
```kotlin
@Component
class EmailEventHandler : DomainEventHandler<UserCreatedEvent> {
    
    @Retryable(maxAttempts = 3, backoff = Backoff(delay = 1000))
    override suspend fun handle(event: UserCreatedEvent) {
        try {
            emailService.sendWelcomeEmail(event.email)
        } catch (e: EmailServiceException) {
            log.error("Failed to send welcome email to ${event.email}", e)
            throw e  // 재시도를 위해 예외 재발생
        }
    }
}
```

## 🔄 완전한 Event Bus 사용

### Publisher + Consumer 조합
```kotlin
// 1. 이벤트 발행 (Application Service)
@Service
class UserService(
    private val userRepository: UserRepository,
    private val eventPublisher: DomainEventPublisher
) {
    fun createUser(command: CreateUserCommand): User {
        val user = User.create(command.email, command.name)
        userRepository.save(user)
        
        // InMemory로 이벤트 발행
        eventPublisher.publish(user.domainEvents)
        user.clearEvents()
        
        return user
    }
}

// 2. 이벤트 소비 (자동 호출됨)
@Component
class WelcomeEmailHandler : DomainEventHandler<UserCreatedEvent> {
    override suspend fun handle(event: UserCreatedEvent) {
        emailService.sendWelcomeEmail(event.email)
    }
}

@Component  
class UserStatisticsHandler : DomainEventHandler<UserCreatedEvent> {
    override suspend fun handle(event: UserCreatedEvent) {
        statisticsService.incrementUserCount()
    }
}
```

## 🎛️ 환경별 설정

### 개발 환경
```yaml
ball:
  event:
    publisher:
      type: inmemory
    consumer:
      inmemory:
        enabled: true
        async: false  # 개발 시 동기 처리로 디버깅 용이
        parallel: false
```

### 테스트 환경
```yaml
ball:
  event:
    publisher:
      type: inmemory
    consumer:
      inmemory:
        enabled: true
        async: false  # 테스트에서는 동기 처리
        timeout-ms: 1000
```

### 운영 환경
```yaml
ball:
  event:
    publisher:
      type: kafka
    consumer:
      kafka:
        enabled: true
      inmemory:
        enabled: false  # Kafka 사용 시 InMemory 비활성화
```

## 🔧 고급 기능

### 1. 커스텀 ExecutorService
```kotlin
@Configuration
class CustomEventConfiguration {
    
    @Bean("eventTaskExecutor")
    fun customEventTaskExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 2
        executor.maxPoolSize = 20
        executor.queueCapacity = 200
        executor.setThreadNamePrefix("custom-event-")
        executor.initialize()
        return executor
    }
}
```

### 2. 조건부 Handler 활성화
```kotlin
@Component
@ConditionalOnProperty("feature.user-welcome-email.enabled", havingValue = "true")
class ConditionalWelcomeEmailHandler : DomainEventHandler<UserCreatedEvent> {
    override suspend fun handle(event: UserCreatedEvent) {
        // 조건부로만 실행되는 핸들러
    }
}
```

## 🚀 장점

1. **제로 설정**: Handler만 구현하면 자동으로 동작
2. **타입 안전성**: 컴파일 타임에 타입 체크
3. **유연한 설정**: 동기/비동기, 순차/병렬 선택 가능
4. **Spring 친화적**: Spring 생태계와 완벽 통합
5. **점진적 확장**: 외부 메시징 시스템으로 쉽게 전환

## ⚠️ 제한사항

1. **단일 프로세스**: 여러 인스턴스 간 이벤트 공유 불가
2. **메모리 기반**: 프로세스 재시작 시 이벤트 손실
3. **백프레셔 없음**: 대용량 이벤트 처리 시 메모리 부족 가능

## 🔧 트러블슈팅

### Handler가 호출되지 않는 경우
1. `@Component` 어노테이션 확인
2. 패키지 스캔 범위 확인
3. 이벤트 타입 매칭 확인
4. Consumer 활성화 여부 확인

### 성능 이슈
1. `parallel: true` 설정
2. `max-concurrency` 증가
3. `async: true` 설정
4. Handler 내부 로직 최적화
