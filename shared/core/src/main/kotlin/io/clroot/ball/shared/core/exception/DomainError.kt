package io.clroot.ball.shared.core.exception

/**
 * 도메인 오류를 나타내는 sealed 클래스
 * 함수형 오류 처리를 위해 Either<DomainError, T>와 함께 사용
 */
sealed class DomainError {
    /**
     * 인증 오류
     */
    data class AuthenticationError(val exception: AuthenticationException) : DomainError() {
        constructor(message: String = "Authentication failed", cause: Throwable? = null) : 
            this(AuthenticationException(message, cause))
    }

    /**
     * 권한 오류
     */
    data class AuthorizationError(val exception: AuthorizationException) : DomainError() {
        constructor(message: String = "Authorization failed", cause: Throwable? = null) : 
            this(AuthorizationException(message, cause))
    }

    /**
     * 검증 오류
     */
    data class ValidationError(val exception: ValidationException) : DomainError() {
        constructor(message: String = "Validation failed", errors: Map<String, String> = emptyMap(), cause: Throwable? = null) : 
            this(ValidationException(message, cause, errors))
    }

    /**
     * 엔티티 찾을 수 없음 오류
     */
    data class EntityNotFoundError(val exception: EntityNotFoundException) : DomainError() {
        constructor(entityName: String, idValue: Any, cause: Throwable? = null) : 
            this(EntityNotFoundException(entityName, idValue, cause))
    }

    /**
     * 중복 엔티티 오류
     */
    data class DuplicateEntityError(val exception: DuplicateEntityException) : DomainError() {
        constructor(entityName: String, fieldName: String, fieldValue: Any, cause: Throwable? = null) : 
            this(DuplicateEntityException(entityName, fieldName, fieldValue, cause))
    }

    /**
     * 비즈니스 규칙 위반 오류
     */
    data class BusinessRuleViolationError(val exception: BusinessRuleViolationException) : DomainError() {
        constructor(message: String, cause: Throwable? = null) : 
            this(BusinessRuleViolationException(message, cause))
    }

    /**
     * 외부 서비스 오류
     */
    data class ExternalServiceError(val exception: ExternalServiceException) : DomainError() {
        constructor(serviceName: String, message: String = "External service error", cause: Throwable? = null) : 
            this(ExternalServiceException(serviceName, message, cause))
    }

    /**
     * 메시징 오류
     */
    data class MessagingError(
        val message: String,
        val cause: Throwable? = null,
        val messageId: String? = null,
        val topic: String? = null
    ) : DomainError()

    /**
     * 일반 오류
     */
    data class GenericError(
        val message: String,
        val cause: Throwable? = null
    ) : DomainError()
}