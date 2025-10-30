package io.clroot.ball.domain.exception

/**
 * 비즈니스 규칙 위반
 */
class BusinessRuleException(
    message: String,
    errorType: ErrorType = ErrorType.UNPROCESSABLE,
    code: String = DomainErrorCodes.UNPROCESSABLE,
    messageKey: String? = "domain.business_rule.violation",
    messageArgs: Map<String, Any?> = emptyMap(),
    metadata: Map<String, Any?> = emptyMap(),
    cause: Throwable? = null,
) : DomainException(
        message = message,
        errorType = errorType,
        errorCode = code,
        messageKey = messageKey,
        messageArgs = messageArgs,
        metadata = metadata,
        cause = cause,
    ) {
    companion object {
        /** 중복 리소스 */
        fun duplicate(resource: String) =
            BusinessRuleException(
                message = "이미 사용 중인 ${resource}입니다",
                errorType = ErrorType.CONFLICT,
                code = DomainErrorCodes.BUSINESS_RULE_DUPLICATE,
                messageKey = "domain.business_rule.duplicate",
                messageArgs = mapOf("resource" to resource),
            )

        /** 이미 처리됨 */
        fun alreadyDone(action: String) =
            BusinessRuleException(
                message = "이미 ${action}되었습니다",
                errorType = ErrorType.CONFLICT,
                code = DomainErrorCodes.BUSINESS_RULE_ALREADY_DONE,
                messageKey = "domain.business_rule.already_done",
                messageArgs = mapOf("action" to action),
            )

        /** 시간 제약 */
        fun timeConstraint(message: String) =
            BusinessRuleException(
                message = message,
                errorType = ErrorType.UNPROCESSABLE,
                code = DomainErrorCodes.BUSINESS_RULE_TIME_CONSTRAINT,
                messageKey = "domain.business_rule.time_constraint",
            )

        /** 전제조건 미충족 */
        fun precondition(message: String) =
            BusinessRuleException(
                message = message,
                errorType = ErrorType.PRECONDITION_FAILED,
                code = DomainErrorCodes.BUSINESS_RULE_PRECONDITION,
                messageKey = "domain.business_rule.precondition",
            )

        /** 리소스 사용 불가 */
        fun unavailable(resource: String) =
            BusinessRuleException(
                message = "${resource}을(를) 사용할 수 없습니다",
                errorType = ErrorType.GONE,
                code = DomainErrorCodes.BUSINESS_RULE_UNAVAILABLE,
                messageKey = "domain.business_rule.unavailable",
                messageArgs = mapOf("resource" to resource),
            )

        /** 동시성 충돌 */
        fun conflict(message: String) =
            BusinessRuleException(
                message = message,
                errorType = ErrorType.CONFLICT,
                code = DomainErrorCodes.BUSINESS_RULE_CONFLICT,
                messageKey = "domain.business_rule.conflict",
            )

        /** 작업 불가 */
        fun cannot(action: String, reason: String) =
            BusinessRuleException(
                message = "${action}할 수 없습니다: $reason",
                errorType = ErrorType.UNPROCESSABLE,
                code = DomainErrorCodes.BUSINESS_RULE_CANNOT,
                messageKey = "domain.business_rule.cannot",
                messageArgs = mapOf("action" to action, "reason" to reason),
                metadata = mapOf("reason" to reason),
            )

        /** 비활성 상태 */
        fun inactive(entity: String) =
            BusinessRuleException(
                message = "비활성화된 ${entity}입니다",
                errorType = ErrorType.GONE,
                code = DomainErrorCodes.BUSINESS_RULE_INACTIVE,
                messageKey = "domain.business_rule.inactive",
                messageArgs = mapOf("entity" to entity),
            )
    }
}
