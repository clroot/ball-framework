package io.clroot.example.project.service

import io.clroot.ball.application.port.outbound.DomainEventPublisher
import io.clroot.example.project.events.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

/**
 * 사용자 서비스
 * 
 * Domain Event Publisher와 Integration Event Publisher를 모두 사용하는 예시입니다.
 */
@Service
class UserService(
    // 도메인 이벤트 발행자 (프로세스 내부 처리)
    @Qualifier("domainEventPublisher")
    private val domainEventPublisher: DomainEventPublisher,
    
    // 통합 이벤트 발행자 (서비스 간 통신)
    @Qualifier("integrationEventPublisher")
    private val integrationEventPublisher: DomainEventPublisher
) {
    
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 사용자 생성
     * 
     * 사용자를 생성하고 관련 이벤트들을 발행합니다.
     */
    @Transactional
    fun createUser(request: CreateUserRequest): CreateUserResponse {
        log.info("Creating user with email: {}", request.email)
        
        // 1. 사용자 생성 로직 (실제로는 Repository 사용)
        val userId = UUID.randomUUID().toString()
        val user = User(
            id = userId,
            email = request.email,
            tenantId = request.tenantId,
            role = request.role,
            createdAt = Instant.now()
        )
        
        // 2. 통합 이벤트 발행 - 다른 서비스들에게 알림
        // (환영 이메일, 권한 설정, 분석 데이터 수집 등)
        integrationEventPublisher.publish(
            UserCreatedIntegrationEvent(
                id = UUID.randomUUID().toString(),
                occurredAt = Instant.now(),
                userId = user.id,
                email = user.email,
                tenantId = user.tenantId,
                userRole = user.role,
                createdAt = user.createdAt,
                metadata = mapOf(
                    "source" to "user-service",
                    "correlationId" to request.correlationId
                )
            )
        )
        
        log.info("User created successfully: {}", userId)
        
        return CreateUserResponse(
            userId = userId,
            email = user.email,
            createdAt = user.createdAt
        )
    }

    /**
     * 비밀번호 변경
     * 
     * 비밀번호를 변경하고 도메인 이벤트를 발행합니다.
     */
    @Transactional
    fun changePassword(request: ChangePasswordRequest) {
        log.info("Changing password for user: {}", request.userId)
        
        // 1. 비밀번호 변경 로직 (실제로는 암호화, 검증 등)
        val user = findUserById(request.userId)
        
        // 기존 비밀번호 검증
        if (!isValidPassword(user, request.currentPassword)) {
            throw InvalidPasswordException("Current password is incorrect")
        }
        
        // 새 비밀번호 설정
        updateUserPassword(user, request.newPassword)
        
        // 2. 도메인 이벤트 발행 - 같은 프로세스 내 처리
        // (세션 무효화, 보안 로그, 비밀번호 히스토리 등)
        domainEventPublisher.publish(
            UserPasswordChangedEvent(
                id = UUID.randomUUID().toString(),
                occurredAt = Instant.now(),
                userId = request.userId,
                changedAt = Instant.now(),
                isAdminAction = request.isAdminAction
            )
        )
        
        log.info("Password changed successfully for user: {}", request.userId)
    }

    /**
     * 사용자 프로필 업데이트
     * 
     * 프로필을 업데이트하고 관련 이벤트들을 발행합니다.
     */
    @Transactional
    fun updateProfile(request: UpdateProfileRequest) {
        log.info("Updating profile for user: {}", request.userId)
        
        val user = findUserById(request.userId)
        val previousValues = mapOf(
            "email" to user.email,
            "firstName" to user.firstName,
            "lastName" to user.lastName
        )
        
        // 프로필 업데이트
        val updatedUser = user.copy(
            email = request.email ?: user.email,
            firstName = request.firstName ?: user.firstName,
            lastName = request.lastName ?: user.lastName
        )
        
        val changedFields = mutableMapOf<String, Any>()
        if (request.email != null && request.email != user.email) {
            changedFields["email"] = request.email
        }
        if (request.firstName != null && request.firstName != user.firstName) {
            changedFields["firstName"] = request.firstName
        }
        if (request.lastName != null && request.lastName != user.lastName) {
            changedFields["lastName"] = request.lastName
        }
        
        // 도메인 이벤트 발행 (프로필 변경 내부 처리)
        if (changedFields.isNotEmpty()) {
            domainEventPublisher.publish(
                UserProfileUpdatedEvent(
                    id = UUID.randomUUID().toString(),
                    occurredAt = Instant.now(),
                    userId = request.userId,
                    changedFields = changedFields,
                    previousValues = previousValues
                )
            )
        }
        
        // 이메일 변경 시 통합 이벤트 발행 (다른 서비스들에게 알림)
        if (request.email != null && request.email != user.email) {
            integrationEventPublisher.publish(
                UserEmailChangedIntegrationEvent(
                    id = UUID.randomUUID().toString(),
                    occurredAt = Instant.now(),
                    userId = request.userId,
                    tenantId = user.tenantId,
                    previousEmail = user.email,
                    newEmail = request.email,
                    changedAt = Instant.now(),
                    verificationRequired = true
                )
            )
        }
        
        log.info("Profile updated successfully for user: {}", request.userId)
    }

    /**
     * 사용자 삭제
     * 
     * 사용자를 삭제하고 통합 이벤트를 발행합니다.
     */
    @Transactional
    fun deleteUser(request: DeleteUserRequest) {
        log.info("Deleting user: {}", request.userId)
        
        val user = findUserById(request.userId)
        
        // 사용자 삭제/비활성화
        if (request.hardDelete) {
            // 물리적 삭제
            physicallyDeleteUser(user)
        } else {
            // 논리적 삭제 (상태 변경)
            deactivateUser(user)
        }
        
        // 통합 이벤트 발행 - 다른 서비스들에게 삭제 알림
        integrationEventPublisher.publish(
            UserDeletedIntegrationEvent(
                id = UUID.randomUUID().toString(),
                occurredAt = Instant.now(),
                userId = user.id,
                email = user.email,
                tenantId = user.tenantId,
                deletedAt = Instant.now(),
                deletionReason = request.reason,
                isHardDelete = request.hardDelete
            )
        )
        
        log.info("User deleted successfully: {}", request.userId)
    }

    /**
     * 주문 생성 (예시)
     * 
     * 전자상거래 시나리오에서 주문 생성 시 통합 이벤트 발행 예시입니다.
     */
    @Transactional
    fun createOrder(request: CreateOrderRequest): CreateOrderResponse {
        log.info("Creating order for customer: {}", request.customerId)
        
        // 주문 생성 로직
        val orderId = UUID.randomUUID().toString()
        val order = Order(
            id = orderId,
            customerId = request.customerId,
            tenantId = request.tenantId,
            items = request.items,
            totalAmount = request.items.sumOf { it.totalPrice },
            shippingAddress = request.shippingAddress,
            createdAt = Instant.now()
        )
        
        // 통합 이벤트 발행 - 다른 서비스들에게 알림
        // (재고 차감, 결제 처리, 배송 준비, 알림 발송 등)
        integrationEventPublisher.publish(
            OrderCreatedIntegrationEvent(
                id = UUID.randomUUID().toString(),
                occurredAt = Instant.now(),
                orderId = order.id,
                customerId = order.customerId,
                tenantId = order.tenantId,
                totalAmount = order.totalAmount,
                currency = "USD",
                items = order.items,
                shippingAddress = order.shippingAddress,
                createdAt = order.createdAt
            )
        )
        
        log.info("Order created successfully: {}", orderId)
        
        return CreateOrderResponse(
            orderId = orderId,
            totalAmount = order.totalAmount,
            createdAt = order.createdAt
        )
    }
    
    // Private helper methods
    private fun findUserById(userId: String): User {
        // 실제로는 Repository에서 조회
        return User(
            id = userId,
            email = "user@example.com",
            tenantId = "tenant-1",
            role = "USER",
            firstName = "John",
            lastName = "Doe",
            createdAt = Instant.now()
        )
    }
    
    private fun isValidPassword(user: User, password: String): Boolean {
        // 실제로는 암호화된 비밀번호와 비교
        return true
    }
    
    private fun updateUserPassword(user: User, newPassword: String) {
        // 실제로는 암호화하여 저장
    }
    
    private fun physicallyDeleteUser(user: User) {
        // 실제로는 데이터베이스에서 완전 삭제
    }
    
    private fun deactivateUser(user: User) {
        // 실제로는 상태를 INACTIVE로 변경
    }
}

// Data classes for requests and responses
data class CreateUserRequest(
    val email: String,
    val tenantId: String,
    val role: String,
    val correlationId: String = UUID.randomUUID().toString()
)

data class CreateUserResponse(
    val userId: String,
    val email: String,
    val createdAt: Instant
)

data class ChangePasswordRequest(
    val userId: String,
    val currentPassword: String,
    val newPassword: String,
    val isAdminAction: Boolean = false
)

data class UpdateProfileRequest(
    val userId: String,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null
)

data class DeleteUserRequest(
    val userId: String,
    val reason: String? = null,
    val hardDelete: Boolean = false
)

data class CreateOrderRequest(
    val customerId: String,
    val tenantId: String,
    val items: List<OrderItem>,
    val shippingAddress: Address
)

data class CreateOrderResponse(
    val orderId: String,
    val totalAmount: Double,
    val createdAt: Instant
)

// Domain models
data class User(
    val id: String,
    val email: String,
    val tenantId: String,
    val role: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val createdAt: Instant
)

data class Order(
    val id: String,
    val customerId: String,
    val tenantId: String,
    val items: List<OrderItem>,
    val totalAmount: Double,
    val shippingAddress: Address,
    val createdAt: Instant
)

// Exceptions
class InvalidPasswordException(message: String) : RuntimeException(message)
