package io.clroot.ball.domain.model.policy

import io.clroot.ball.domain.exception.PolicyViolationException
import io.clroot.ball.domain.model.specification.Specification

/**
 * 명세 기반 정책
 *
 * 명세 패턴을 사용하여 정책을 구현
 *
 * @param T 검증 대상 객체 타입
 * @param specification 사용할 명세
 * @param errorMessageProvider 오류 메시지 생성 함수
 */
class SpecificationPolicy<T>(
    private val specification: Specification<T>,
    private val errorMessageProvider: (T) -> String
) : Policy<T> {
    override fun validate(target: T) {
        if (!specification.isSatisfiedBy(target)) {
            throw PolicyViolationException(errorMessageProvider(target))
        }
    }
}
