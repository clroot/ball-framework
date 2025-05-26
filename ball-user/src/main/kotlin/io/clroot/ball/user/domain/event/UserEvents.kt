package io.clroot.ball.user.domain.event

import io.clroot.ball.domain.event.DomainEventBase
import io.clroot.ball.user.domain.model.UserStatus

/**
 * 사용자 생성 이벤트 (User Created Event)
 *
 * 새로운 사용자가 생성되었을 때 발행되는 이벤트
 *
 * @param userId 사용자 ID
 * @param username 사용자 이름
 * @param email 이메일
 */
class UserCreatedEvent(
    val userId: String,
    val username: String,
    val email: String
) : DomainEventBase()

/**
 * 사용자 상태 변경 이벤트 (User Status Changed Event)
 *
 * 사용자의 상태가 변경되었을 때 발행되는 이벤트
 *
 * @param userId 사용자 ID
 * @param oldStatus 이전 상태
 * @param newStatus 새로운 상태
 */
class UserStatusChangedEvent(
    val userId: String,
    val oldStatus: UserStatus,
    val newStatus: UserStatus
) : DomainEventBase()

/**
 * 사용자 역할 추가 이벤트 (User Role Added Event)
 *
 * 사용자에게 새로운 역할이 추가되었을 때 발행되는 이벤트
 *
 * @param userId 사용자 ID
 * @param role 추가된 역할
 */
class UserRoleAddedEvent(
    val userId: String,
    val role: String
) : DomainEventBase()

/**
 * 사용자 역할 제거 이벤트 (User Role Removed Event)
 *
 * 사용자에게서 역할이 제거되었을 때 발행되는 이벤트
 *
 * @param userId 사용자 ID
 * @param role 제거된 역할
 */
class UserRoleRemovedEvent(
    val userId: String,
    val role: String
) : DomainEventBase()