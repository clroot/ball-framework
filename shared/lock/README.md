# 🔐 Ball Framework - Distributed Lock Module

## 개요

`shared/lock` 모듈은 **@LockKey 어노테이션 기반**의 깔끔하고 직관적인 분산 락 기능을 제공합니다.
복잡한 SpEL 표현식 대신 명시적인 어노테이션을 사용하여 락 키를 정의할 수 있습니다.

## 주요 특징

- 🎯 **명시적이고 안전한 락 키 정의** - @LockKey 어노테이션으로 명확한 의도 표현
- 🚀 **뛰어난 성능** - SpEL 파싱 오버헤드 없음, 메타데이터 캐싱
- 🔧 **리팩터링 안전** - 파라미터명 변경에 영향받지 않음
- 🏗️ **확장 가능한 구조** - 다양한 LockProvider 구현체 지원
- 🧪 **테스트 친화적** - 로컬 락 제공자로 단위 테스트 지원

## 빠른 시작

### 1. 기본 사용법

```kotlin
@Service
class UserService {
    
    @DistributedLock(key = "user-{userId}")
    fun updateUser(@LockKey("userId") id: String, data: UserData) {
        // 락 키: "user-12345"
        // 같은 사용자 ID로 동시 업데이트 방지
    }
}
```

### 2. 객체 프로퍼티 사용

```kotlin
@Service
class OrderService {
    
    @DistributedLock(key = "order-{orderId}")
    fun processOrder(@LockKey(value = "orderId", property = "id") order: Order) {
        // 락 키: "order-ORDER123" (order.id 사용)
    }
    
    @DistributedLock(key = "payment-{userId}-{paymentId}")
    fun processPayment(
        @LockKey(value = "userId", property = "user.id") payment: Payment,
        @LockKey(value = "paymentId", property = "id") samePayment: Payment
    ) {
        // 락 키: "payment-USER456-PAY789"
        // payment.user.id와 payment.id 사용
    }
}
```

### 3. 다중 파라미터 조합

```kotlin
@Service  
class InventoryService {
    
    @DistributedLock(key = "inventory-{productId}-{warehouseId}")
    fun updateStock(
        @LockKey("productId") productId: String,
        @LockKey("warehouseId") warehouseId: String,
        quantity: Int  // 락 키에 사용되지 않음
    ) {
        // 락 키: "inventory-PROD123-WH001"
    }
}
```

### 4. Null 값 처리

```kotlin
@Service
class NotificationService {
    
    @DistributedLock(key = "notification-{userId}-{type}")
    fun sendNotification(
        @LockKey("userId") userId: String,
        @LockKey(value = "type", nullValue = "GENERAL") type: String?
    ) {
        // type이 null인 경우 "GENERAL" 사용
        // 락 키: "notification-USER123-GENERAL"
    }
}
```

## 설정

### 락 시간 조정

```kotlin
@DistributedLock(
    key = "critical-{operationId}",
    waitTime = 30,      // 30초 대기
    leaseTime = 60,     // 60초 유지
    timeUnit = TimeUnit.SECONDS
)
fun criticalOperation(@LockKey("operationId") id: String) {
    // 중요한 작업
}
```

### 로컬 개발 환경

기본적으로 `LocalLockProvider`가 제공되어 별도 설정 없이 개발/테스트가 가능합니다.

```kotlin
// 자동으로 등록되는 로컬 락 제공자
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
class LocalLockProvider : LockProvider {
    // 로컬 메모리 기반 락 구현
}
```

## 고급 사용법

### 중첩 프로퍼티 접근

```kotlin
data class User(val id: String, val profile: Profile)
data class Profile(val settings: Settings)  
data class Settings(val theme: String)

@Service
class ThemeService {
    
    @DistributedLock(key = "theme-{userId}-{theme}")
    fun updateTheme(
        @LockKey(value = "userId", property = "id") user: User,
        @LockKey(value = "theme", property = "profile.settings.theme") sameUser: User
    ) {
        // user.id와 user.profile.settings.theme 사용
    }
}
```

### 커스텀 LockProvider 구현

```kotlin
@Component
@Primary
class RedisLockProvider(
    private val redisTemplate: RedisTemplate<String, String>
) : LockProvider {
    
    override fun <T> withLock(key: String, waitTime: Long, leaseTime: Long, block: () -> T): T {
        // Redis 기반 분산 락 구현
    }
}
```

## 에러 처리

### 락 획득 실패

```kotlin
try {
    service.updateUser("user123", userData)
} catch (e: LockAcquisitionException) {
    log.warn("Failed to acquire lock for user: user123", e)
    // 재시도 로직 또는 에러 응답
}
```

### 락 키 해석 실패

```kotlin
// 잘못된 사용 예시
@DistributedLock(key = "user-{userId}-{missingKey}")  // missingKey에 대응하는 @LockKey 없음
fun badMethod(@LockKey("userId") id: String) { }

// LockKeyResolutionException 발생
```

## 테스트

### 단위 테스트

```kotlin
@Test
fun `분산 락이 적용된 메서드 테스트`() {
    val service = TestService()
    // LocalLockProvider가 자동으로 사용됨
    
    service.updateUser("user123", userData)
    // 테스트 검증 로직
}
```

### 동시성 테스트

```kotlin
@Test  
fun `동일한 락 키로 동시 접근시 순차 실행 테스트`() {
    val latch = CountDownLatch(2)
    val results = mutableListOf<String>()
    
    repeat(2) {
        thread {
            try {
                val result = service.processUser("user123")
                synchronized(results) { results.add(result) }
            } finally {
                latch.countDown()
            }
        }
    }
    
    latch.await()
    // 순차 실행 검증
}
```

## 마이그레이션 가이드

### 기존 SpEL 방식에서 @LockKey 방식으로

```kotlin
// Before (SpEL)
@DistributedLock(key = "#userId + ':' + #operation", keyType = KeyType.SPEL)
fun oldMethod(userId: String, operation: String) { }

// After (@LockKey)  
@DistributedLock(key = "user-{userId}-{operation}")
fun newMethod(@LockKey("userId") id: String, @LockKey("operation") op: String) { }
```

## 성능 특징

- **메타데이터 캐싱**: 메서드별 @LockKey 정보를 캐싱하여 리플렉션 오버헤드 최소화
- **SpEL 제거**: 표현식 파싱 비용 완전 제거
- **지연 로딩**: 필요한 경우에만 프로퍼티 리플렉션 수행

## 제한사항

- 파라미터당 하나의 @LockKey만 지원 (중첩 어노테이션 불가)
- 정적 컴파일 타임 검증은 별도 어노테이션 프로세서 필요
- 복잡한 조건부 로직은 비즈니스 로직에서 처리 필요

## 기여하기

이슈나 개선 사항이 있으시면 언제든 PR을 보내주세요! 🚀
