package io.clroot.ball.domain.exception

/**
 * 외부 시스템 연동 중 발생하는 예외의 기본 클래스
 * 
 * 외부 API, 결제 게이트웨이, 메시징 시스템 등과의 연동에서
 * 발생하는 모든 예외의 부모 클래스입니다.
 */
abstract class ExternalSystemException(
    message: String,
    errorType: ErrorType = ErrorType.EXTERNAL_ERROR,
    val systemName: String? = null,
    val errorCode: String? = null,
    cause: Throwable? = null
) : DomainException(message, errorType, cause) {
    
    companion object {
        /** 외부 시스템 일반 오류 */
        fun error(system: String, message: String, code: String? = null) =
            object : ExternalSystemException(
                message = "$system 오류: $message",
                errorType = ErrorType.EXTERNAL_ERROR,
                systemName = system,
                errorCode = code
            ) {}
        
        /** 외부 시스템 타임아웃 */
        fun timeout(system: String, timeoutMs: Long? = null) =
            object : ExternalSystemException(
                message = "$system 응답 시간 초과" + (timeoutMs?.let { " (${it}ms)" } ?: ""),
                errorType = ErrorType.EXTERNAL_TIMEOUT,
                systemName = system
            ) {}
        
        /** 외부 시스템 사용 불가 */
        fun unavailable(system: String, reason: String? = null) =
            object : ExternalSystemException(
                message = "$system 서비스를 사용할 수 없습니다" + (reason?.let { ": $it" } ?: ""),
                errorType = ErrorType.GONE,
                systemName = system
            ) {}
    }
}