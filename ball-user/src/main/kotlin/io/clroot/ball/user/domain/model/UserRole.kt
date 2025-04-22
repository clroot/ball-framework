package io.clroot.ball.user.domain.model

/**
 * 사용자 역할 (User Role)
 *
 * 사용자의 역할을 나타내는 열거형
 */
enum class UserRole {
    USER,    // 일반 사용자
    ADMIN,   // 관리자
    SYSTEM,  // 시스템 계정
    GUEST    // 게스트
}