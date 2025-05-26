# Event Consumer Domain

**도메인 이벤트** 처리를 위한 Ball Framework 모듈입니다.

## 🎯 **목적**

Spring ApplicationEvent 메커니즘을 통해 **같은 프로세스 내에서** 발생하는 도메인 이벤트를 수신하고 처리합니다.

- ✅ **프로세스 내 이벤트 처리**
- ✅ **트랜잭션 내/외 처리 지원**  
- ✅ **동기/비동기 처리 지원**
- ✅ **Spring ApplicationEvent 기반**

## 🚀 **사용법**

### 1. 설정

```yaml
# application.yml
ball:
  events:
    domain:
      consumer:
        enabled: true                    # 도메인 이벤트 소비자 활성화
        async: false                     # 동기 처리 (기본값)
        enable-retry: false              # 재시도 비활성화
        timeout-ms: 5000                 # 5초 타임아웃
        enable-debug-logging: false      # 디버그 로깅
        enable-metrics: true             # 메트릭 수집
        
        # 도메인 이벤트 특화 설정
        use-spring-application-event: true    # Spring ApplicationEvent 사용
        process-in-transaction: true          # 트랜잭션 내 처리
        process-after-commit: false           # 커밋 후 처리 여부
```

### 2. 이벤트 핸들러 작성

```kotlin
@Component
class UserDomainEventHandler {

    @EventHandler
    fun handleUserCreated(event: UserCreatedEvent) {
        // 사용자 생성 후 도메인 로직 처리
        println("사용자가 생성되었습니다: ${event.userId}")
        
        // 예: 내부 캐시 갱신, 도메인 상태 업데이트 등
        updateUserCache(event.userId)
        notifyInternalServices(event)
    }

    @EventHandler  
    fun handleUserPasswordChanged(event: UserPasswordChangedEvent) {
        // 비밀번호 변경 시 관련 처리
        println("사용자 비밀번호가 변경되었습니다: ${event.userId}")
        
        // 예: 보안 로그 기록, 세션 무효화 등
        logSecurityEvent(event)
        invalidateUserSessions(event.userId)
    }
}
```

### 3. 도메인 이벤트 발행

```kotlin
@Component
class UserService(
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    
    fun createUser(userData: UserData): User {
        // 사용자 생성 로직
        val user = User.create(userData)
        
        // 도메인 이벤트 발행 (자동으로 DomainEventConsumer가 수신)
        val event = UserCreatedEvent(
            userId = user.id,
            email = user.email.value,
            occurredAt = Instant.now()
        )
        
        applicationEventPublisher.publishEvent(event)
        
        return user
    }
}
```

## ⚙️ **고급 설정**

### 비동기 처리

```yaml
ball:
  events:
    domain:
      consumer:
        async: true  # 비동기 처리 활성화
```

비동기 처리 시 전용 ThreadPoolTaskExecutor가 생성됩니다:
- 코어 스레드: CPU 코어 수
- 최대 스레드: CPU 코어 수 × 2  
- 큐 용량: 100개

### 트랜잭션 후 처리

```yaml
ball:
  events:
    domain:
      consumer:
        process-after-commit: true  # 트랜잭션 커밋 후 처리
```

```kotlin
@Component
class UserNotificationHandler {

    @EventHandler
    fun handleUserCreated(event: UserCreatedEvent) {
        // 트랜잭션 커밋 후에 실행됨
        // 외부 시스템 호출이나 이메일 발송 등에 적합
        emailService.sendWelcomeEmail(event.userId)
    }
}
```

## 🔍 **모니터링**

### 메트릭 (Micrometer)

도메인 이벤트 처리 관련 메트릭이 자동으로 수집됩니다:

- `domain.events.processed.total` - 처리된 이벤트 수
- `domain.events.processed.duration` - 처리 시간
- `domain.events.handler.errors` - 핸들러 에러 수

### 로깅

```yaml
logging:
  level:
    io.clroot.ball.adapter.inbound.event.consumer.domain: DEBUG
```

## 🎯 **언제 사용하나?**

### ✅ **도메인 이벤트에 적합한 경우**

- 같은 프로세스 내 비즈니스 로직 연계
- 트랜잭션 내에서 즉시 처리해야 하는 경우
- 도메인 규칙 실행 (예: 정책, 불변식 검증)
- 내부 캐시 갱신
- 도메인 상태 동기화

### ❌ **부적합한 경우** 

- 서비스 간 통신 → `event-consumer-integration` 사용
- 외부 시스템 연동 → `event-consumer-integration` 사용  
- 메시지 큐 기반 처리 → `event-consumer-integration` 사용
- 높은 처리량의 스트리밍 → `event-consumer-integration` 사용

## 🔧 **트러블슈팅**

### 이벤트가 처리되지 않는 경우

1. **핸들러 빈 등록 확인**
   ```kotlin
   @Component  // 이 어노테이션이 있는지 확인
   class MyEventHandler
   ```

2. **@EventHandler 어노테이션 확인**
   ```kotlin
   @EventHandler  // 이 어노테이션이 있는지 확인
   fun handleEvent(event: MyEvent)
   ```

3. **이벤트 타입 확인**
   ```kotlin
   // 파라미터가 DomainEvent를 상속하는지 확인
   fun handleEvent(event: DomainEvent)  // ✅
   fun handleEvent(event: String)       // ❌
   ```

### 성능 이슈

1. **동기 처리로 변경**
   ```yaml
   ball.events.domain.consumer.async: false
   ```

2. **타임아웃 조정**
   ```yaml
   ball.events.domain.consumer.timeout-ms: 10000  # 10초로 증가
   ```

3. **핸들러 로직 최적화**
   - 무거운 작업은 `event-consumer-integration`으로 이동
   - DB 쿼리 최소화
   - 캐시 활용
