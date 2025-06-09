package io.clroot.ball.adapter.outbound.data.access.core.exception

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
 *     throw DatabaseException("Database error occurred", e)
 * }
 * ```
 */
class DatabaseException(message: String, cause: Throwable? = null) : PersistenceException(message, cause)