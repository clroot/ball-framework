package io.clroot.ball.domain.model.specification

/**
 * OR 명세
 * 두 명세 중 하나라도 만족하면 true를 반환
 */
class OrSpecification<T>(
    private val left: Specification<T>,
    private val right: Specification<T>
) : Specification<T> {
    override fun isSatisfiedBy(t: T): Boolean =
        left.isSatisfiedBy(t) || right.isSatisfiedBy(t)
}