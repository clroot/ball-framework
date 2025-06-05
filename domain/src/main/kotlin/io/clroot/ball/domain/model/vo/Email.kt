package io.clroot.ball.domain.model.vo

import io.clroot.ball.domain.exception.ValidationException
import io.clroot.ball.domain.model.core.ValueObject

/**
 * 이메일 값 객체
 *
 * 이메일 주소를 나타내는 불변 값 객체
 * 유효한 이메일 형식인지 검증하는 기능 제공
 */
@JvmInline
value class Email private constructor(val value: String) : ValueObject {
    companion object {
        /**
         * 문자열로부터 이메일 객체 생성
         *
         * @param value 이메일 문자열
         * @return Email 객체
         * @throws ValidationException 유효하지 않은 이메일 형식인 경우
         */
        fun from(value: String): Email {
            if (!isValidEmail(value)) {
                throw ValidationException("Invalid email format: $value")
            }
            return Email(value)
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
