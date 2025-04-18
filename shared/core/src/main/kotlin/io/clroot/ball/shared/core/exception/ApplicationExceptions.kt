package io.clroot.ball.shared.core.exception

/**
 * 애플리케이션 예외의 기본 클래스
 */
abstract class ApplicationException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * 인증 예외
 */
class AuthenticationException(
    message: String = "Authentication failed",
    cause: Throwable? = null
) : ApplicationException(message, cause)

/**
 * 권한 예외
 */
class AuthorizationException(
    message: String = "Authorization failed",
    cause: Throwable? = null
) : ApplicationException(message, cause)

/**
 * 검증 예외
 */
class ValidationException(
    message: String = "Validation failed",
    cause: Throwable? = null,
    val errors: Map<String, String> = emptyMap()
) : ApplicationException(message, cause)

/**
 * 엔티티 찾을 수 없음 예외
 */
class EntityNotFoundException(
    entityName: String,
    idValue: Any,
    cause: Throwable? = null
) : ApplicationException("$entityName with id $idValue not found", cause)

/**
 * 중복 엔티티 예외
 */
class DuplicateEntityException(
    entityName: String,
    fieldName: String,
    fieldValue: Any,
    cause: Throwable? = null
) : ApplicationException("$entityName with $fieldName $fieldValue already exists", cause)

/**
 * 비즈니스 규칙 위반 예외
 */
class BusinessRuleViolationException(
    message: String,
    cause: Throwable? = null
) : ApplicationException(message, cause)

/**
 * 외부 서비스 예외
 */
class ExternalServiceException(
    serviceName: String,
    message: String = "External service error",
    cause: Throwable? = null
) : ApplicationException("$serviceName error: $message", cause)
