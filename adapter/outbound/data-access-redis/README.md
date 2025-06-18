# Ball Framework Redis Data Access Module

Ball Framework의 Redis 데이터 접근 모듈로, Redis를 사용한 분산 락 기능을 제공합니다.

## 목차
1. [모듈 개요](#모듈-개요)
2. [RedisLockProvider](#redislockprovider)
3. [자동 설정](#자동-설정)
4. [사용 예시](#사용-예시)
5. [베스트 프랙티스](#베스트-프랙티스)

## 모듈 개요

Redis Data Access 모듈은 다음과 같은 패키지 구조로 구성되어 있습니다:

```
adapter/outbound/data-access-redis/
└── RedisLockProvider.kt    # Redis 기반 분산 락 구현
```

**주요 의존성:**
- Spring Boot Starter Data Redis
- Spring Integration Redis
- Ball Framework Shared Lock

**핵심 특징:**
- Redis 기반 분산 락 제공
- Spring Integration Redis 활용
- 자동 락 해제 (TTL 지원)
- 락 획득 실패 시 예외 처리

## RedisLockProvider

Redis를 사용하여 분산 락을 구현하는 클래스입니다.

### 기본 구조

```kotlin
@Component
@ConditionalOnProperty(name = ["ball.adapter.redis.enabled"], havingValue = "true", matchIfMissing = true)
class RedisLockProvider(
    private val redisTemplate: RedisTemplate<String, String>
) : LockProvider {

    private val lockRegistry = RedisLockRegistry(redisTemplate.connectionFactory!!, REGISTRY_KEY)

    override fun acquireLock(key: String, timeout: Long, leaseTime: Long): Lock? {
        return try {
            val redisLock = lockRegistry.obtain(key)
            
            if (redisLock.tryLock(timeout, TimeUnit.MILLISECONDS)) {
                // TTL 설정을 위한 Redis 명령 실행
                setLockExpiration(key, leaseTime)
                
                RedisDistributedLock(redisLock, key, redisTemplate)
            } else {
                null
            }
        } catch (e: Exception) {
            log.warn("분산 락 획득 실패: key={}, error={}", key, e.message)
            null
        }
    }

    override fun releaseLock(key: String) {
        try {
            val redisLock = lockRegistry.obtain(key)
            if (redisLock.isHeldByCurrentThread) {
                redisLock.unlock()
            }
        } catch (e: Exception) {
            log.warn("분산 락 해제 실패: key={}, error={}", key, e.message)
        }
    }

    private fun setLockExpiration(key: String, leaseTime: Long) {
        redisTemplate.expire("$REGISTRY_KEY:$key", Duration.ofMillis(leaseTime))
    }

    companion object {
        private const val REGISTRY_KEY = "ball-framework-locks"
        private val log = LoggerFactory.getLogger(RedisLockProvider::class.java)
    }
}
```

### RedisDistributedLock 구현

```kotlin
private class RedisDistributedLock(
    private val redisLock: Lock,
    private val key: String,
    private val redisTemplate: RedisTemplate<String, String>
) : Lock {

    @Volatile
    private var released = false

    override fun unlock() {
        if (!released) {
            try {
                redisLock.unlock()
                released = true
                log.debug("분산 락 해제 완료: key={}", key)
            } catch (e: Exception) {
                log.warn("분산 락 해제 중 오류 발생: key={}, error={}", key, e.message)
            }
        }
    }

    override fun tryLock(): Boolean = redisLock.tryLock()

    override fun tryLock(time: Long, unit: TimeUnit): Boolean = redisLock.tryLock(time, unit)

    override fun lock() = redisLock.lock()

    override fun lockInterruptibly() = redisLock.lockInterruptibly()

    override fun newCondition(): Condition = redisLock.newCondition()
}
```

**주요 특징:**
- **Spring Integration Redis**: 검증된 분산 락 구현 활용
- **TTL 지원**: 락이 자동으로 만료되도록 설정
- **스레드 안전성**: 동일 스레드에서만 락 해제 가능
- **예외 처리**: 락 획득/해제 실패 시 안전한 처리

## 자동 설정

### Redis 설정

```yaml
# application.yml
ball:
  adapter:
    redis:
      enabled: true

spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: redis_password
      database: 0
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: -1ms
```

### 프로그래밍 설정

```kotlin
@Configuration
@EnableConfigurationProperties(RedisProperties::class)
@ConditionalOnProperty(name = ["ball.adapter.redis.enabled"], havingValue = "true")
class RedisConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, String> {
        val template = RedisTemplate<String, String>()
        template.connectionFactory = connectionFactory
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = StringRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()
        template.hashValueSerializer = StringRedisSerializer()
        template.afterPropertiesSet()
        return template
    }

    @Bean
    @ConditionalOnMissingBean
    fun redisLockProvider(redisTemplate: RedisTemplate<String, String>): RedisLockProvider {
        return RedisLockProvider(redisTemplate)
    }
}
```

## 사용 예시

### 기본 분산 락 사용

```kotlin
@Service
class AccountService(
    private val lockProvider: LockProvider,
    private val accountRepository: AccountRepository
) {
    
    fun transferMoney(fromAccountId: String, toAccountId: String, amount: BigDecimal) {
        // 계좌 ID를 정렬하여 데드락 방지
        val sortedAccountIds = listOf(fromAccountId, toAccountId).sorted()
        val lockKey = "account-transfer:${sortedAccountIds.joinToString("-")}"
        
        val lock = lockProvider.acquireLock(
            key = lockKey,
            timeout = 5000L,
            leaseTime = 30000L
        ) ?: throw LockAcquisitionException("계좌 이체 락 획득 실패")
        
        try {
            // 이체 로직 실행
            val fromAccount = accountRepository.findById(fromAccountId)!!
            val toAccount = accountRepository.findById(toAccountId)!!
            
            fromAccount.withdraw(amount)
            toAccount.deposit(amount)
            
            accountRepository.save(fromAccount)
            accountRepository.save(toAccount)
            
        } finally {
            lock.unlock()
        }
    }
}
```

### @DistributedLock 어노테이션과 함께 사용

```kotlin
@Service
class InventoryService(
    private val inventoryRepository: InventoryRepository
) {
    
    @DistributedLock(
        key = "inventory-update",
        timeout = 3000,
        leaseTime = 10000
    )
    fun updateInventory(
        @LockKey("productId") productId: String,
        @LockKey("warehouseId") warehouseId: String,
        quantity: Int
    ) {
        val inventory = inventoryRepository.findByProductIdAndWarehouseId(productId, warehouseId)
            ?: throw EntityNotFoundException("재고 정보를 찾을 수 없습니다")
        
        inventory.updateQuantity(quantity)
        inventoryRepository.save(inventory)
    }
}
```

### 조건부 락 사용

```kotlin
@Service
class OrderProcessingService(
    private val lockProvider: LockProvider,
    private val orderRepository: OrderRepository
) {
    
    fun processOrderIfNotProcessing(orderId: String): Boolean {
        val lockKey = "order-processing:$orderId"
        
        val lock = lockProvider.acquireLock(
            key = lockKey,
            timeout = 100L,  // 즉시 실패
            leaseTime = 60000L
        )
        
        if (lock == null) {
            log.info("주문 {} 이미 처리 중", orderId)
            return false
        }
        
        try {
            val order = orderRepository.findById(orderId)!!
            
            if (order.status != OrderStatus.PENDING) {
                log.info("주문 {} 이미 처리됨: {}", orderId, order.status)
                return false
            }
            
            // 주문 처리 로직
            order.process()
            orderRepository.save(order)
            
            log.info("주문 {} 처리 완료", orderId)
            return true
            
        } finally {
            lock.unlock()
        }
    }
}
```

### 배치 처리에서 분산 락 활용

```kotlin
@Component
class DailyReportBatchJob(
    private val lockProvider: LockProvider,
    private val reportService: ReportService
) {
    
    @Scheduled(cron = "0 0 2 * * *")  // 매일 새벽 2시
    fun generateDailyReport() {
        val today = LocalDate.now()
        val lockKey = "daily-report:${today.format(DateTimeFormatter.ISO_LOCAL_DATE)}"
        
        val lock = lockProvider.acquireLock(
            key = lockKey,
            timeout = 1000L,
            leaseTime = 3600000L  // 1시간
        )
        
        if (lock == null) {
            log.info("일간 리포트 {} 이미 생성 중", today)
            return
        }
        
        try {
            log.info("일간 리포트 {} 생성 시작", today)
            
            reportService.generateDailyReport(today)
            
            log.info("일간 리포트 {} 생성 완료", today)
            
        } catch (e: Exception) {
            log.error("일간 리포트 {} 생성 실패", today, e)
            throw e
        } finally {
            lock.unlock()
        }
    }
}
```

## 베스트 프랙티스

### 1. 적절한 락 키 설계

```kotlin
// ✅ Good: 계층적이고 명확한 락 키
val lockKey = "inventory:product:${productId}:warehouse:${warehouseId}"
val userLockKey = "user:profile:update:${userId}"
val reportLockKey = "report:daily:${date.format(DateTimeFormatter.ISO_LOCAL_DATE)}"

// ❌ Bad: 모호한 락 키
val lockKey = "lock1"
val userLockKey = userId
val reportLockKey = "report"
```

### 2. 적절한 타임아웃 설정

```kotlin
// ✅ Good: 작업 특성에 맞는 타임아웃
val lock = lockProvider.acquireLock(
    key = lockKey,
    timeout = 5000L,    // 5초 대기 (일반적인 업무 로직)
    leaseTime = 30000L  // 30초 후 자동 해제
)

// 배치 작업의 경우
val batchLock = lockProvider.acquireLock(
    key = batchLockKey,
    timeout = 1000L,     // 1초 대기 (즉시 실패)
    leaseTime = 3600000L // 1시간 후 자동 해제
)

// ❌ Bad: 모든 상황에 동일한 타임아웃
val lock = lockProvider.acquireLock(key, 10000L, 10000L)
```

### 3. 안전한 락 해제

```kotlin
// ✅ Good: try-finally로 안전한 락 해제
val lock = lockProvider.acquireLock(lockKey, timeout, leaseTime)
    ?: throw LockAcquisitionException("락 획득 실패")

try {
    // 비즈니스 로직
} finally {
    lock.unlock()
}

// ✅ Good: use 함수 활용
lockProvider.acquireLock(lockKey, timeout, leaseTime)?.use { lock ->
    // 비즈니스 로직
} ?: throw LockAcquisitionException("락 획득 실패")

// ❌ Bad: 락 해제 누락 위험
val lock = lockProvider.acquireLock(lockKey, timeout, leaseTime)!!
// 비즈니스 로직
lock.unlock() // 예외 발생 시 실행되지 않을 수 있음
```

### 4. 데드락 방지

```kotlin
// ✅ Good: 락 순서 정렬로 데드락 방지
fun transferBetweenAccounts(accountId1: String, accountId2: String, amount: BigDecimal) {
    val sortedIds = listOf(accountId1, accountId2).sorted()
    val lockKey = "account-transfer:${sortedIds.joinToString("-")}"
    
    lockProvider.acquireLock(lockKey, 5000L, 30000L)?.use { lock ->
        // 이체 로직
    }
}

// ❌ Bad: 락 순서 불일치로 데드락 위험
fun transferBetweenAccounts(accountId1: String, accountId2: String, amount: BigDecimal) {
    val lock1 = lockProvider.acquireLock("account:$accountId1", 5000L, 30000L)!!
    val lock2 = lockProvider.acquireLock("account:$accountId2", 5000L, 30000L)!!
    // 데드락 발생 가능
}
```

### 5. 락 실패 처리

```kotlin
// ✅ Good: 락 실패에 대한 명확한 처리
fun processOrder(orderId: String): OrderProcessResult {
    val lockKey = "order:processing:$orderId"
    
    val lock = lockProvider.acquireLock(lockKey, 100L, 60000L)
    
    return if (lock != null) {
        try {
            // 주문 처리
            OrderProcessResult.success(processOrderInternal(orderId))
        } finally {
            lock.unlock()
        }
    } else {
        OrderProcessResult.alreadyProcessing("주문이 이미 처리 중입니다")
    }
}

// ❌ Bad: 락 실패 시 예외 발생
fun processOrder(orderId: String) {
    val lock = lockProvider.acquireLock(lockKey, 100L, 60000L)
        ?: throw IllegalStateException("주문 처리 락 획득 실패")
    // 사용자에게 적절한 피드백 제공 불가
}
```

### 6. 모니터링과 로깅

```kotlin
// ✅ Good: 락 획득/해제 로깅
@Service
class MonitoredLockService(
    private val lockProvider: LockProvider
) {
    
    fun <T> withLock(
        lockKey: String,
        timeout: Long,
        leaseTime: Long,
        operation: () -> T
    ): T {
        val startTime = System.currentTimeMillis()
        
        val lock = lockProvider.acquireLock(lockKey, timeout, leaseTime)
            ?: throw LockAcquisitionException("락 획득 실패: $lockKey")
        
        val acquiredTime = System.currentTimeMillis()
        log.info("락 획득 완료: key={}, waitTime={}ms", lockKey, acquiredTime - startTime)
        
        try {
            return operation()
        } finally {
            lock.unlock()
            val totalTime = System.currentTimeMillis() - startTime
            log.info("락 해제 완료: key={}, totalTime={}ms", lockKey, totalTime)
        }
    }
}
```

Ball Framework의 Redis Data Access 모듈은 안정적이고 효율적인 분산 락 기능을 제공하여, 멀티 인스턴스 환경에서의 동시성 문제를 해결할 수 있게 해줍니다.