package io.clroot.ball.domain.model.policy

import io.clroot.ball.domain.exception.BusinessRuleException

/**
 * 정책 (Policy)
 *
 * 도메인 규칙을 적용하는 정책 인터페이스
 * 객체가 특정 비즈니스 규칙을 만족하는지 검증
 *
 * @param T 검증 대상 객체 타입
 */
interface Policy<T> {
    /**
     * 정책 검증
     *
     * @param target 검증 대상 객체
     * @throws BusinessRuleException 정책 위반 시
     */
    fun validate(target: T)

    /**
     * 정책을 검증하고 조건을 만족하는지 확인
     *
     * @param target 검증 대상 객체
     * @return 정책을 만족하면 true, 아니면 false
     */
    fun isSatisfiedBy(target: T): Boolean {
        return try {
            validate(target)
            true
        } catch (e: BusinessRuleException) {
            false
        }
    }

    /**
     * 두 정책을 AND 연산으로 결합
     *
     * @param other 결합할 다른 정책
     * @return 두 정책을 모두 만족해야 성공하는 새로운 정책
     */
    fun and(other: Policy<T>): Policy<T> = AndPolicy(this, other)

    /**
     * 두 정책을 OR 연산으로 결합
     *
     * @param other 결합할 다른 정책
     * @return 두 정책 중 하나라도 만족하면 성공하는 새로운 정책
     */
    fun or(other: Policy<T>): Policy<T> = OrPolicy(this, other)
}

