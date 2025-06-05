package io.clroot.ball.domain.exception

/**
 * 도메인 계층의 기본 예외
 */
abstract class DomainException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * 잘못된 ID 형식
 */
class InvalidIdException(message: String) : DomainException(message)

/**
 * 값 객체 유효성 검증 실패
 */
class ValidationException(message: String) : DomainException(message)

/**
 * 엔티티를 찾을 수 없음
 */
class EntityNotFoundException(message: String) : DomainException(message)

/**
 * 비즈니스 규칙 위반
 */
class BusinessRuleViolationException(message: String) : DomainException(message)

/**
 * 명세(Specification) 조건 불만족
 */
class SpecificationNotSatisfiedException(message: String) : DomainException(message)
