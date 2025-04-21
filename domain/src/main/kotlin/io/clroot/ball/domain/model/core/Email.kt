package io.clroot.ball.domain.model.core

import arrow.core.Either
import arrow.core.left
import arrow.core.right

/**
 * 이메일 값 객체
 *
 * 이메일 주소를 나타내는 불변 값 객체
 * 유효한 이메일 형식인지 검증하는 기능 제공
 */
data class Email private constructor(val value: String) : ValueObject {
    companion object {
        /**
         * 문자열로부터 이메일 객체 생성
         *
         * @param value 이메일 문자열
         * @return 유효한 이메일이면 Email 객체, 아니면 ValidationError
         */
        fun from(value: String): Either<ValidationError, Email> {
            return if (isValidEmail(value)) {
                Email(value).right()
            } else {
                ValidationError("Invalid email format: $value").left()
            }
        }

        /**
         * 이메일 형식 유효성 검증
         *
         * @param value 검증할 이메일 문자열
         * @return 유효한 이메일이면 true, 아니면 false
         */
        private fun isValidEmail(value: String): Boolean {
            if (value.isBlank()) return false

            // 이메일 유효성 검증 로직 - 더 엄격한 검증
            val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
            return value.matches(emailRegex.toRegex()) && !value.contains(" ")
        }
    }

    override fun toString(): String = value
}

/**
 * 유효성 검증 오류
 */
data class ValidationError(val message: String)
