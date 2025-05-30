package io.clroot.ball.adapter.inbound.event.consumer.core

/**
 * 이벤트 핸들러 어노테이션
 *
 * 메서드가 이벤트 핸들러임을 표시하는 어노테이션입니다.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class EventHandler