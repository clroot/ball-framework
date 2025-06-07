package io.clroot.ball.adapter.inbound.rest

import arrow.core.Either
import io.clroot.ball.application.ApplicationError
import io.clroot.ball.domain.exception.EntityNotFoundException
import org.springframework.http.ResponseEntity

fun <T> Either<ApplicationError, T>.toResponseEntity(): ResponseEntity<T> =
    fold({ error -> throw error.toException() }, { data -> ResponseEntity.ok(data) })

fun <T> Either<ApplicationError, T?>.toResponseEntityWithNull(): ResponseEntity<T> =
    fold({ error -> throw error.toException() }, { data ->
        data?.let { ResponseEntity.ok(it) } ?: throw EntityNotFoundException("Resource not found")
    })

private fun ApplicationError.toException(): RuntimeException = when (this) {
    is ApplicationError.DomainError -> this.exception
    ApplicationError.NotFound -> EntityNotFoundException("Resource not found")
    is ApplicationError.SystemError -> RuntimeException(this.message, this.cause)
}