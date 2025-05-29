package io.clroot.ball.domain.model.core.specification

/**
 * AND 명세
 * 두 명세를 모두 만족해야 true를 반환
 */
class AndSpecification<T>(
    private val left: Specification<T>,
    private val right: Specification<T>
) : Specification<T> {
    override fun isSatisfiedBy(t: T): Boolean =
        left.isSatisfiedBy(t) && right.isSatisfiedBy(t)
}