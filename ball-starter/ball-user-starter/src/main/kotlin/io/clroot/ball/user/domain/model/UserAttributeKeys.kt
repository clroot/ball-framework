package io.clroot.ball.user.domain.model

import io.clroot.ball.shared.attribute.AttributeKey

/**
 * 사용자 속성 키 (User Attribute Keys)
 *
 * 사용자 속성에 대한 사전 정의된 키 모음
 */
object UserAttributeKeys {
    /**
     * 비밀번호 해시
     */
    val PASSWORD_HASH = AttributeKey("password_hash", String::class)

    /**
     * 비밀번호 솔트
     */
    val PASSWORD_SALT = AttributeKey("password_salt", String::class)

    /**
     * 전화번호
     */
    val PHONE_NUMBER = AttributeKey("phone_number", String::class)

    /**
     * 주소
     */
    val ADDRESS = AttributeKey("address", String::class)

    /**
     * 생년월일
     */
    val BIRTH_DATE = AttributeKey("birth_date", String::class)

    /**
     * 성별
     */
    val GENDER = AttributeKey("gender", String::class)

    /**
     * 소셜 로그인 제공자
     */
    val SOCIAL_PROVIDER = AttributeKey("social_provider", String::class)

    /**
     * 소셜 로그인 ID
     */
    val SOCIAL_ID = AttributeKey("social_id", String::class)
}