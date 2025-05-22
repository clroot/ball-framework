package io.clroot.ball.adapter.inbound.messaging.consumer.inmemory

import io.clroot.ball.adapter.shared.messaging.DomainEventWrapper
import io.clroot.ball.application.event.DomainEventHandler
import io.clroot.ball.domain.event.DomainEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

/**
 * 인메모리 도메인 이벤트 리스너
 * 
 * Spring ApplicationEvent로 발행된 도메인 이벤트를 수신하고,
 * 등록된 DomainEventHandler들에게 전달합니다.
 * 
 * 이 클래스는 Auto Configuration에 의해 자동으로 등록됩니다.
 */
@Component
class InMemoryEventListener(
    private val handlerRegistry: DomainEventHandlerRegistry,
    private val properties: InMemoryEventConsumerProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val eventScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Spring ApplicationEvent로 발행된 도메인 이벤트 처리
     */
    @EventListener
    @Async("eventTaskExecutor")
    fun handleDomainEvent(wrapper: DomainEventWrapper) {
        val event = wrapper.domainEvent
        log.debug("Received domain event: {} (ID: {})", event.type, event.id)

        try {
            if (properties.async) {
                handleEventAsync(event)
            } else {
                handleEventSync(event)
            }
        } catch (e: Exception) {
            log.error("Failed to handle domain event: {} (ID: {})", event.type, event.id, e)
            handleEventError(event, e)
        }
    }

    /**
     * 동기적 이벤트 처리
     */
    private fun handleEventSync(event: DomainEvent) {
        runBlocking {
            processEvent(event)
        }
    }

    /**
     * 비동기적 이벤트 처리
     */
    private fun handleEventAsync(event: DomainEvent) {
        if (properties.parallel) {
            // 병렬 처리
            eventScope.launch {
                processEvent(event)
            }
        } else {
            // 순차 처리
            eventScope.launch {
                processEvent(event)
            }
        }
    }

    /**
     * 실제 이벤트 처리 로직
     */
    private suspend fun processEvent(event: DomainEvent) {
        val handlers = handlerRegistry.getHandlers(event.javaClass)

        if (handlers.isEmpty()) {
            log.debug("No handlers found for event type: {}", event.type)
            return
        }

        log.debug("Processing event {} with {} handlers", event.type, handlers.size)

        if (properties.parallel && handlers.size > 1) {
            // 핸들러들을 병렬로 실행
            processHandlersInParallel(event, handlers)
        } else {
            // 핸들러들을 순차적으로 실행
            processHandlersSequentially(event, handlers)
        }
    }

    /**
     * 핸들러들을 병렬로 실행
     */
    private suspend fun processHandlersInParallel(
        event: DomainEvent,
        handlers: List<DomainEventHandler<*>>
    ) {
        val semaphore = Semaphore(properties.maxConcurrency)
        coroutineScope {
            handlers.map { handler ->
                async {
                    semaphore.withPermit {
                        executeHandler(handler, event)
                    }
                }
            }.awaitAll()
        }
    }

    /**
     * 핸들러들을 순차적으로 실행
     */
    private suspend fun processHandlersSequentially(
        event: DomainEvent,
        handlers: List<DomainEventHandler<*>>
    ) {
        handlers.forEach { handler ->
            executeHandler(handler, event)
        }
    }

    /**
     * 개별 핸들러 실행
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun executeHandler(
        handler: DomainEventHandler<*>,
        event: DomainEvent
    ) {
        try {
            val startTime = System.currentTimeMillis()

            if (properties.timeoutMs > 0) {
                withTimeout(properties.timeoutMs) {
                    (handler as DomainEventHandler<DomainEvent>).handle(event)
                }
            } else {
                (handler as DomainEventHandler<DomainEvent>).handle(event)
            }

            val executionTime = System.currentTimeMillis() - startTime
            log.debug(
                "Handler {} completed for event {} in {}ms",
                handler.javaClass.simpleName, event.type, executionTime
            )

        } catch (e: Exception) {
            log.error(
                "Handler {} failed for event {}: {}",
                handler.javaClass.simpleName, event.type, e.message, e
            )

            if (properties.enableRetry) {
                handleRetry(handler, event, e)
            }
        }
    }

    /**
     * 재시도 처리
     */
    private suspend fun handleRetry(
        handler: DomainEventHandler<*>,
        event: DomainEvent,
        originalError: Exception
    ) {
        repeat(properties.maxRetryAttempts) { attempt ->
            try {
                delay(properties.retryDelayMs)
                log.debug(
                    "Retrying handler {} for event {} (attempt {})",
                    handler.javaClass.simpleName, event.type, attempt + 1
                )

                @Suppress("UNCHECKED_CAST")
                (handler as DomainEventHandler<DomainEvent>).handle(event)

                log.debug(
                    "Handler {} succeeded on retry {} for event {}",
                    handler.javaClass.simpleName, attempt + 1, event.type
                )
                return // 성공했으므로 재시도 중단

            } catch (e: Exception) {
                log.warn(
                    "Handler {} retry {} failed for event {}: {}",
                    handler.javaClass.simpleName, attempt + 1, event.type, e.message
                )
            }
        }

        log.error(
            "Handler {} failed after {} retries for event {}",
            handler.javaClass.simpleName, properties.maxRetryAttempts, event.type
        )
    }

    /**
     * 이벤트 처리 오류 핸들링
     */
    private fun handleEventError(event: DomainEvent, error: Exception) {
        // 여기서 데드 레터 큐나 오류 알림 등을 처리할 수 있음
        log.error("Unhandled error processing event {} (ID: {})", event.type, event.id, error)
    }
}
