package io.clroot.ball.domain.model

/**
 * 값 객체 (Value Object)
 *
 * 불변성을 가진 값 객체를 위한 마커 인터페이스
 * 값 객체는 식별자가 없고 속성 값에 의해 동등성이 결정됨
 *
 * 구현 클래스는 data class로 구현하여 equals, hashCode, toString 메서드를 자동으로 생성하는 것을 권장
 */
interface ValueObject
