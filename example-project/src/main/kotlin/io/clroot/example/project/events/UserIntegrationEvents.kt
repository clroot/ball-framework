package io.clroot.example.project.events

import io.clroot.ball.domain.event.DomainEvent
import java.time.Instant

/**
 * 통합 이벤트 예시
 * 
 * 서비스 간 통합을 위한 이벤트들입니다.
 * 다른 서비스들이 구독하여 처리합니다.
 */

/**
 * 사용자 생성 통합 이벤트
 * 
 * 새로운 사용자가 생성되었을 때 다른 서비스들에게 알리는 통합 이벤트입니다.
 * 
 * 구독 서비스 예시:
 * - 알림 서비스: 환영 이메일 발송
 * - 분석 서비스: 사용자 생성 통계 업데이트
 * - 권한 서비스: 기본 권한 할당
 * - 감사 서비스: 계정 생성 로그 기록
 */
data class UserCreatedIntegrationEvent(
    override val id: String,
    override val type: String = "UserCreated",
    override val occurredAt: Instant,
    val version: String = "1.0",
    
    // 비즈니스 데이터
    val userId: String,
    val email: String,
    val tenantId: String,
    val userRole: String,
    val createdAt: Instant,
    val metadata: Map<String, Any> = emptyMap()
) : DomainEvent

/**
 * 사용자 삭제 통합 이벤트
 * 
 * 사용자가 삭제(또는 비활성화)되었을 때 다른 서비스들에게 알리는 통합 이벤트입니다.
 * 
 * 구독 서비스 예시:
 * - 데이터 서비스: 사용자 관련 데이터 정리
 * - 권한 서비스: 권한 정보 삭제
 * - 알림 서비스: 계정 삭제 확인 이메일 발송
 * - 감사 서비스: 계정 삭제 로그 기록
 */
data class UserDeletedIntegrationEvent(
    override val id: String,
    override val type: String = "UserDeleted",
    override val occurredAt: Instant,
    val version: String = "1.0",
    
    // 비즈니스 데이터
    val userId: String,
    val email: String,
    val tenantId: String,
    val deletedAt: Instant,
    val deletionReason: String?,
    val isHardDelete: Boolean = false
) : DomainEvent

/**
 * 사용자 이메일 변경 통합 이벤트
 * 
 * 사용자의 이메일 주소가 변경되었을 때 다른 서비스들에게 알리는 통합 이벤트입니다.
 * 
 * 구독 서비스 예시:
 * - 알림 서비스: 새로운 이메일로 변경 확인 발송
 * - 권한 서비스: 이메일 기반 권한 업데이트
 * - 감사 서비스: 이메일 변경 로그 기록
 */
data class UserEmailChangedIntegrationEvent(
    override val id: String,
    override val type: String = "UserEmailChanged",
    override val occurredAt: Instant,
    val version: String = "1.0",
    
    // 비즈니스 데이터
    val userId: String,
    val tenantId: String,
    val previousEmail: String,
    val newEmail: String,
    val changedAt: Instant,
    val verificationRequired: Boolean = true
) : DomainEvent

/**
 * 주문 생성 통합 이벤트 (예시)
 * 
 * 전자상거래 시나리오에서 주문이 생성되었을 때의 통합 이벤트 예시입니다.
 */
data class OrderCreatedIntegrationEvent(
    override val id: String,
    override val type: String = "OrderCreated",
    override val occurredAt: Instant,
    val version: String = "1.0",
    
    // 비즈니스 데이터
    val orderId: String,
    val customerId: String,
    val tenantId: String,
    val totalAmount: Double,
    val currency: String,
    val items: List<OrderItem>,
    val shippingAddress: Address,
    val createdAt: Instant
) : DomainEvent

/**
 * 주문 아이템 정보
 */
data class OrderItem(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double
)

/**
 * 주소 정보
 */
data class Address(
    val street: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String
)
