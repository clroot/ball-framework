package io.clroot.ball.domain.exception

import io.clroot.ball.domain.model.EntityBase
import kotlin.reflect.KClass

class DomainStateException(
    message: String,
    val entityType: String? = null,
    val entityId: String? = null,
    cause: Throwable? = null,
) : DomainException(message, cause) {
    companion object {
        fun entityNotFound(
            entityType: KClass<*>,
            query: Pair<String, Any?>? = null,
        ) = DomainStateException("${entityType.simpleName}을(를) 찾을 수 없습니다: $query", entityType.simpleName)

        fun <T : EntityBase<*>> entityAlreadyExists(
            entity: T,
            id: String,
        ) = DomainStateException("${entity::class.simpleName}이(가) 이미 존재합니다: $id", entity::class.simpleName, id)

        fun <T : EntityBase<*>> invalidState(
            entity: T,
            currentState: String,
            expectedState: String,
        ) = DomainStateException(
            "잘못된 상태 전환입니다: $currentState -> $expectedState",
            entity::class.simpleName,
            null,
        )
    }

    override fun toString(): String = "DomainStateException(message='$message', entityType='$entityType', entityId='$entityId')"
}
