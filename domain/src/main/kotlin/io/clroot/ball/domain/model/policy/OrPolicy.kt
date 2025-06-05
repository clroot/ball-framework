package io.clroot.ball.domain.model.policy

/**
 * OR 정책
 * 두 정책 중 하나라도 만족하면 성공
 */
class OrPolicy<T>(
    private val left: Policy<T>,
    private val right: Policy<T>
) : Policy<T> {
    override fun validate(target: T) {
        try {
            left.validate(target)
            return // 첫 번째 정책이 성공하면 바로 반환
        } catch (e: PolicyViolationException) {
            // 첫 번째 정책이 실패하면 두 번째 정책 시도
            right.validate(target) // 이것도 실패하면 예외가 전파됨
        }
    }
}
