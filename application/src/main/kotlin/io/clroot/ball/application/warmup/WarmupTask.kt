package io.clroot.ball.application.warmup

/**
 * Warm-up 작업 인터페이스
 *
 * 각 모듈은 이 인터페이스를 구현하여 자신만의 warm-up 로직을 제공한다.
 */
interface WarmupTask {
    /**
     * Warm-up 작업 실행
     *
     * @throws Exception warm-up 실패 시
     */
    fun warmup()
}
