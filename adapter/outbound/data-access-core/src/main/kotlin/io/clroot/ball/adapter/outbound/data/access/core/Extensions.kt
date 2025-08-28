package io.clroot.ball.adapter.outbound.data.access.core

import io.clroot.ball.adapter.outbound.data.access.core.exception.DatabaseException
import io.clroot.ball.domain.model.paging.*
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.data.domain.PageImpl
import java.io.IOException
import java.nio.charset.StandardCharsets
import org.springframework.data.domain.Page as SpringPage
import org.springframework.data.domain.Pageable as SpringPageable
import org.springframework.data.domain.Sort as SpringSort

typealias RepositoryBean = org.springframework.stereotype.Repository

object SpringDataConverter {
    fun fromSpringPageable(springPageable: SpringPageable): PageRequest =
        PageRequest(
            page = springPageable.pageNumber,
            size = springPageable.pageSize,
            sort = fromSpringSort(springPageable.sort),
        )

    fun toSpringPageable(pageRequest: PageRequest): SpringPageable =
        org.springframework.data.domain.PageRequest.of(
            pageRequest.page,
            pageRequest.size,
            toSpringSort(pageRequest.sort),
        )

    fun <T> fromSpringPage(springPage: SpringPage<T>): Page<T> {
        val pageRequest =
            PageRequest(
                page = springPage.number,
                size = springPage.size,
                sort = fromSpringSort(springPage.sort),
            )

        return Page(
            content = springPage.content,
            pageRequest = pageRequest,
            totalElements = springPage.totalElements,
        )
    }

    fun <T> toSpringPage(page: Page<T>): SpringPage<T> =
        PageImpl<T>(
            page.content,
            toSpringPageable(page.pageRequest),
            page.totalElements,
        )

    private fun toSpringSort(sort: Sort): SpringSort {
        if (sort.orders.isEmpty()) return SpringSort.unsorted()

        val springOrders =
            sort.orders.map { order ->
                val direction =
                    when (order.direction) {
                        Direction.ASC -> SpringSort.Direction.ASC
                        Direction.DESC -> SpringSort.Direction.DESC
                    }
                SpringSort.Order(direction, order.property)
            }

        return SpringSort.by(springOrders)
    }

    private fun fromSpringSort(springSort: SpringSort): Sort {
        val orders =
            springSort.map { springOrder ->
                val direction =
                    when (springOrder.direction) {
                        SpringSort.Direction.DESC -> Direction.DESC
                        else -> Direction.ASC
                    }
                Order(springOrder.property, direction)
            }

        return Sort(orders.toList())
    }
}

fun PageRequest.toSpring(): SpringPageable = SpringDataConverter.toSpringPageable(this)

fun SpringPageable.toBall(): PageRequest = SpringDataConverter.fromSpringPageable(this)

fun <T> SpringPage<T>.toBall(): Page<T> = SpringDataConverter.fromSpringPage(this)

fun <T> Page<T>.toSpring(): SpringPage<T> = SpringDataConverter.toSpringPage(this)

fun loadSqlFromClasspath(path: String): String {
    try {
        val resource: Resource = ClassPathResource(path)
        return String(resource.inputStream.readAllBytes(), StandardCharsets.UTF_8)
    } catch (e: IOException) {
        throw DatabaseException("SQL 파일을 찾을 수 없습니다: $path")
    }
}
