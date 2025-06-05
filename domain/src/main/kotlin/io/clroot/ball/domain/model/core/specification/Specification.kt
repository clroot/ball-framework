package io.clroot.ball.domain.model.core.specification

import io.clroot.ball.domain.exception.SpecificationNotSatisfiedException

/**
 * 명세 (Specification)
 *
 * 객체가 특정 조건을 만족하는지 검증하는 명세 패턴
 * 도메인 규칙을 명시적으로 표현하고 재사용 가능하게 함
 *
 * @param T 검증 대상 객체 타입
 */
interface Specification<T> {
    /**
     * 객체가 명세를 만족하는지 검증
     *
     * @param t 검증 대상 객체
     * @return 만족하면 true, 아니면 false
     */
    fun isSatisfiedBy(t: T): Boolean
    
    /**
     * 두 명세를 AND 연산으로 결합
     *
     * @param other 결합할 다른 명세
     * @return 두 명세를 모두 만족해야 true를 반환하는 새로운 명세
     */
    fun and(other: Specification<T>): Specification<T> = AndSpecification(this, other)
    
    /**
     * 두 명세를 OR 연산으로 결합
     *
     * @param other 결합할 다른 명세
     * @return 두 명세 중 하나라도 만족하면 true를 반환하는 새로운 명세
     */
    fun or(other: Specification<T>): Specification<T> = OrSpecification(this, other)
    
    /**
     * 명세를 NOT 연산으로 부정
     *
     * @return 원래 명세의 결과를 부정하는 새로운 명세
     */
    fun not(): Specification<T> = NotSpecification(this)
    
    /**
     * 명세를 검증하고 조건을 만족하지 않으면 예외를 발생시킴
     *
     * @param t 검증 대상 객체
     * @param errorMessage 명세를 만족하지 않을 때 사용할 오류 메시지
     * @return 조건을 만족하는 경우 원본 객체 반환
     * @throws SpecificationNotSatisfiedException 명세를 만족하지 않는 경우
     */
    fun validate(t: T, errorMessage: String): T {
        if (!isSatisfiedBy(t)) {
            throw SpecificationNotSatisfiedException(errorMessage)
        }
        return t
    }
    
    /**
     * 명세를 검증하고 조건을 만족하지 않으면 기본 오류 메시지로 예외를 발생시킨다
     *
     * @param t 검증 대상 객체
     * @return 조건을 만족하는 경우 원본 객체 반환
     * @throws SpecificationNotSatisfiedException 명세를 만족하지 않는 경우
     */
    fun validate(t: T): T = validate(t, "Specification not satisfied for: $t")
}
