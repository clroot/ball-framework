package io.clroot.ball.adapter.outbound.data.access.core.exception

import io.clroot.ball.domain.exception.DomainErrorCodes
import io.clroot.ball.domain.exception.ErrorType

/**
 * 데이터베이스 수준에서 발생한 오류
 *
 * 연결 실패, SQL 오류, 트랜잭션 오류 등 데이터베이스 시스템에서
 * 발생하는 모든 예외 상황을 나타냅니다.
 *
 * @since 2.0
 *
 * @sample
 * ```kotlin
 * // JPA 구현체에서 사용 예시
 * try {
 *     entityManager.persist(entity)
 * } catch (e: SQLException) {
 *     throw DatabaseException("데이터베이스 오류가 발생했습니다", e)
 * }
 * ```
 */
class DatabaseException(
    message: String,
    cause: Throwable? = null,
    metadata: Map<String, Any?> = emptyMap(),
) : PersistenceException(
        message = message,
        errorType = ErrorType.EXTERNAL_ERROR,
        code = DomainErrorCodes.PERSISTENCE_DATABASE_ERROR,
        messageKey = "persistence.database_error",
        metadata = metadata,
        cause = cause,
    )
