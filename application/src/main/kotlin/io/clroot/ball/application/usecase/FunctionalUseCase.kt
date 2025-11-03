package io.clroot.ball.application.usecase

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import io.clroot.ball.application.ApplicationError
import io.clroot.ball.domain.exception.ExternalSystemException
import org.springframework.context.ApplicationEventPublisher
import kotlin.coroutines.cancellation.CancellationException

/**
 * Raise 컨텍스트를 활용해 사용자 정의 오류 타입을 선언적으로 전파할 수 있는 UseCase.
 *
 * @param TCommand 실행 명령 타입
 * @param TResult 성공 결과 타입
 * @param TError 구현체가 다루는 오류 타입 (Left 서명)
 * @param applicationEventPublisher 도메인 이벤트 발행용 스프링 이벤트 퍼블리셔
 *
 * ### Example:
 * ```kotlin
 * class ConfirmOrderUseCase(
 *     publisher: ApplicationEventPublisher,
 *     private val orderReader: OrderReader,
 * ) : FunctionalUseCase<ConfirmOrderCommand, ConfirmOrderResult, ConfirmOrderError>(publisher) {
 *     override fun Raise<ConfirmOrderError>.executeFunctional(command: ConfirmOrderCommand): ConfirmOrderResult {
 *         val order = orderReader.find(command.id) ?: raise(ConfirmOrderError.NotFound)
 *         ensure(!order.isConfirmed) { ConfirmOrderError.AlreadyConfirmed }
 *         return order.confirm().also { it.publishEvents() }
 *     }
 *
 *     override fun mapError(error: ConfirmOrderError): ApplicationError =
 *         when (error) {
 *             ConfirmOrderError.NotFound -> ApplicationError.NotFound
 *             ConfirmOrderError.AlreadyConfirmed ->
 *                 ApplicationError.DomainError(BusinessRuleException.alreadyDone("주문 확정"))
 *         }
 * }
 * ```
 */
abstract class FunctionalUseCase<TCommand, TResult, TError>(
    applicationEventPublisher: ApplicationEventPublisher,
) : UseCase<TCommand, TResult>(applicationEventPublisher) {
    final override fun execute(command: TCommand): Either<ApplicationError, TResult> =
        either {
            try {
                val domainResult: Either<TError, TResult> =
                    either {
                        executeFunctional(command)
                    }
                when (domainResult) {
                    is Either.Left -> raise(mapError(domainResult.value))
                    is Either.Right -> domainResult.value
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ExternalSystemException) {
                log.warn("External system error: ${e.message}", e)
                raise(ApplicationError.ExternalSystemError(e))
            } catch (e: io.clroot.ball.domain.exception.DomainException) {
                raise(ApplicationError.DomainError(e))
            } catch (e: Exception) {
                log.error("Unhandled exception", e)
                raise(ApplicationError.SystemError(e.message ?: "Unknown error", e))
            }
        }

    /**
     * 기존 [UseCase.executeInternal] API와의 충돌을 막기 위해 막아 둔다.
     * 구현체는 [executeFunctional]만 재정의하면 된다.
     */
    final override fun executeInternal(
        @Suppress("UNUSED_PARAMETER") command: TCommand,
    ): TResult {
        error("FunctionalUseCase는 executeInternal 대신 executeFunctional을 구현해야 합니다.")
    }

    /**
     * Raise 컨텍스트 안에서 명시적으로 [TError]를 전파할 수 있는 핵심 훅.
     */
    protected abstract fun Raise<TError>.executeFunctional(command: TCommand): TResult

    /**
     * 사용자 정의 오류를 [ApplicationError]로 변환한다.
     */
    protected abstract fun mapError(error: TError): ApplicationError
}
