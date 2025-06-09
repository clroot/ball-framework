package io.clroot.ball.domain.exception

/**
 * 비즈니스 규칙 위반
 */
class BusinessRuleException(
    message: String,
    val ruleCode: String? = null,
    cause: Throwable? = null
) : DomainException(message) {
    companion object {
        fun policyViolation(policy: String, message: String) =
            BusinessRuleException(message, "POLICY_VIOLATION_$policy")

        fun invariantViolation(message: String) =
            BusinessRuleException(message, "INVARIANT_VIOLATION")
    }
}