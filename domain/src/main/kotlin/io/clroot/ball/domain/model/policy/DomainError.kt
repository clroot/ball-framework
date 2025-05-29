package io.clroot.ball.domain.model.policy

sealed class DomainError {
    /**
     * 유효성 검증 오류
     */
    data class ValidationError(val message: String) : DomainError()

    /**
     * 비즈니스 규칙 위반 오류
     */
    data class BusinessRuleViolation(val message: String) : DomainError()

    /**
     * 권한 오류
     */
    data class AuthorizationError(val message: String) : DomainError()
}