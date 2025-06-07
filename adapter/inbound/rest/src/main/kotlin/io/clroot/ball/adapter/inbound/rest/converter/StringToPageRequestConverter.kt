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
        val size = parts.getOrNull(1)?.toIntOrNull() ?: PageRequest.DEFAULT_SIZE
        val sortParts = parts.drop(2)

        val sort = parseSortFromParts(sortParts)

        return PageRequest(page, size, sort)
    }

    private fun parseSortFromParts(sortParts: List<String>): Sort {
        if (sortParts.isEmpty()) return Sort.unsorted()

        val orders = sortParts.mapNotNull { part ->
            val trimmedPart = part.trim()
            if (trimmedPart.isBlank()) return@mapNotNull null
            
            when {
                // :desc 패턴 처리
                trimmedPart.contains(":desc") -> {
                    val property = trimmedPart.substringBefore(":desc").trim()
                    if (property.isNotBlank()) Order.desc(property) else null
                }
                // :asc 패턴 처리
                trimmedPart.contains(":asc") -> {
                    val property = trimmedPart.substringBefore(":asc").trim()
                    if (property.isNotBlank()) Order.asc(property) else null
                }
                // 방향 지정 없는 경우 (콜론으로 끝나는 경우 제외)
                !trimmedPart.endsWith(":") -> Order.asc(trimmedPart)
                // 잘못된 형식 (콜론으로 끝나는 경우)
                else -> null
            }
        }

        return Sort(orders)
    }
}