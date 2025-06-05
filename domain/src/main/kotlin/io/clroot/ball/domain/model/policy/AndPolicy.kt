package io.clroot.ball.domain.model.policy

/**
 * AND 정책
 * 두 정책을 모두 만족해야 성공
 */
class AndPolicy<T>(
    private val left: Policy<T>,
    private val right: Policy<T>
) : Policy<T> {
    override fun validate(target: T) {
        left.validate(target)  // 첫 번째 정책이 실패하면 예외 발생
        right.validate(target) // 두 번째 정책이 실패하면 예외 발생
    }
}
