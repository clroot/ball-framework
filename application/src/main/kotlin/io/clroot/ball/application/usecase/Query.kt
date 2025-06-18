package io.clroot.ball.application.usecase

import arrow.core.Either
import arrow.core.raise.either
import io.clroot.ball.application.ApplicationError
import io.clroot.ball.domain.exception.ExternalSystemException
import org.springframework.transaction.annotation.Transactional

abstract class Query<TQuery, TResult> {
    @Transactional(readOnly = true)
    open fun execute(query: TQuery): Either<ApplicationError, TResult> =
        either {
            try {
                executeInternal(query)
            } catch (e: ExternalSystemException) {
                raise(ApplicationError.ExternalSystemError(e))
            } catch (e: io.clroot.ball.domain.exception.DomainException) {
                raise(ApplicationError.DomainError(e))
            } catch (e: Exception) {
                raise(ApplicationError.SystemError(e.message ?: "Unknown error", e))
            }
        }

    protected abstract fun executeInternal(query: TQuery): TResult
}
