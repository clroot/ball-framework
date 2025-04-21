package io.clroot.ball.adapter.inbound.messaging.core.annotation

import org.springframework.stereotype.Component

/**
 * 메시지 소비자 컴포넌트 지정 애노테이션
 * 메시지 소비자 클래스에 적용하여 자동 등록 및 토픽 지정
 *
 * @property topic 처리할 메시지 토픽 이름
 * @property autoRegister 자동 등록 여부 (기본값: true)
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Component
annotation class MessageConsumerComponent(
    val topic: String = "",
    val autoRegister: Boolean = true
)