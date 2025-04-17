package io.clroot.ball.shared.lock.exception

import io.clroot.ball.shared.core.exception.ApplicationException

/**
 * 락 키 해석 실패 예외
 */
class LockKeyResolutionException(message: String) : ApplicationException(message)