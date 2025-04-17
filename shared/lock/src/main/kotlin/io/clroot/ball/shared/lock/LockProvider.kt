package io.clroot.ball.shared.lock

/**
 * 분산 락 제공자 인터페이스
 * 여러 서버 인스턴스 간에 공유되는 락 메커니즘을 제공합니다.
 */
interface LockProvider {
    /**
     * 락을 획득하고 함수를 실행한 후 락을 해제합니다.
     *
     * @param key 락 키 - 락의 고유 식별자
     * @param waitTime 락 획득 대기 시간 (밀리초)
     * @param leaseTime 락 유지 시간 (밀리초)
     * @param block 락을 획득한 상태에서 실행할 함수
     * @return 함수 실행 결과
     * @throws io.clroot.ball.shared.lock.exception.LockAcquisitionException 락을 획득하지 못한 경우
     */
    fun <T> withLock(key: String, waitTime: Long = 5000, leaseTime: Long = 10000, block: () -> T): T
}
