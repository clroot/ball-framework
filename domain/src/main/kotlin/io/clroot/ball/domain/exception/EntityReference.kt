package io.clroot.ball.domain.exception

import kotlin.reflect.KClass

/**
 * 예외와 연관된 도메인 엔티티 정보
 */
data class EntityReference(
    val type: KClass<*>,
    val id: String? = null,
) {
    val typeName: String = type.simpleName ?: type.qualifiedName ?: type.toString()

    fun asMetadata(): Map<String, Any?> =
        buildMap {
            put("entityType", typeName)
            id?.let { put("entityId", it) }
        }
}
