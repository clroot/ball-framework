package io.clroot.ball.shared.lock

import java.util.concurrent.TimeUnit

/**
 * 분산 락을 적용할 메서드에 사용하는 어노테이션
 *
 * @param key 락 키 템플릿 (예: "user-{userId}", "payment-{userId}-{paymentId}")
 * @param timeUnit 시간 단위
 * @param waitTime 락 획득 대기 시간
 * @param leaseTime 락 유지 시간
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class DistributedLock(
    val key: String,
    val timeUnit: TimeUnit = TimeUnit.SECONDS,
    val waitTime: Long = 10L,
    val leaseTime: Long = 3L,
)
