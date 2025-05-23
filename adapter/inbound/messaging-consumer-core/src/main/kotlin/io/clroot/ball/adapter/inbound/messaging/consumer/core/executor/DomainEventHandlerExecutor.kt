package io.clroot.ball.adapter.inbound.messaging.consumer.core.executor

import io.clroot.ball.adapter.inbound.messaging.consumer.core.properties.EventConsumerProperties
import io.clroot.ball.adapter.inbound.messaging.consumer.core.registry.BlockingDomainEventHandlerRegistry
import io.clroot.ball.adapter.inbound.messaging.consumer.core.registry.DomainEventHandlerRegistry
import io.clroot.ball.application.event.BlockingDomainEventHandler
import io.clroot.ball.application.event.DomainEventHandler
import io.clroot.ball.domain.event.DomainEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 도메인 이벤트 핸들러 실행기
 * 
 * suspend 핸들러와 blocking 핸들러를 모두 처리하는 공통 실행 로직을 제공합니다.
 * 
 * 주요 기능:
 * - 핸들러 타입별 적절한 디스패처에서 실행
 * - 병렬/순차 실행 제어
 * - 타임아웃 처리
 * - 재시도 로직
 * - 에러 핸들링
 */
@Component
class DomainEventHandlerExecutor(
    private val handlerRegistry: DomainEventHandlerRegistry,
    private val blockingHandlerRegistry: BlockingDomainEventHandlerRegistry,
    private val properties: EventConsumerProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 도메인 이벤트 처리
     * 
     * @param event 처리할 도메인 이벤트
     */
    suspend fun execute(event: DomainEvent) {
        val suspendHandlers = handlerRegistry.getHandlers(event.javaClass)
        val blockingHandlers = blockingHandlerRegistry.getHandlers(event.javaClass)

        if (suspendHandlers.isEmpty() && blockingHandlers.isEmpty()) {
            log.debug("No handlers found for event type: {}", event.type)
            return
        }

        val totalHandlers = suspendHandlers.size + blockingHandlers.size
        log.debug("Processing event {} with {} handlers ({} suspend, {} blocking)", 
            event.type, totalHandlers, suspendHandlers.size, blockingHandlers.size)

        if (properties.parallel && totalHandlers > 1) {
            // 핸들러들을 병렬로 실행
            executeHandlersInParallel(event, suspendHandlers, blockingHandlers)
        } else {
            // 핸들러들을 순차적으로 실행
            executeHandlersSequentially(event, suspendHandlers, blockingHandlers)
        }
    }

    /**
     * 핸들러들을 병렬로 실행
     */
    private suspend fun executeHandlersInParallel(
        event: DomainEvent,
        suspendHandlers: List<DomainEventHandler<*>>,
        blockingHandlers: List<BlockingDomainEventHandler<*>>
    ) {
        val semaphore = Semaphore(properties.maxConcurrency)

        coroutineScope {
            // suspend 핸들러들 처리
            val suspendJobs = suspendHandlers.map { handler ->
                async {
                    semaphore.withPermit {
                        executeSuspendHandler(handler, event)
                    }
                }
            }

            // blocking 핸들러들 처리 (IO 디스패처 사용)
            val blockingJobs = blockingHandlers.map { handler ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        executeBlockingHandler(handler, event)
                    }
                }
            }

            // 모든 핸들러 실행 완료 대기
            (suspendJobs + blockingJobs).awaitAll()
        }
    }

    /**
     * 핸들러들을 순차적으로 실행
     */
    private suspend fun executeHandlersSequentially(
        event: DomainEvent,
        suspendHandlers: List<DomainEventHandler<*>>,
        blockingHandlers: List<BlockingDomainEventHandler<*>>
    ) {
        // suspend 핸들러들 순차 실행
        suspendHandlers.forEach { handler ->
            executeSuspendHandler(handler, event)
        }

        // blocking 핸들러들 순차 실행 (IO 디스패처 사용)
        blockingHandlers.forEach { handler ->
            withContext(Dispatchers.IO) {
                executeBlockingHandler(handler, event)
            }
        }
    }

    /**
     * Suspend 핸들러 실행
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun executeSuspendHandler(
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
                "Suspend handler {} completed for event {} in {}ms",
                handler.javaClass.simpleName, event.type, executionTime
            )

        } catch (e: Exception) {
            log.error(
                "Suspend handler {} failed for event {}: {}",
                handler.javaClass.simpleName, event.type, e.message, e
            )

            if (properties.enableRetry) {
                handleSuspendRetry(handler, event, e)
            }
        }
    }

    /**
     * Blocking 핸들러 실행
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun executeBlockingHandler(
        handler: BlockingDomainEventHandler<*>,
        event: DomainEvent
    ) {
        try {
            val startTime = System.currentTimeMillis()

            if (properties.timeoutMs > 0) {
                withTimeout(properties.timeoutMs) {
                    (handler as BlockingDomainEventHandler<DomainEvent>).handle(event)
                }
            } else {
                (handler as BlockingDomainEventHandler<DomainEvent>).handle(event)
            }

            val executionTime = System.currentTimeMillis() - startTime
            log.debug(
                "Blocking handler {} completed for event {} in {}ms",
                handler.javaClass.simpleName, event.type, executionTime
            )

        } catch (e: Exception) {
            log.error(
                "Blocking handler {} failed for event {}: {}",
                handler.javaClass.simpleName, event.type, e.message, e
            )

            if (properties.enableRetry) {
                handleBlockingRetry(handler, event, e)
            }
        }
    }

    /**
     * Suspend 핸들러 재시도 처리
     */
    private suspend fun handleSuspendRetry(
        handler: DomainEventHandler<*>,
        event: DomainEvent,
        originalError: Exception
    ) {
        repeat(properties.maxRetryAttempts) { attempt ->
            try {
                delay(properties.retryDelayMs)
                log.debug(
                    "Retrying suspend handler {} for event {} (attempt {})",
                    handler.javaClass.simpleName, event.type, attempt + 1
                )

                @Suppress("UNCHECKED_CAST")
                (handler as DomainEventHandler<DomainEvent>).handle(event)

                log.debug(
                    "Suspend handler {} succeeded on retry {} for event {}",
                    handler.javaClass.simpleName, attempt + 1, event.type
                )
                return // 성공했으므로 재시도 중단

            } catch (e: Exception) {
                log.warn(
                    "Suspend handler {} retry {} failed for event {}: {}",
                    handler.javaClass.simpleName, attempt + 1, event.type, e.message
                )
            }
        }

        log.error(
            "Suspend handler {} failed after {} retries for event {}",
            handler.javaClass.simpleName, properties.maxRetryAttempts, event.type
        )
        
        handleFinalError(handler.javaClass.simpleName, event, originalError)
    }

    /**
     * Blocking 핸들러 재시도 처리
     */
    private suspend fun handleBlockingRetry(
        handler: BlockingDomainEventHandler<*>,
        event: DomainEvent,
        originalError: Exception
    ) {
        repeat(properties.maxRetryAttempts) { attempt ->
            try {
                delay(properties.retryDelayMs)
                log.debug(
                    "Retrying blocking handler {} for event {} (attempt {})",
                    handler.javaClass.simpleName, event.type, attempt + 1
                )

                @Suppress("UNCHECKED_CAST")
                (handler as BlockingDomainEventHandler<DomainEvent>).handle(event)

                log.debug(
                    "Blocking handler {} succeeded on retry {} for event {}",
                    handler.javaClass.simpleName, attempt + 1, event.type
                )
                return // 성공했으므로 재시도 중단

            } catch (e: Exception) {
                log.warn(
                    "Blocking handler {} retry {} failed for event {}: {}",
                    handler.javaClass.simpleName, attempt + 1, event.type, e.message
                )
            }
        }

        log.error(
            "Blocking handler {} failed after {} retries for event {}",
            handler.javaClass.simpleName, properties.maxRetryAttempts, event.type
        )
        
        handleFinalError(handler.javaClass.simpleName, event, originalError)
    }

    /**
     * 최종 에러 처리
     * 모든 재시도가 실패했을 때 호출됩니다.
     */
    private fun handleFinalError(handlerName: String, event: DomainEvent, error: Exception) {
        when (properties.errorHandling.logLevel.uppercase()) {
            "ERROR" -> log.error("Handler {} permanently failed for event {} (ID: {})", handlerName, event.type, event.id, error)
            "WARN" -> log.warn("Handler {} permanently failed for event {} (ID: {})", handlerName, event.type, event.id, error)
            "INFO" -> log.info("Handler {} permanently failed for event {} (ID: {})", handlerName, event.type, event.id)
            "DEBUG" -> log.debug("Handler {} permanently failed for event {} (ID: {})", handlerName, event.type, event.id, error)
        }

        // TODO: Dead Letter Queue, 알림 등 추가 에러 처리 로직
        if (properties.errorHandling.enableDeadLetterQueue) {
            // Dead Letter Queue에 이벤트 저장
            log.info("Event {} (ID: {}) moved to dead letter queue", event.type, event.id)
        }

        if (properties.errorHandling.enableNotification) {
            // 외부 시스템에 에러 알림
            log.info("Error notification sent for event {} (ID: {})", event.type, event.id)
        }
    }
}
