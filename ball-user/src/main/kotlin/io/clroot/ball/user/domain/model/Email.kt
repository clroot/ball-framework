package io.clroot.ball.user.domain.model

import arrow.core.Either
import io.clroot.ball.domain.model.core.ValueObject

/**
 * 이메일 (Email)
 *
 * 이메일 주소를 나타내는 값 객체
 */
@JvmInline
value class Email private constructor(
    val value: String
) : ValueObject {
    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")

        /**
         * 이메일 주소 생성
         *
         * @param value 이메일 주소 문자열
         * @return 유효한 이메일 주소인 경우 Email 객체, 그렇지 않은 경우 InvalidEmailError
         */
        @JvmStatic
        fun of(value: String): Either<InvalidEmailError, Email> =
            Either.catch {
                validate(value)
                Email(value)
            }.mapLeft { InvalidEmailError }

        private fun validate(value: String) {
            require(value.matches(EMAIL_REGEX)) { "Invalid email format: $value" }
        }
    }

    override fun toString(): String = value
}

sealed class EmailError

data object InvalidEmailError : EmailError()