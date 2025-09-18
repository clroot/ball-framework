package io.clroot.ball.domain.port

import kotlin.reflect.KProperty1

interface SearchCriteria {
    fun isEmpty(): Boolean =
        this::class
            .members
            .filterIsInstance<KProperty1<*, *>>()
            .filter { it.returnType.isMarkedNullable }
            .all {
                if (it.call(this) is SearchCriteria?) {
                    (it.call(this) as SearchCriteria?)?.isEmpty() ?: true
                } else {
                    it.call(this) == null
                }
            }
}
