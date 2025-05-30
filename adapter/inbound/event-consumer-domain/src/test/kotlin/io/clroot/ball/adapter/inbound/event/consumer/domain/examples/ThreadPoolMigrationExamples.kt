package io.clroot.ball.adapter.inbound.event.consumer.domain.examples

import io.clroot.ball.application.port.inbound.EventConsumerPort
import io.clroot.ball.application.port.inbound.ExecutorConfig
import io.clroot.ball.application.port.inbound.EventErrorHandler
import io.clroot.ball.domain.event.DomainEvent
import java.time.Instant

/**
 * ThreadPool 기반 EventConsumerPort 마이그레이션 예시
 * 
 * 코루틴 기반에서 ThreadPool 기반으로 변경된 사용법을 보여줍니다.
 */

// ===== 1. 기본 사용 예시 =====

/**
 * 사용자 생성 이벤트
 */
data class UserCreatedEvent(
    override val id: String,
    override val type: String,
    override val occurredAt: Instant,
    val userId: String,
    val email: String,
    val name: String
) : DomainEvent

/**
 * BEFORE: 코루틴 기반 (기존 방식)
 */
/*
class OldUserCreatedEventHandler : EventConsumerPort<UserCreatedEvent> {
    override val eventType = UserCreatedEvent::class
    
    override suspend fun consume(event: UserCreatedEvent) {
        withContext(Dispatchers.IO) {  // 😕 복잡한 래핑
            userRepository.save(createUser(event))
        }
    }
}
*/

/**
 * AFTER: ThreadPool 기반 (새로운 방식)
 */
class UserCreatedEventHandler : EventConsumerPort<UserCreatedEvent> {
    
    // 모의 레포지토리 (실제로는 @Autowired로 주입)
    private val userRepository = MockUserRepository()
    
    override val eventType = UserCreatedEvent::class
    
    // JPA에 최적화된 보수적 설정
    override val executorConfig = ExecutorConfig.conservative().copy(
        threadNamePrefix = "user-event"
    )
    
    // 재시도 가능한 에러 핸들러
    override val errorHandler = EventErrorHandler.retrying(maxAttempts = 3)
    
    // ✅ 단순하고 직관적!
    override fun consume(event: UserCreatedEvent) {
        println("🔄 Processing user creation: ${event.email}")
        
        // 자연스러운 JPA 호출 (blocking)
        val user = User(
            id = event.userId,
            email = event.email,
            name = event.name,
            createdAt = event.occurredAt
        )
        
        userRepository.save(user)
        
        // 부가 작업도 동일한 스레드에서
        sendWelcomeEmail(user)
        
        println("✅ User created successfully: ${event.email}")
    }
    
    private fun sendWelcomeEmail(user: User) {
        // 이메일 발송 로직
        println("📧 Welcome email sent to: ${user.email}")
    }
}

// ===== 2. 고성능 처리 예시 =====

/**
 * 로그 이벤트
 */
data class AuditLogEvent(
    override val id: String,
    override val type: String,
    override val occurredAt: Instant,
    val userId: String,
    val action: String,
    val resource: String
) : DomainEvent

/**
 * 고성능 로그 처리 핸들러
 */
class AuditLogEventHandler : EventConsumerPort<AuditLogEvent> {
    
    private val auditLogRepository = MockAuditLogRepository()
    
    override val eventType = AuditLogEvent::class
    
    // 로그는 빠른 처리가 중요
    override val executorConfig = ExecutorConfig.highThroughput().copy(
        threadNamePrefix = "audit-log"
    )
    
    // 로그는 실패해도 Skip
    override val errorHandler = EventErrorHandler.logging()
    
    override fun consume(event: AuditLogEvent) {
        val auditLog = AuditLog(
            id = event.id,
            userId = event.userId,
            action = event.action,
            resource = event.resource,
            timestamp = event.occurredAt
        )
        
        auditLogRepository.save(auditLog)
    }
}

// ===== 3. 복잡한 비즈니스 로직 예시 =====

/**
 * 주문 완료 이벤트
 */
data class OrderCompletedEvent(
    override val id: String,
    override val type: String,
    override val occurredAt: Instant,
    val orderId: String,
    val userId: String,
    val totalAmount: Long,
    val items: List<OrderItem>
) : DomainEvent

data class OrderItem(
    val productId: String,
    val quantity: Int,
    val price: Long
)

/**
 * 복합적인 비즈니스 로직 처리 핸들러
 */
class OrderCompletedEventHandler : EventConsumerPort<OrderCompletedEvent> {
    
    private val orderRepository = MockOrderRepository()
    private val inventoryService = MockInventoryService()
    private val pointService = MockPointService()
    private val notificationService = MockNotificationService()
    
    override val eventType = OrderCompletedEvent::class
    
    // JPA 연결 풀 고려한 설정
    override val executorConfig = ExecutorConfig.forJpa(connectionPoolSize = 20)
    
    override val errorHandler = EventErrorHandler.retrying(maxAttempts = 2)
    
    override fun consume(event: OrderCompletedEvent) {
        println("🛒 Processing order completion: ${event.orderId}")
        
        try {
            // 1. 주문 상태 업데이트
            val order = orderRepository.findById(event.orderId)
            
            order.status = "COMPLETED"
            order.completedAt = event.occurredAt
            orderRepository.save(order)
            
            // 2. 재고 차감
            inventoryService.decreaseStock(event.items)
            
            // 3. 포인트 적립
            pointService.earnPoints(event.userId, event.totalAmount)
            
            // 4. 알림 발송 (비동기로 처리 - ThreadPool의 장점!)
            Thread {
                notificationService.sendOrderCompletedNotification(order)
            }.start()
            
            println("✅ Order completion processed: ${event.orderId}")
            
        } catch (e: Exception) {
            println("❌ Order completion failed: ${event.orderId} - ${e.message}")
            throw e
        }
    }
}

// ===== 4. 에러 처리 전략 예시 =====

/**
 * 결제 실패 이벤트
 */
data class PaymentFailedEvent(
    override val id: String,
    override val type: String,
    override val occurredAt: Instant,
    val orderId: String,
    val reason: String
) : DomainEvent

/**
 * 커스텀 에러 핸들러를 사용하는 예시
 */
class PaymentFailedEventHandler : EventConsumerPort<PaymentFailedEvent> {
    
    override val eventType = PaymentFailedEvent::class
    
    // 커스텀 에러 핸들러
    override val errorHandler = object : EventErrorHandler {
        override fun handleError(event: io.clroot.ball.domain.event.Event, exception: Exception, attempt: Int): io.clroot.ball.application.port.inbound.ErrorAction {
            return when {
                exception is IllegalArgumentException -> {
                    println("⚠️ Invalid payment data, skipping: ${exception.message}")
                    io.clroot.ball.application.port.inbound.ErrorAction.SKIP
                }
                attempt < 2 -> {
                    println("🔄 Retrying payment processing (attempt $attempt)")
                    Thread.sleep(1000L * attempt.toLong()) // 점진적 백오프
                    io.clroot.ball.application.port.inbound.ErrorAction.RETRY
                }
                else -> {
                    println("❌ Payment processing failed permanently")
                    io.clroot.ball.application.port.inbound.ErrorAction.FAIL
                }
            }
        }
    }
    
    override fun consume(event: PaymentFailedEvent) {
        // 결제 실패 처리 로직
        println("💳 Processing payment failure: ${event.orderId} - ${event.reason}")
        
        // 실패 원인에 따른 분기 처리
        when (event.reason) {
            "INSUFFICIENT_FUNDS" -> handleInsufficientFunds(event)
            "INVALID_CARD" -> handleInvalidCard(event)
            "NETWORK_ERROR" -> handleNetworkError(event)
            else -> throw IllegalArgumentException("Unknown payment failure reason: ${event.reason}")
        }
    }
    
    private fun handleInsufficientFunds(event: PaymentFailedEvent) {
        println("💰 Handling insufficient funds for order: ${event.orderId}")
        // 고객에게 알림, 주문 보류 등
    }
    
    private fun handleInvalidCard(event: PaymentFailedEvent) {
        println("💳 Handling invalid card for order: ${event.orderId}")
        // 카드 정보 재입력 요청 등
    }
    
    private fun handleNetworkError(event: PaymentFailedEvent) {
        println("🌐 Handling network error for order: ${event.orderId}")
        throw RuntimeException("Network error - should retry")
    }
}

// ===== 5. 설정별 성능 비교 예시 =====

/**
 * 테스트 이벤트
 */
data class LoadTestEvent(
    override val id: String,
    override val type: String,
    override val occurredAt: Instant,
    val data: String
) : DomainEvent

/**
 * 보수적 설정 핸들러
 */
class ConservativeHandler : EventConsumerPort<LoadTestEvent> {
    override val eventType = LoadTestEvent::class
    override val executorConfig = ExecutorConfig.conservative()
    
    override fun consume(event: LoadTestEvent) {
        Thread.sleep(100L) // DB 작업 시뮬레이션
        println("🐌 Conservative: ${event.id}")
    }
}

/**
 * 고성능 설정 핸들러
 */
class HighThroughputHandler : EventConsumerPort<LoadTestEvent> {
    override val eventType = LoadTestEvent::class
    override val executorConfig = ExecutorConfig.highThroughput()
    
    override fun consume(event: LoadTestEvent) {
        Thread.sleep(100L) // DB 작업 시뮬레이션
        println("🚀 HighThroughput: ${event.id}")
    }
}

// ===== Mock 클래스들 (실제 구현에서는 실제 서비스 사용) =====

data class User(val id: String, val email: String, val name: String, val createdAt: Instant)
data class AuditLog(val id: String, val userId: String, val action: String, val resource: String, val timestamp: Instant)
data class Order(val id: String, var status: String, var completedAt: Instant?)

class MockUserRepository {
    fun save(user: User) = println("💾 Saved user: ${user.email}")
}

class MockAuditLogRepository {
    fun save(log: AuditLog) = println("📝 Saved audit log: ${log.action}")
}

class MockOrderRepository {
    fun findById(id: String): Order = Order(id, "PENDING", null)
    fun save(order: Order) = println("💾 Saved order: ${order.id}")
}

class MockInventoryService {
    fun decreaseStock(items: List<OrderItem>) = println("📦 Decreased stock for ${items.size} items")
}

class MockPointService {
    fun earnPoints(userId: String, amount: Long) = println("⭐ Earned ${amount/100} points for user: $userId")
}

class MockNotificationService {
    fun sendOrderCompletedNotification(order: Order) = println("📱 Sent notification for order: ${order.id}")
}
