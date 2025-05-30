package io.clroot.ball.adapter.inbound.event.consumer.domain.test

/**
 * 구체적인 테스트용 이벤트 핸들러
 */
class TestDomainEventHandler : TestEventConsumerPortBase<TestDomainEvent>(TestDomainEvent::class) {

    private val executions = mutableListOf<TestDomainEvent>()
    private var shouldThrowException = false
    private var exceptionToThrow: RuntimeException? = null

    override suspend fun consume(event: TestDomainEvent) {
        handleEvent(event)
    }

    /**
     * 테스트용 non-suspend 핸들러 (리플렉션 문제 해결용)
     */
    fun handleEvent(event: TestDomainEvent) {
        if (shouldThrowException) {
            exceptionToThrow?.let { throw it } ?: throw RuntimeException("Test exception")
        }
        executions.add(event)
    }

    // 테스트 검증을 위한 메서드들
    fun getExecutionCount(): Int = executions.size
    fun wasExecuted(): Boolean = executions.isNotEmpty()
}