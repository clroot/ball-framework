package io.clroot.ball.adapter.inbound.rest.extension

import arrow.core.Either
import io.clroot.ball.adapter.outbound.data.access.core.exception.EntityNotFoundException
import io.clroot.ball.application.ApplicationError
import org.springframework.http.ResponseEntity

/**
 * Either 타입을 ResponseEntity로 변환하는 확장 함수들
 * 
 * REST 컨트롤러에서 Application Layer의 Either 결과를
 * HTTP 응답으로 변환할 때 사용합니다.
 */

fun <T> Either<ApplicationError, T>.toResponseEntity(): ResponseEntity<T> =
    fold({ error -> throw error.toException() }, { data -> ResponseEntity.ok(data) })

fun <T> Either<ApplicationError, T?>.toResponseEntityWithNull(): ResponseEntity<T> =
    fold({ error -> throw error.toException() }, { data ->
        data?.let { ResponseEntity.ok(it) } ?: throw EntityNotFoundException("Resource not found")
    })

/**
 * ApplicationError를 적절한 예외로 변환
 * GlobalExceptionHandler에서 처리될 예외로 변환합니다.
 */
private fun ApplicationError.toException(): RuntimeException = when (this) {
    is ApplicationError.DomainError -> this.exception
    ApplicationError.NotFound -> EntityNotFoundException("Resource not found")
    is ApplicationError.SystemError -> RuntimeException(this.message, this.cause)
}