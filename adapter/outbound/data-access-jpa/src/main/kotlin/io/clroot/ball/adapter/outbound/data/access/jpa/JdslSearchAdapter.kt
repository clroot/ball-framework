package io.clroot.ball.adapter.outbound.data.access.jpa

import com.linecorp.kotlinjdsl.dsl.jpql.Jpql
import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.dsl.jpql.select.SelectQueryWhereStep
import com.linecorp.kotlinjdsl.querymodel.jpql.expression.Expressionable
import com.linecorp.kotlinjdsl.querymodel.jpql.predicate.Predicatable
import com.linecorp.kotlinjdsl.querymodel.jpql.select.SelectQuery
import com.linecorp.kotlinjdsl.querymodel.jpql.sort.Sortable
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.spring.data.jpa.extension.createQuery
import io.clroot.ball.adapter.outbound.data.access.core.toBall
import io.clroot.ball.adapter.outbound.data.access.core.toSpring
import io.clroot.ball.domain.model.paging.Page
import io.clroot.ball.domain.model.paging.PageRequest
import io.clroot.ball.domain.port.Search
import io.clroot.ball.domain.port.SearchCriteria
import jakarta.persistence.EntityManager
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Order

abstract class JdslSearchAdapter<T : SearchCriteria, R, E : Any>(
    protected open val entityManager: EntityManager,
    protected open val jpqlRenderContext: JpqlRenderContext,
) : Search<T, R> {
    override fun search(criteria: T): List<R> {
        val contentQuery = query(criteria, Pageable.unpaged())
        return entityManager
            .createQuery(contentQuery, jpqlRenderContext)
            .resultList
            .map { it.toResult() }
    }

    override fun search(
        criteria: T,
        pageRequest: PageRequest,
    ): Page<R> {
        val pageable = pageRequest.toSpring()
        val contentQuery = query(criteria, pageable)
        val countQuery =
            jpql {
                selectCount()
                    .whereAnd(*where(criteria))
            }
        val content =
            entityManager
                .createQuery(contentQuery, jpqlRenderContext)
                .apply {
                    firstResult = pageable.offset.toInt()
                    maxResults = pageable.pageSize
                }.resultList
                .map { it.toResult() }
        val count =
            entityManager
                .createQuery(countQuery, jpqlRenderContext)
                .singleResult as Long
        return PageImpl(content, pageable, count).toBall()
    }

    protected abstract fun E.toResult(): R

    protected abstract fun Jpql.selectFrom(): SelectQueryWhereStep<E>

    protected abstract fun Jpql.selectCount(): SelectQueryWhereStep<Long>

    protected abstract fun Jpql.where(criteria: T): Array<out Predicatable?>

    protected abstract fun Jpql.orderBy(order: Order): Expressionable<*>

    protected open fun Jpql.defaultOrder(): Array<out Sortable?> = emptyArray()

    private fun query(
        criteria: T,
        pageable: Pageable,
    ): SelectQuery<E> =
        jpql {
            selectFrom()
                .whereAnd(*where(criteria))
                .orderBy(*orderBy(pageable.sort), *defaultOrder())
        }

    private fun Jpql.orderBy(sort: Sort): Array<out Sortable?> =
        sort
            .map {
                val direction = if (it.isAscending) Sort.Direction.ASC else Sort.Direction.DESC
                val path = orderBy(it)
                if (direction == Sort.Direction.ASC) {
                    path.asc()
                } else {
                    path.desc()
                }
            }.toList()
            .toTypedArray()

    fun buildWherePredicates(
        jpql: Jpql,
        criteria: T,
    ): Array<out Predicatable?> = jpql.where(criteria)
}
