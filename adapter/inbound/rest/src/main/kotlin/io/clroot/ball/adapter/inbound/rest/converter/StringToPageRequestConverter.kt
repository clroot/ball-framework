package io.clroot.ball.adapter.inbound.rest.converter

import io.clroot.ball.domain.model.core.paging.Order
import io.clroot.ball.domain.model.core.paging.PageRequest
import io.clroot.ball.domain.model.core.paging.Sort
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class StringToPageRequestConverter : Converter<String, PageRequest> {
    override fun convert(source: String): PageRequest {
        val parts = source.split(",").map { it.trim() }

        val page = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val size = parts.getOrNull(1)?.toIntOrNull() ?: 20
        val sortParts = parts.drop(2)

        val sort = parseSortFromParts(sortParts)

        return PageRequest(page, size, sort)
    }

    private fun parseSortFromParts(sortParts: List<String>): Sort {
        if (sortParts.isEmpty()) return Sort.unsorted()

        val orders = sortParts.mapNotNull { part ->
            when {
                part.contains(":desc") -> Order.desc(part.substringBefore(":desc"))
                part.contains(":asc") -> Order.asc(part.substringBefore(":asc"))
                part.isNotBlank() -> Order.asc(part)
                else -> null
            }
        }

        return Sort(orders)
    }
}