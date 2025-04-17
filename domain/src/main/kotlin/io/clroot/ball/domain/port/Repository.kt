package io.clroot.ball.domain.port

import arrow.core.Either
import arrow.core.Option
import io.clroot.ball.domain.model.core.EntityBase

interface Repository<T : EntityBase<ID>, ID : Any> {
    fun findById(id: ID): Option<T>

    fun save(entity: T): Either<PersistenceError, T>
}

sealed class PersistenceError {
    /**
     * 데이터베이스 오류
     */
    data class DatabaseError(val cause: Throwable) : PersistenceError()

    /**
     * 엔티티를 찾을 수 없음
     */
    data object EntityNotFound : PersistenceError()

    /**
     * 중복된 엔티티
     */
    data object DuplicateEntity : PersistenceError()
}
