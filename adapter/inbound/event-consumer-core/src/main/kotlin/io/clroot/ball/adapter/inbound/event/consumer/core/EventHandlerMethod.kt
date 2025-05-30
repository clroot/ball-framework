package io.clroot.ball.adapter.inbound.event.consumer.core

import io.clroot.ball.domain.event.DomainEvent
import java.lang.reflect.Method

/**
 * 이벤트 핸들러 메서드 정보를 담는 데이터 클래스
 */
data class EventHandlerMethod(
    val bean: Any,
    val method: Method,
    val eventType: Class<out DomainEvent>,
    val methodName: String
) {

    /**
     * 핸들러 메서드 실행
     */
    suspend fun invoke(event: DomainEvent) {
        try {
            method.isAccessible = true
            method.invoke(bean, event)
        } catch (e: Exception) {
            throw EventHandlerExecutionException("Failed to execute event handler: $methodName", e)
        }
    }
}