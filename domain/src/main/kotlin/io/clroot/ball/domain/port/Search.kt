package io.clroot.ball.domain.port

import io.clroot.ball.domain.model.paging.Page
import io.clroot.ball.domain.model.paging.PageRequest

interface Search<T, R> {
    fun search(criteria: T): List<R>

    fun search(
        criteria: T,
        pageRequest: PageRequest,
    ): Page<R>
}
