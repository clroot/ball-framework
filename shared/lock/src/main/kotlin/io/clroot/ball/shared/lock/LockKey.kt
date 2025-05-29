package io.clroot.ball.shared.lock

/**
 * 분산 락 키로 사용할 메서드 파라미터를 지정하는 어노테이션
 *
 * @param value 락 키에서 사용할 이름 (생략시 파라미터 이름 사용)
 * @param property 객체에서 추출할 프로퍼티명 (예: "id", "user.id")
 * @param nullValue null일 때 사용할 기본값
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class LockKey(
    val value: String = "",
    val property: String = "",
    val nullValue: String = "null"
)
