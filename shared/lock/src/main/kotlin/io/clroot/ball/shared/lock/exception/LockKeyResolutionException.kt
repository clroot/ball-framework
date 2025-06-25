package io.clroot.ball.shared.lock.exception


/**
 * 락 키 해석 실패 예외
 */
class LockKeyResolutionException(message: String) : Exception(message) {
    companion object {
        fun invalidKey(keyExpression: String) = 
            LockKeyResolutionException("유효하지 않은 락 키입니다: $keyExpression")
        fun resolutionFailed(keyExpression: String, reason: String? = null) = 
            LockKeyResolutionException("락 키를 해석할 수 없습니다: $keyExpression${reason?.let { " ($it)" } ?: ""}")
    }
}