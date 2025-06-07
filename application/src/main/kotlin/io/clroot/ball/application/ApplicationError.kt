package io.clroot.ball.application

import io.clroot.ball.domain.exception.DomainException

sealed class ApplicationError {
    data class DomainError(val exception: DomainException) : ApplicationError()
    data class SystemError(val message: String, val cause: Throwable?) : ApplicationError()
    data object NotFound : ApplicationError()
}
