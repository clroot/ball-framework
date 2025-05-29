package io.clroot.example.project.events

import io.clroot.ball.domain.event.DomainEvent
import java.time.Instant

/**
 * 도메인 이벤트 예시
 * 
 * 도메인 내부 처리를 위한 이벤트들입니다.
 * 같은 프로세스 내에서 즉시 처리됩니다.
 */

/**
 * 사용자 비밀번호 변경 도메인 이벤트
 * 
 * 사용자가 비밀번호를 변경했을 때 발생하는 도메인 이벤트입니다.
 * 이 이벤트는 다음과 같은 도메인 로직을 트리거합니다:
 * - 비밀번호 히스토리 저장
 * - 보안 이벤트 로깅
 * - 사용자 세션 무효화
 */
data class UserPasswordChangedEvent(
    override val id: String,
    override val type: String = "UserPasswordChanged",
    override val occurredAt: Instant,
    val userId: String,
    val changedAt: Instant,
    val isAdminAction: Boolean = false
) : DomainEvent

/**
 * 사용자 프로필 업데이트 도메인 이벤트
 * 
 * 사용자 프로필 정보가 변경되었을 때 발생하는 도메인 이벤트입니다.
 */
data class UserProfileUpdatedEvent(
    override val id: String,
    override val type: String = "UserProfileUpdated",
    override val occurredAt: Instant,
    val userId: String,
    val changedFields: Map<String, Any?>,
    val previousValues: Map<String, Any?>
) : DomainEvent

/**
 * 사용자 상태 변경 도메인 이벤트
 * 
 * 사용자의 상태(활성/비활성/잠금 등)가 변경되었을 때 발생하는 도메인 이벤트입니다.
 */
data class UserStatusChangedEvent(
    override val id: String,
    override val type: String = "UserStatusChanged",
    override val occurredAt: Instant,
    val userId: String,
    val fromStatus: String,
    val toStatus: String,
    val reason: String?
) : DomainEvent
