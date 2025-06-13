package io.clroot.ball.domain.exception

open class DomainValidationException(
    message: String,
    val field: String? = null,
    val code: String? = null,
    cause: Throwable? = null
) : DomainException(message, cause) {
    companion object {
        fun invalidId(idValue: String) =
            DomainValidationException("Invalid ID format: $idValue", "id", "INVALID_ID")

        fun specificationNotSatisfied(specification: String) =
            DomainValidationException("Specification not satisfied: $specification", code = "SPEC_NOT_SATISFIED")

        fun fieldValidation(field: String, message: String) =
            DomainValidationException(message, field, "FIELD_VALIDATION")
    }
}