package io.clroot.ball.adapter.inbound.event.consumer.domain.test

import io.clroot.ball.application.port.inbound.EventConsumerPort
import io.clroot.ball.application.port.inbound.ExecutorConfig
import io.clroot.ball.application.port.inbound.EventErrorHandler
import io.clroot.ball.domain.event.Event
import kotlin.reflect.KClass

/**
 * 테스트용 EventConsumerPort 구현체 - ThreadPool 기반
 * 
 * 코루틴 기반에서 ThreadPool 기반으로 완전히 변경되었습니다.
 */
abstract class TestEventConsumerPortBase<T : Event>(
    private val eventTypeClass: KClass<T>
) : EventConsumerPort<T> {

    override val eventType: KClass<T> = eventTypeClass

    // ThreadPool 기반 설정
    protected var _executorConfig: ExecutorConfig = ExecutorConfig.conservative()
    protected var _order: Int = 0
    protected var _handlerName: String = javaClass.simpleName
    protected var _errorHandler: EventErrorHandler = EventErrorHandler.default()

    override val executorConfig: ExecutorConfig get() = _executorConfig
    override val order: Int get() = _order
    override val handlerName: String get() = _handlerName
    override val errorHandler: EventErrorHandler get() = _errorHandler
    
    // 테스트용 이벤트 추적 - 완전히 다른 이름 사용
    private val _trackedProcessedEvents = mutableListOf<T>()
    private val _trackedErrorEvents = mutableListOf<Pair<T, Exception>>()
    
    /**
     * 테스트용 설정 메서드들
     */
    open fun withExecutorConfig(config: ExecutorConfig): TestEventConsumerPortBase<T> {
        _executorConfig = config
        return this
    }
    
    open fun withOrder(order: Int): TestEventConsumerPortBase<T> {
        _order = order
        return this
    }
    
    open fun withHandlerName(name: String): TestEventConsumerPortBase<T> {
        _handlerName = name
        return this
    }
    
    open fun withErrorHandler(handler: EventErrorHandler): TestEventConsumerPortBase<T> {
        _errorHandler = handler
        return this
    }
    
    /**
     * 테스트용 편의 메서드 - 완전히 다른 이름으로 충돌 방지
     */
    fun retrieveProcessedEvents(): List<T> = _trackedProcessedEvents.toList()
    
    fun retrieveErrorEvents(): List<Pair<T, Exception>> = _trackedErrorEvents.toList()
    
    fun getProcessedEventCount(): Int = _trackedProcessedEvents.size
    
    fun getErrorEventCount(): Int = _trackedErrorEvents.size
    
    fun clearProcessedEvents() {
        _trackedProcessedEvents.clear()
        _trackedErrorEvents.clear()
    }
    
    fun wasEventProcessed(event: T): Boolean = _trackedProcessedEvents.contains(event)
    
    /**
     * 하위 클래스에서 이벤트 처리 시 호출할 메서드들
     */
    protected fun recordProcessedEvent(event: T) {
        _trackedProcessedEvents.add(event)
    }
    
    protected fun recordErrorEvent(event: T, exception: Exception) {
        _trackedErrorEvents.add(event to exception)
    }
}
