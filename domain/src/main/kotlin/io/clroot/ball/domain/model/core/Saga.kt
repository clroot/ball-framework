package io.clroot.ball.domain.model.core

import arrow.core.Either
import arrow.core.left
import arrow.core.right

/**
 * 세이가 (Saga)
 *
 * 분산 트랜잭션을 관리하는 세이가 패턴
 * 여러 단계의 트랜잭션을 조율하고 실패 시 보상 트랜잭션을 실행
 *
 * @param T 세이가 컨텍스트 타입
 * @param E 오류 타입
 */
interface Saga<T, E> {
    /**
     * 세이가 실행
     *
     * @param context 세이가 컨텍스트
     * @return 성공 시 Right(결과), 실패 시 Left(오류)
     */
    suspend fun execute(context: T): Either<E, T>

    /**
     * 두 세이가를 순차적으로 결합
     *
     * @param next 다음에 실행할 세이가
     * @return 두 세이가를 순차적으로 실행하는 새로운 세이가
     */
    fun andThen(next: Saga<T, E>): Saga<T, E> = ComposedSaga(this, next)
}

/**
 * 세이가 단계
 *
 * 세이가의 한 단계를 나타내는 클래스
 * 실행 함수와 보상 함수를 포함
 *
 * @param T 세이가 컨텍스트 타입
 * @param E 오류 타입
 * @param execute 실행 함수
 * @param compensate 보상 함수
 */
class SagaStep<T, E>(
    private val execute: suspend (T) -> Either<E, T>,
    private val compensate: suspend (T) -> Either<E, T>
) : Saga<T, E> {
    override suspend fun execute(context: T): Either<E, T> = execute(context)

    /**
     * 보상 트랜잭션 실행
     *
     * @param context 세이가 컨텍스트
     * @return 성공 시 Right(결과), 실패 시 Left(오류)
     */
    suspend fun compensate(context: T): Either<E, T> = compensate(context)
}

/**
 * 결합된 세이가
 *
 * 두 세이가를 순차적으로 결합한 세이가
 *
 * @param T 세이가 컨텍스트 타입
 * @param E 오류 타입
 * @param first 첫 번째 세이가
 * @param second 두 번째 세이가
 */
class ComposedSaga<T, E>(
    private val first: Saga<T, E>,
    private val second: Saga<T, E>
) : Saga<T, E> {
    override suspend fun execute(context: T): Either<E, T> {
        val firstResult = first.execute(context)
        return when (firstResult) {
            is Either.Right -> second.execute(firstResult.value)
            is Either.Left -> firstResult
        }
    }
}

/**
 * 세이가 오케스트레이터
 *
 * 여러 세이가 단계를 조율하고 실패 시 보상 트랜잭션을 실행하는 오케스트레이터
 *
 * @param T 세이가 컨텍스트 타입
 * @param E 오류 타입
 */
class SagaOrchestrator<T, E> {
    private val steps = mutableListOf<SagaStep<T, E>>()

    /**
     * 세이가 단계 추가
     *
     * @param step 추가할 세이가 단계
     * @return 현재 오케스트레이터
     */
    fun addStep(step: SagaStep<T, E>): SagaOrchestrator<T, E> {
        steps.add(step)
        return this
    }

    /**
     * 세이가 실행
     *
     * @param context 초기 컨텍스트
     * @return 성공 시 Right(결과), 실패 시 Left(오류)
     */
    suspend fun execute(context: T): Either<E, T> {
        var currentContext = context
        val executedSteps = mutableListOf<SagaStep<T, E>>()

        // 모든 단계 실행
        for (step in steps) {
            val result = step.execute(currentContext)
            when (result) {
                is Either.Right -> {
                    currentContext = result.value
                    executedSteps.add(step)
                }
                is Either.Left -> {
                    // 실패 시 보상 트랜잭션 실행
                    val compensationResult = compensate(executedSteps, currentContext)
                    return when (compensationResult) {
                        is Either.Right -> result // 원래 오류 반환
                        is Either.Left -> compensationResult // 보상 실패 오류 반환
                    }
                }
            }
        }

        return currentContext.right()
    }

    /**
     * 보상 트랜잭션 실행
     *
     * @param executedSteps 실행된 단계 목록
     * @param context 현재 컨텍스트
     * @return 성공 시 Right(결과), 실패 시 Left(오류)
     */
    private suspend fun compensate(executedSteps: List<SagaStep<T, E>>, context: T): Either<E, T> {
        var currentContext = context

        // 역순으로 보상 트랜잭션 실행
        for (step in executedSteps.reversed()) {
            val result = step.compensate(currentContext)
            when (result) {
                is Either.Right -> currentContext = result.value
                is Either.Left -> return result // 보상 실패 시 오류 반환
            }
        }

        return currentContext.right()
    }

}
