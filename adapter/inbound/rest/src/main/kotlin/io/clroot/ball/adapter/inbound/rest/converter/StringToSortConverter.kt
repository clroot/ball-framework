package io.clroot.ball.adapter.inbound.rest.converter

import io.clroot.ball.domain.model.core.paging.Order
import io.clroot.ball.domain.model.core.paging.Sort
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class StringToSortConverter : Converter<String, Sort> {
    override fun convert(source: String): Sort {
        if (source.isBlank()) return Sort.unsorted()

        val orders = source.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { part ->
                when {
                    part.endsWith(":desc") -> Order.desc(part.removeSuffix(":desc"))
                    part.endsWith(":asc") -> Order.asc(part.removeSuffix(":asc"))
                    else -> Order.asc(part)
                }
            }

        return Sort(orders)
    }
}