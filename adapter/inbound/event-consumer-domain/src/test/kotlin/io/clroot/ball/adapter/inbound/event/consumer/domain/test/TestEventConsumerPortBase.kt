package io.clroot.ball.adapter.inbound.event.consumer.domain.test

import io.clroot.ball.application.port.inbound.EventConsumerPort
import io.clroot.ball.domain.event.Event
import kotlin.reflect.KClass

/**
 * 테스트용 EventConsumerPort 구현체
 */
abstract class TestEventConsumerPortBase<T : Event>(
    private val eventTypeClass: KClass<T>
) : EventConsumerPort<T> {

    override val eventType: KClass<T> = eventTypeClass

    protected var _async: Boolean = false
    protected var _order: Int = 0
    protected var _handlerName: String = javaClass.simpleName

    override val async: Boolean get() = _async
    override val order: Int get() = _order
    override val handlerName: String get() = _handlerName
}