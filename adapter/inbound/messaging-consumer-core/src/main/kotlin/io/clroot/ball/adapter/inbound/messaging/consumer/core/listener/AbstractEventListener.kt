package io.clroot.ball.adapter.inbound.messaging.consumer.core.listener

import io.clroot.ball.adapter.inbound.messaging.consumer.core.executor.DomainEventHandlerExecutor
import io.clroot.ball.adapter.inbound.messaging.consumer.core.properties.EventConsumerProperties
import io.clroot.ball.domain.event.DomainEvent
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

/**
 * 추상 이벤트 리스너
 * 
 * 모든 이벤트 리스너 구현체의 기본 클래스입니다.
 * 공통 이벤트 처리 로직을 제공하며, 구체적인 이벤트 수신 방식은 하위 클래스에서 구현합니다.
 * 
 * 주요 기능:
 * - 동기/비동기 이벤트 처리
 * - 공통 에러 핸들링
 * - 코루틴 스코프 관리
 */
abstract class AbstractEventListener(
    protected val handlerExecutor: DomainEventHandlerExecutor,
    protected val properties: EventConsumerProperties
) {
    protected val log = LoggerFactory.getLogger(javaClass)
    protected val eventScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * 도메인 이벤트 처리 (공통 로직)
     * 
     * @param event 처리할 도메인 이벤트
     */
    protected fun processEvent(event: DomainEvent) {
        log.debug("Received domain event: {} (ID: {})", event.type, event.id)

        try {
            if (properties.async) {
                processEventAsync(event)
            } else {
                processEventSync(event)
            }
        } catch (e: Exception) {
            log.error("Failed to process domain event: {} (ID: {})", event.type, event.id, e)
            handleEventError(event, e)
        }
    }

    /**
     * 동기적 이벤트 처리
     */
    private fun processEventSync(event: DomainEvent) {
        runBlocking {
            handlerExecutor.execute(event)
        }
    }

    /**
     * 비동기적 이벤트 처리
     */
    private fun processEventAsync(event: DomainEvent) {
        eventScope.launch {
            try {
                handlerExecutor.execute(event)
            } catch (e: Exception) {
                log.error("Async event processing failed for event {} (ID: {})", event.type, event.id, e)
                handleEventError(event, e)
            }
        }
    }

    /**
     * 이벤트 처리 에러 핸들링
     * 
     * 하위 클래스에서 필요에 따라 오버라이드할 수 있습니다.
     * 
     * @param event 처리에 실패한 이벤트
     * @param error 발생한 에러
     */
    protected open fun handleEventError(event: DomainEvent, error: Exception) {
        // 기본 에러 처리 로직
        log.error("Unhandled error processing event {} (ID: {})", event.type, event.id, error)
        
        // TODO: 필요시 메트릭 수집, 알림 발송 등 추가 로직
    }

    /**
     * 리스너 종료 시 정리 작업
     */
    open fun shutdown() {
        log.info("Shutting down event listener...")
        eventScope.cancel()
        log.info("Event listener shutdown completed")
    }
}
