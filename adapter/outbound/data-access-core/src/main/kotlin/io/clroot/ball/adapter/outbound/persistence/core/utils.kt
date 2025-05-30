package io.clroot.ball.adapter.outbound.persistence.core

import io.clroot.ball.domain.model.core.paging.*
import org.springframework.data.domain.Page as SpringPage
import org.springframework.data.domain.Pageable as SpringPageable
import org.springframework.data.domain.Sort as SpringSort

typealias RepositoryBean = org.springframework.stereotype.Repository

object SpringDataConverter {
    fun toSpringPageable(pageRequest: PageRequest): SpringPageable {
        return org.springframework.data.domain.PageRequest.of(
            pageRequest.page,
            pageRequest.size,
            toSpringSort(pageRequest.sort)
        )
    }

    fun <T> fromSpringPage(springPage: SpringPage<T>): Page<T> {
        val pageRequest = PageRequest(
            page = springPage.number,
            size = springPage.size,
            sort = fromSpringSort(springPage.sort)
        )

        return Page(
            content = springPage.content,
            pageRequest = pageRequest,
            totalElements = springPage.totalElements
        )
    }

    private fun toSpringSort(sort: Sort): SpringSort {
        if (sort.orders.isEmpty()) return SpringSort.unsorted()

        val springOrders = sort.orders.map { order ->
            val direction = when (order.direction) {
                Direction.ASC -> SpringSort.Direction.ASC
                Direction.DESC -> SpringSort.Direction.DESC
            }
            SpringSort.Order(direction, order.property)
        }

        return SpringSort.by(springOrders)
    }

    private fun fromSpringSort(springSort: SpringSort): Sort {
        val orders = springSort.map { springOrder ->
            val direction = when (springOrder.direction) {
                SpringSort.Direction.ASC -> Direction.ASC
                SpringSort.Direction.DESC -> Direction.DESC
                else -> Direction.ASC
            }
            Order(springOrder.property, direction)
        }

        return Sort(orders.toList())
    }
}

fun PageRequest.toSpring(): SpringPageable = SpringDataConverter.toSpringPageable(this)

fun <T> SpringPage<T>.toBall(): Page<T> = SpringDataConverter.fromSpringPage(this)