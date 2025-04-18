package io.clroot.ball.application.service

import arrow.core.Either
import io.clroot.ball.shared.core.exception.ApplicationException

/**
 * 트랜잭션 관리자 인터페이스
 */
interface TransactionManager {
    /**
     * 트랜잭션 내에서 함수 실행
     *
     * @param block 트랜잭션 내에서 실행할 함수
     * @return 함수 실행 결과
     */
    fun <T> withTransaction(block: () -> T): T

    /**
     * 새로운 트랜잭션 내에서 함수 실행
     *
     * @param block 새로운 트랜잭션 내에서 실행할 함수
     * @return 함수 실행 결과
     */
    fun <T> withNewTransaction(block: () -> T): T

    fun <E : Any, T> withTransaction(block: () -> Either<E, T>): Either<E, T> {
        return Either.catch {
            withNewTransaction {
                block().fold({ error -> throw TransactionWrappedException(error) }, { it })
            }
        }.fold(
            { error ->
                if (error is TransactionWrappedException) {
                    @Suppress("UNCHECKED_CAST")
                    Either.Left(error.wrappedError as E)
                } else {
                    throw error
                }
            },
            { Either.Right(it) }
        )
    }


    /**
     * Either 에러를 래핑하기 위한 내부 예외
     */
    private class TransactionWrappedException(val wrappedError: Any) : ApplicationException(wrappedError.toString())
}