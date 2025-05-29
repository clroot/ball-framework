package io.clroot.ball.user.domain.model

import io.clroot.ball.domain.model.core.ValueObject
import java.time.Instant

/**
 * 사용자 메타데이터 (User Metadata)
 *
 * 사용자에 대한 추가 정보를 포함하는 값 객체
 */
data class UserMetadata(
    val displayName: String?,
    val lastLoginAt: Instant?,
    val passwordChangedAt: Instant?,
    val loginFailCount: Int,
    val profileImageUrl: String?,
    val emailVerified: Boolean
) : ValueObject {
    companion object {
        /**
         * 기본 메타데이터 생성
         *
         * @return 기본값으로 초기화된 UserMetadata 객체
         */
        fun default(): UserMetadata = UserMetadata(
            displayName = null,
            lastLoginAt = null,
            passwordChangedAt = null,
            loginFailCount = 0,
            profileImageUrl = null,
            emailVerified = false
        )
    }

    /**
     * 마지막 로그인 시간 업데이트
     *
     * @param loginAt 로그인 시간
     * @return 업데이트된 UserMetadata 객체
     */
    fun updateLastLogin(loginAt: Instant): UserMetadata = copy(
        lastLoginAt = loginAt,
        loginFailCount = 0
    )

    /**
     * 로그인 실패 횟수 증가
     *
     * @return 업데이트된 UserMetadata 객체
     */
    fun incrementLoginFailCount(): UserMetadata = copy(
        loginFailCount = loginFailCount + 1
    )

    /**
     * 비밀번호 변경 시간 업데이트
     *
     * @param changedAt 비밀번호 변경 시간
     * @return 업데이트된 UserMetadata 객체
     */
    fun updatePasswordChanged(changedAt: Instant): UserMetadata = copy(
        passwordChangedAt = changedAt
    )

    /**
     * 이메일 인증 상태 업데이트
     *
     * @param verified 인증 여부
     * @return 업데이트된 UserMetadata 객체
     */
    fun updateEmailVerified(verified: Boolean): UserMetadata = copy(
        emailVerified = verified
    )
}