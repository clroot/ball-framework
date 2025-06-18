package io.clroot.ball.domain.exception

/**
 * 외부 시스템 연동 중 발생하는 예외의 기본 클래스
 * 
 * 외부 API, 결제 게이트웨이, 메시징 시스템 등과의 연동에서
 * 발생하는 모든 예외의 부모 클래스입니다.
 */
abstract class ExternalSystemException(
    message: String,
    cause: Throwable? = null
) : DomainException(message, cause)