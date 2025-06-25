package io.clroot.ball.domain.exception

open class DomainValidationException(
    message: String,
    val field: String? = null,
    val code: String? = null,
    cause: Throwable? = null
) : DomainException(message, cause) {
    companion object {
        fun invalidId(idValue: String) =
            DomainValidationException("잘못된 ID 형식입니다: $idValue", "id", "INVALID_ID")

        fun specificationNotSatisfied(specification: String) =
            DomainValidationException("조건을 만족하지 않습니다: $specification", code = "SPEC_NOT_SATISFIED")

        fun fieldValidation(field: String, message: String) =
            DomainValidationException(message, field, "FIELD_VALIDATION")
    }
}