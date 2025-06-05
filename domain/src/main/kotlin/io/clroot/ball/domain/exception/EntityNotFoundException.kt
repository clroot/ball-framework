package io.clroot.ball.domain.exception

/**
 * 엔티티를 찾을 수 없음
 */
class EntityNotFoundException(message: String) : DomainException(message)