package io.clroot.ball.application.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import io.clroot.ball.application.ApplicationError
import io.clroot.ball.domain.slf4j
import org.springframework.context.ApplicationEventPublisher
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.ForkJoinPool

/**
 * 여러 UseCase를 병렬로 실행하는 조합 클래스
 *
 * 독립적인 UseCase들을 병렬로 실행하여 성능을 향상시킬 때 사용합니다.
 * 모든 UseCase가 성공해야 전체가 성공으로 간주됩니다.
 *
 * @param TCommand 전체 조합 명령 타입
 * @param TResult 최종 결과 타입
 */
abstract class ParallelUseCaseComposition<TCommand, TResult>(
    protected val applicationEventPublisher: ApplicationEventPublisher,
    protected val executor: Executor = ForkJoinPool.commonPool(),
) {
    protected val log = slf4j()

    /**
     * 트랜잭션 내에서 UseCase들을 병렬 실행합니다.
     * 
     * @param command 조합 명령
     * @return Either<ApplicationError, TResult> 조합 실행 결과
     */
    @Transactional
    open fun execute(command: TCommand): Either<ApplicationError, TResult> =
        either {
            try {
                val result = executeParallel(command).bind()
                result
            } catch (e: Exception) {
                log.error("Parallel UseCase composition failed", e)
                raise(ApplicationError.SystemError("Parallel UseCase composition failed: ${e.message}", e))
            }
        }

    /**
     * 실제 병렬 UseCase 조합 로직을 구현합니다.
     * 
     * @param command 조합 명령
     * @return Either<ApplicationError, TResult> 조합 실행 결과
     */
    protected abstract fun executeParallel(command: TCommand): Either<ApplicationError, TResult>

    /**
     * 두 개의 UseCase를 병렬로 실행하는 헬퍼 함수
     */
    protected fun <T1, T2> runInParallel(
        task1: () -> Either<ApplicationError, T1>,
        task2: () -> Either<ApplicationError, T2>
    ): Either<ApplicationError, Pair<T1, T2>> = either {
        val future1 = CompletableFuture.supplyAsync({ task1() }, executor)
        val future2 = CompletableFuture.supplyAsync({ task2() }, executor)
        
        val result1 = future1.get().bind()
        val result2 = future2.get().bind()
        
        Pair(result1, result2)
    }

    /**
     * 세 개의 UseCase를 병렬로 실행하는 헬퍼 함수
     */
    protected fun <T1, T2, T3> runInParallel(
        task1: () -> Either<ApplicationError, T1>,
        task2: () -> Either<ApplicationError, T2>,
        task3: () -> Either<ApplicationError, T3>
    ): Either<ApplicationError, Triple<T1, T2, T3>> = either {
        val future1 = CompletableFuture.supplyAsync({ task1() }, executor)
        val future2 = CompletableFuture.supplyAsync({ task2() }, executor)
        val future3 = CompletableFuture.supplyAsync({ task3() }, executor)
        
        val result1 = future1.get().bind()
        val result2 = future2.get().bind()
        val result3 = future3.get().bind()
        
        Triple(result1, result2, result3)
    }

    /**
     * 여러 UseCase를 병렬로 실행하는 헬퍼 함수
     */
    protected fun <T> runInParallel(
        tasks: List<() -> Either<ApplicationError, T>>
    ): Either<ApplicationError, List<T>> = either {
        val futures = tasks.map { task ->
            CompletableFuture.supplyAsync({ task() }, executor)
        }
        
        futures.map { it.get().bind() }
    }

    /**
     * CompletableFuture를 Either로 변환하는 헬퍼 함수
     */
    protected fun <T> CompletableFuture<Either<ApplicationError, T>>.awaitEither(): Either<ApplicationError, T> =
        try {
            this.get()
        } catch (e: Exception) {
            ApplicationError.SystemError("Future execution failed: ${e.message}", e).left()
        }
}