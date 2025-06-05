package io.clroot.ball.domain.exception

/**
 * 비즈니스 규칙 위반
 */
class BusinessRuleViolationException(message: String) : DomainException(message)