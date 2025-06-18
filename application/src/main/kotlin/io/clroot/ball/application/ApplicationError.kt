package io.clroot.ball.application

import io.clroot.ball.domain.exception.DomainException
import io.clroot.ball.domain.exception.ExternalSystemException

sealed class ApplicationError {
    data class DomainError(val exception: DomainException) : ApplicationError()
    data class ExternalSystemError(val exception: ExternalSystemException) : ApplicationError()
    data class SystemError(val message: String, val cause: Throwable?) : ApplicationError()
    data object NotFound : ApplicationError()
}
