package io.clroot.ball.adapter.inbound.event.consumer.domain.test

import io.clroot.ball.application.port.inbound.ExecutorConfig
import org.springframework.stereotype.Component

/**
 * 구체적인 테스트용 이벤트 핸들러 - ThreadPool 기반
 * 
 * 코루틴 기반에서 ThreadPool 기반으로 완전히 변경되었습니다.
 */
@Component
open class TestDomainEventHandler : TestEventConsumerPortBase<TestDomainEvent>(TestDomainEvent::class) {

    private val executions = mutableListOf<TestDomainEvent>()
    private var shouldThrowException = false
    private var exceptionToThrow: RuntimeException? = null
    private var executionDelay: Long = 0  // 실행 지연 시간 (ms)

    /**
     * ThreadPool 기반 이벤트 처리 메서드
     */
    override fun consume(event: TestDomainEvent) {
        // 지연 시간이 설정되어 있다면 대기
        if (executionDelay > 0) {
            Thread.sleep(executionDelay)
        }
        
        if (shouldThrowException) {
            val exception = exceptionToThrow ?: RuntimeException("Test exception")
            recordErrorEvent(event, exception)  // 부모 클래스 메서드 사용
            throw exception
        }
        
        executions.add(event)
        recordProcessedEvent(event)  // 부모 클래스 메서드 사용
        
        println("✅ TestDomainEventHandler processed: ${event.message}")
    }

    // 테스트 제어 메서드들
    fun setThrowException(shouldThrow: Boolean, exception: RuntimeException? = null): TestDomainEventHandler {
        this.shouldThrowException = shouldThrow
        this.exceptionToThrow = exception
        return this
    }
    
    fun setExecutionDelay(delayMs: Long): TestDomainEventHandler {
        this.executionDelay = delayMs
        return this
    }
    
    // Fluent interface를 위한 오버라이드 메서드들
    override fun withExecutorConfig(config: ExecutorConfig): TestDomainEventHandler {
        super.withExecutorConfig(config)
        return this
    }
    
    override fun withOrder(order: Int): TestDomainEventHandler {
        super.withOrder(order)
        return this
    }
    
    override fun withHandlerName(name: String): TestDomainEventHandler {
        super.withHandlerName(name)
        return this
    }
    
    override fun withErrorHandler(handler: io.clroot.ball.application.port.inbound.EventErrorHandler): TestDomainEventHandler {
        super.withErrorHandler(handler)
        return this
    }

    // 테스트 검증을 위한 메서드들 - 충돌 없는 이름 사용
    fun getExecutionCount(): Int = executions.size
    fun wasExecuted(): Boolean = executions.isNotEmpty()
    fun getExecutedEvents(): List<TestDomainEvent> = executions.toList()
    fun getLastExecutedEvent(): TestDomainEvent? = executions.lastOrNull()
    
    // 부모 클래스 메서드들에 대한 편의 접근자
    fun getTestedProcessedEvents(): List<TestDomainEvent> = retrieveProcessedEvents()
    fun getTestedErrorEvents(): List<Pair<TestDomainEvent, Exception>> = retrieveErrorEvents()
    
    fun clearExecutions() {
        executions.clear()
        clearProcessedEvents()
    }
    
    // 빌더 패턴 지원
    companion object {
        fun create(): TestDomainEventHandler = TestDomainEventHandler()
        
        fun withConfig(config: ExecutorConfig): TestDomainEventHandler {
            val handler = create()
            handler.withExecutorConfig(config)
            return handler
        }
        
        fun withDelay(delayMs: Long): TestDomainEventHandler {
            return create().setExecutionDelay(delayMs)
        }
        
        fun withException(exception: RuntimeException = RuntimeException("Test error")): TestDomainEventHandler {
            return create().setThrowException(true, exception)
        }
    }
}
