package io.clroot.ball.shared.lock.exception

import io.clroot.ball.shared.core.exception.ApplicationException

/**
 * 락 획득 실패 예외
 */
class LockAcquisitionException(message: String) : ApplicationException(message)