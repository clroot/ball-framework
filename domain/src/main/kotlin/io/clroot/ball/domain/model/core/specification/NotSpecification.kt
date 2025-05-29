package io.clroot.ball.domain.model.core.specification

/**
 * NOT 명세
 * 원래 명세의 결과를 부정
 */
class NotSpecification<T>(
    private val specification: Specification<T>
) : Specification<T> {
    override fun isSatisfiedBy(t: T): Boolean =
        !specification.isSatisfiedBy(t)
}