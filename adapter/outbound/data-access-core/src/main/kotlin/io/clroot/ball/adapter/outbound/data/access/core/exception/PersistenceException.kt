package io.clroot.ball.adapter.outbound.data.access.core.exception

import io.clroot.ball.domain.exception.DomainException

/**
 * 영속성 계층에서 발생하는 기본 예외
 *
 * Repository 인터페이스의 작업 중 발생할 수 있는 모든 영속성 관련 예외의 기본 클래스입니다.
 *
 * @since 2.0
 */
abstract class PersistenceException(message: String, cause: Throwable? = null) : DomainException(message, cause)