package io.clroot.ball.domain.model.vo

import arrow.core.Either
import io.clroot.ball.domain.model.core.ValueObject
import ulid.ULID

@JvmInline
value class BinaryId(
    private val value: String
) : ValueObject {
    companion object {
        fun new(): BinaryId = BinaryId(generateULID())

        fun fromString(value: String): Either<InvalidIdError, BinaryId> =
            Either.catch {
                validateULID(value)
                BinaryId(value)
            }.mapLeft { InvalidIdError }

        private fun generateULID(): String =
            ULID.nextULID().toString()

        private fun validateULID(value: String) {
            require(value.length == 26) { "ULID must be 26 characters long" }
            require(value.matches(Regex("^[0123456789ABCDEFGHJKMNPQRSTVWXYZ]{26}$"))) {
                "ULID must contain only Crockford's base32 characters"
            }
        }
    }

    override fun toString(): String = value
}

sealed class IdError

data object InvalidIdError : IdError()

