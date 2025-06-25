package io.clroot.ball.shared.lock.exception


/**
 * 락 획득 실패 예외
 */
class LockAcquisitionException(message: String) : Exception(message) {
    companion object {
        fun timeout(lockKey: String) = LockAcquisitionException("락 획득 시간이 초과되었습니다: $lockKey")
        fun failed(lockKey: String, reason: String? = null) = 
            LockAcquisitionException("락을 획득할 수 없습니다: $lockKey${reason?.let { " ($it)" } ?: ""}")
    }
}