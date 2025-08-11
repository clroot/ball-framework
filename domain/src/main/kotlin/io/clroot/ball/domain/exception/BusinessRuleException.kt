package io.clroot.ball.domain.exception

/**
 * 비즈니스 규칙 위반
 */
class BusinessRuleException(
    message: String,
    errorType: ErrorType = ErrorType.UNPROCESSABLE,
    val code: String? = null,
    cause: Throwable? = null
) : DomainException(message, errorType, cause) {
    companion object {
        /** 중복 리소스 */
        fun duplicate(resource: String) =
            BusinessRuleException("이미 사용 중인 ${resource}입니다", ErrorType.CONFLICT, "DUPLICATE")

        /** 이미 처리됨 */
        fun alreadyDone(action: String) =
            BusinessRuleException("이미 ${action}되었습니다", ErrorType.CONFLICT, "ALREADY_DONE")

        /** 시간 제약 */
        fun timeConstraint(message: String) =
            BusinessRuleException(message, ErrorType.UNPROCESSABLE, "TIME_CONSTRAINT")

        /** 전제조건 미충족 */
        fun precondition(message: String) =
            BusinessRuleException(message, ErrorType.PRECONDITION_FAILED, "PRECONDITION")

        /** 리소스 사용 불가 */
        fun unavailable(resource: String) =
            BusinessRuleException("${resource}을(를) 사용할 수 없습니다", ErrorType.GONE, "UNAVAILABLE")

        /** 동시성 충돌 */
        fun conflict(message: String) =
            BusinessRuleException(message, ErrorType.CONFLICT, "CONFLICT")

        /** 작업 불가 */
        fun cannot(action: String, reason: String) =
            BusinessRuleException("${action}할 수 없습니다: $reason", ErrorType.UNPROCESSABLE, "CANNOT")

        /** 비활성 상태 */
        fun inactive(entity: String) =
            BusinessRuleException("비활성화된 ${entity}입니다", ErrorType.GONE, "INACTIVE")
    }
}