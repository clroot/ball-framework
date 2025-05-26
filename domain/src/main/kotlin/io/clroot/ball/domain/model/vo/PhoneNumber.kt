package io.clroot.ball.domain.model.vo

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.clroot.ball.domain.model.core.ValueObject

/**
 * 전화번호 값 객체 (PhoneNumber Value Object)
 *
 * 전화번호를 나타내는 불변 값 객체
 * 국가 코드, 지역 코드, 번호를 포함하며 유효성 검증 기능 제공
 */
data class PhoneNumber private constructor(
    val countryCode: String,
    val areaCode: String,
    val number: String
) : ValueObject {
    companion object {
        private val COUNTRY_CODE_PATTERN = Regex("^\\+[1-9]\\d{0,2}$")
        private val AREA_CODE_PATTERN = Regex("^[0-9]{1,5}$")
        private val NUMBER_PATTERN = Regex("^[0-9]{4,12}$")
        
        /**
         * 국가 코드, 지역 코드, 번호로 PhoneNumber 객체 생성
         *
         * @param countryCode 국가 코드 (예: +82)
         * @param areaCode 지역 코드 (예: 10)
         * @param number 전화번호
         * @return 유효한 전화번호이면 Right(PhoneNumber), 아니면 Left(PhoneNumberError)
         */
        fun of(
            countryCode: String,
            areaCode: String,
            number: String
        ): Either<PhoneNumberError, PhoneNumber> {
            // 국가 코드 검증
            if (!COUNTRY_CODE_PATTERN.matches(countryCode)) {
                return PhoneNumberError.InvalidCountryCode(
                    "Invalid country code format: $countryCode. Must start with + followed by 1-3 digits."
                ).left()
            }
            
            // 지역 코드 검증
            if (!AREA_CODE_PATTERN.matches(areaCode)) {
                return PhoneNumberError.InvalidAreaCode(
                    "Invalid area code format: $areaCode. Must be 1-5 digits."
                ).left()
            }
            
            // 번호 검증
            if (!NUMBER_PATTERN.matches(number)) {
                return PhoneNumberError.InvalidNumber(
                    "Invalid number format: $number. Must be 4-12 digits."
                ).left()
            }
            
            return PhoneNumber(countryCode, areaCode, number).right()
        }
        
        /**
         * 전화번호 문자열에서 PhoneNumber 객체 생성
         *
         * @param phoneNumberStr 전화번호 문자열 (예: +82-10-12345678)
         * @return 유효한 전화번호이면 Right(PhoneNumber), 아니면 Left(PhoneNumberError)
         */
        fun parse(phoneNumberStr: String): Either<PhoneNumberError, PhoneNumber> {
            // 전화번호 형식: +CC-AC-NUMBER 또는 +CC.AC.NUMBER
            val cleanedStr = phoneNumberStr.replace("\\s".toRegex(), "")
            val parts = cleanedStr.split("-", ".", " ")
            
            if (parts.size != 3) {
                return PhoneNumberError.InvalidFormat(
                    "Invalid phone number format: $phoneNumberStr. Expected format: +CC-AC-NUMBER or +CC.AC.NUMBER"
                ).left()
            }
            
            return of(parts[0], parts[1], parts[2])
        }
    }
    
    /**
     * 전화번호를 형식화된 문자열로 변환
     *
     * @return 형식화된 전화번호 문자열 (예: +82-10-12345678)
     */
    fun toFormattedString(): String = "$countryCode-$areaCode-$number"
    
    /**
     * 전화번호를 E.164 형식으로 변환
     *
     * @return E.164 형식의 전화번호 문자열 (예: +821012345678)
     */
    fun toE164Format(): String = "$countryCode$areaCode$number"
    
    override fun toString(): String = toFormattedString()
}

/**
 * 전화번호 오류
 */
sealed class PhoneNumberError {
    /**
     * 유효하지 않은 국가 코드
     */
    data class InvalidCountryCode(val message: String) : PhoneNumberError()
    
    /**
     * 유효하지 않은 지역 코드
     */
    data class InvalidAreaCode(val message: String) : PhoneNumberError()
    
    /**
     * 유효하지 않은 번호
     */
    data class InvalidNumber(val message: String) : PhoneNumberError()
    
    /**
     * 유효하지 않은 형식
     */
    data class InvalidFormat(val message: String) : PhoneNumberError()
}