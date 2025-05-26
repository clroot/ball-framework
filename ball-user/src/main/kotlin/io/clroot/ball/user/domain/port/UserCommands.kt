package io.clroot.ball.user.domain.port

import io.clroot.ball.shared.attribute.AttributeStore
import io.clroot.ball.user.domain.model.UserMetadata
import io.clroot.ball.user.domain.model.UserRole

/**
 * 사용자 등록 명령 (Register User Command)
 *
 * 새로운 사용자를 등록하기 위한 명령 객체
 *
 * @param username 사용자 이름
 * @param email 이메일
 * @param password 비밀번호
 * @param roles 역할 집합
 * @param metadata 메타데이터
 * @param attributes 속성 저장소
 */
data class RegisterUserCommand(
    val username: String,
    val email: String,
    val password: String,
    val roles: Set<UserRole> = setOf(UserRole.USER),
    val metadata: UserMetadata = UserMetadata.default(),
    val attributes: AttributeStore = AttributeStore.empty()
)

/**
 * 사용자 인증 명령 (Authenticate User Command)
 *
 * 사용자 인증을 위한 명령 객체
 *
 * @param usernameOrEmail 사용자 이름 또는 이메일
 * @param password 비밀번호
 */
data class AuthenticateUserCommand(
    val usernameOrEmail: String,
    val password: String
)

/**
 * 비밀번호 변경 명령 (Change Password Command)
 *
 * 사용자 비밀번호 변경을 위한 명령 객체
 *
 * @param userId 사용자 ID
 * @param currentPassword 현재 비밀번호
 * @param newPassword 새 비밀번호
 */
data class ChangePasswordCommand(
    val userId: String,
    val currentPassword: String,
    val newPassword: String
)

/**
 * 사용자 상태 변경 명령 (Change User Status Command)
 *
 * 사용자 상태 변경을 위한 명령 객체
 *
 * @param userId 사용자 ID
 * @param newStatus 새로운 상태
 */
data class ChangeUserStatusCommand(
    val userId: String,
    val newStatus: String
)