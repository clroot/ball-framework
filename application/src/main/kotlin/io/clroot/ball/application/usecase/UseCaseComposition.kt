package io.clroot.ball.application.usecase

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.either
import io.clroot.ball.application.ApplicationError
import io.clroot.ball.domain.exception.DomainException
import io.clroot.ball.domain.slf4j
import org.springframework.context.ApplicationEventPublisher
import org.springframework.transaction.annotation.Transactional

/**
 * 여러 UseCase를 조합하여 실행하는 추상 클래스
 *
 * 복잡한 비즈니스 플로우를 여러 UseCase의 조합으로 처리할 때 사용합니다.
 * 각 UseCase는 독립적으로 실행되며, 이전 UseCase의 결과를 다음 UseCase의 입력으로 사용할 수 있습니다.
 *
 * @param TCommand 전체 조합 명령 타입
 * @param TResult 최종 결과 타입
 */
abstract class UseCaseComposition<TCommand, TResult>(
    protected val applicationEventPublisher: ApplicationEventPublisher,
) {
    protected val log = slf4j()

    /**
     * 트랜잭션 내에서 UseCase 조합을 실행합니다.
     *
     * @param command 조합 명령
     * @return Either<ApplicationError, TResult> 조합 실행 결과
     */
    @Transactional
    open fun execute(command: TCommand): Either<ApplicationError, TResult> =
        either {
            try {
                val result = executeComposition(command).bind()
                result
            } catch (e: DomainException) {
                raise(ApplicationError.DomainError(e))
            } catch (e: Exception) {
                log.error("UseCase composition failed", e)
                raise(ApplicationError.SystemError("UseCase composition failed: ${e.message}", e))
            }
        }

    /**
     * 실제 UseCase 조합 로직을 구현합니다.
     *
     * @param command 조합 명령
     * @return Either<ApplicationError, TResult> 조합 실행 결과
     */
    protected abstract fun executeComposition(command: TCommand): Either<ApplicationError, TResult>

    /**
     * UseCase 실행 결과를 다음 UseCase의 입력으로 변환하는 헬퍼 함수
     */
    protected inline fun <T, R> Either<ApplicationError, T>.then(
        crossinline transform: (T) -> Either<ApplicationError, R>,
    ): Either<ApplicationError, R> = this.flatMap(transform)
}
