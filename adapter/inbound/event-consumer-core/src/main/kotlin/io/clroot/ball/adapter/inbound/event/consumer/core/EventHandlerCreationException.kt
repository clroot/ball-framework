package io.clroot.ball.adapter.inbound.event.consumer.core

/**
 * 이벤트 핸들러 생성 예외
 * 
 * EventHandlerMethod나 ThreadPoolEventHandlerMethod 생성 시 발생하는 예외
 */
class EventHandlerCreationException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
