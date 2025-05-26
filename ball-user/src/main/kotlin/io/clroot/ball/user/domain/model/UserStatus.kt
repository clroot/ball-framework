package io.clroot.ball.user.domain.model

/**
 * 사용자 상태 (User Status)
 *
 * 사용자의 현재 상태를 나타내는 열거형
 */
enum class UserStatus {
    PENDING,    // 활성화 대기 중
    ACTIVE,     // 활성화됨
    SUSPENDED,  // 일시 정지됨
    INACTIVE,   // 비활성화됨
    DELETED     // 삭제됨
}