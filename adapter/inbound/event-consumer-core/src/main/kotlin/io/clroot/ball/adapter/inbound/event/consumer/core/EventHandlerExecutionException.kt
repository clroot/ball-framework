package io.clroot.ball.adapter.inbound.event.consumer.core

/**
 * 이벤트 핸들러 실행 예외
 */
class EventHandlerExecutionException(message: String, cause: Throwable) : RuntimeException(message, cause)