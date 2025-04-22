package io.clroot.ball.domain.model.core

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency

/**
 * 금액 값 객체 (Money Value Object)
 *
 * 화폐 금액을 나타내는 불변 값 객체
 * 통화와 금액을 포함하며 화폐 연산을 제공
 *
 * @param amount 금액
 * @param currency 통화
 */
data class Money private constructor(
    val amount: BigDecimal,
    val currency: Currency
) : ValueObject {
    companion object {
        /**
         * 금액과 통화 코드로 Money 객체 생성
         *
         * @param amount 금액
         * @param currencyCode 통화 코드 (ISO 4217)
         * @return 유효한 금액이면 Right(Money), 아니면 Left(MoneyError)
         */
        fun of(amount: BigDecimal, currencyCode: String): Either<MoneyError, Money> {
            return try {
                val currency = Currency.getInstance(currencyCode)
                of(amount, currency)
            } catch (e: IllegalArgumentException) {
                MoneyError.InvalidCurrency("Invalid currency code: $currencyCode").left()
            }
        }

        /**
         * 금액과 통화로 Money 객체 생성
         *
         * @param amount 금액
         * @param currency 통화
         * @return 유효한 금액이면 Right(Money), 아니면 Left(MoneyError)
         */
        fun of(amount: BigDecimal, currency: Currency): Either<MoneyError, Money> {
            val scale = currency.defaultFractionDigits
            val scaledAmount = amount.setScale(scale, RoundingMode.HALF_EVEN)
            return Money(scaledAmount, currency).right()
        }

        /**
         * 금액 문자열과 통화 코드로 Money 객체 생성
         *
         * @param amountStr 금액 문자열
         * @param currencyCode 통화 코드 (ISO 4217)
         * @return 유효한 금액이면 Right(Money), 아니면 Left(MoneyError)
         */
        fun parse(amountStr: String, currencyCode: String): Either<MoneyError, Money> {
            return try {
                val amount = BigDecimal(amountStr)
                of(amount, currencyCode)
            } catch (e: NumberFormatException) {
                MoneyError.InvalidAmount("Invalid amount format: $amountStr").left()
            }
        }

        /**
         * 0 금액 생성
         *
         * @param currency 통화
         * @return 0 금액
         */
        fun zero(currency: Currency): Money {
            return Money(BigDecimal.ZERO.setScale(currency.defaultFractionDigits), currency)
        }

        /**
         * 0 금액 생성
         *
         * @param currencyCode 통화 코드 (ISO 4217)
         * @return 0 금액 또는 오류
         */
        fun zero(currencyCode: String): Either<MoneyError, Money> {
            return try {
                val currency = Currency.getInstance(currencyCode)
                zero(currency).right()
            } catch (e: IllegalArgumentException) {
                MoneyError.InvalidCurrency("Invalid currency code: $currencyCode").left()
            }
        }
    }

    /**
     * 금액 더하기
     *
     * @param other 더할 금액
     * @return 더한 결과
     * @throws IllegalArgumentException 통화가 다른 경우
     */
    fun add(other: Money): Either<MoneyError, Money> {
        if (currency != other.currency) {
            return MoneyError.CurrencyMismatch(
                "Cannot add money with different currencies: $currency and ${other.currency}"
            ).left()
        }
        return Money(amount.add(other.amount), currency).right()
    }

    /**
     * 금액 빼기
     *
     * @param other 뺄 금액
     * @return 뺀 결과
     * @throws IllegalArgumentException 통화가 다른 경우
     */
    fun subtract(other: Money): Either<MoneyError, Money> {
        if (currency != other.currency) {
            return MoneyError.CurrencyMismatch(
                "Cannot subtract money with different currencies: $currency and ${other.currency}"
            ).left()
        }
        return Money(amount.subtract(other.amount), currency).right()
    }

    /**
     * 금액 곱하기
     *
     * @param multiplier 곱할 수
     * @return 곱한 결과
     */
    fun multiply(multiplier: BigDecimal): Money {
        val newAmount = amount.multiply(multiplier)
            .setScale(currency.defaultFractionDigits, RoundingMode.HALF_EVEN)
        return Money(newAmount, currency)
    }

    /**
     * 금액 나누기
     *
     * @param divisor 나눌 수
     * @return 나눈 결과
     * @throws ArithmeticException 0으로 나누는 경우
     */
    fun divide(divisor: BigDecimal): Either<MoneyError, Money> {
        if (divisor == BigDecimal.ZERO) {
            return MoneyError.DivisionByZero("Cannot divide by zero").left()
        }
        
        try {
            val newAmount = amount.divide(divisor, currency.defaultFractionDigits, RoundingMode.HALF_EVEN)
            return Money(newAmount, currency).right()
        } catch (e: ArithmeticException) {
            return MoneyError.ArithmeticError(e.message ?: "Arithmetic error").left()
        }
    }

    /**
     * 금액이 0인지 확인
     *
     * @return 금액이 0이면 true, 아니면 false
     */
    fun isZero(): Boolean = amount.compareTo(BigDecimal.ZERO) == 0

    /**
     * 금액이 양수인지 확인
     *
     * @return 금액이 양수이면 true, 아니면 false
     */
    fun isPositive(): Boolean = amount.compareTo(BigDecimal.ZERO) > 0

    /**
     * 금액이 음수인지 확인
     *
     * @return 금액이 음수이면 true, 아니면 false
     */
    fun isNegative(): Boolean = amount.compareTo(BigDecimal.ZERO) < 0

    /**
     * 금액 비교
     *
     * @param other 비교할 금액
     * @return 같으면 0, 작으면 음수, 크면 양수
     * @throws IllegalArgumentException 통화가 다른 경우
     */
    fun compareTo(other: Money): Either<MoneyError, Int> {
        if (currency != other.currency) {
            return MoneyError.CurrencyMismatch(
                "Cannot compare money with different currencies: $currency and ${other.currency}"
            ).left()
        }
        return amount.compareTo(other.amount).right()
    }

    override fun toString(): String = "$amount $currency"
}

/**
 * 금액 오류
 */
sealed class MoneyError {
    /**
     * 유효하지 않은 금액
     */
    data class InvalidAmount(val message: String) : MoneyError()

    /**
     * 유효하지 않은 통화
     */
    data class InvalidCurrency(val message: String) : MoneyError()

    /**
     * 통화 불일치
     */
    data class CurrencyMismatch(val message: String) : MoneyError()

    /**
     * 0으로 나누기 오류
     */
    data class DivisionByZero(val message: String) : MoneyError()

    /**
     * 산술 오류
     */
    data class ArithmeticError(val message: String) : MoneyError()
}