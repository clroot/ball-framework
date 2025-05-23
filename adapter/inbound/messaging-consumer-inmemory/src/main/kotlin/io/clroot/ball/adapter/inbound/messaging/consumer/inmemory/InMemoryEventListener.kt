package io.clroot.ball.adapter.inbound.messaging.consumer.inmemory

import io.clroot.ball.adapter.shared.messaging.DomainEventWrapper
import io.clroot.ball.application.event.DomainEventHandler
import io.clroot.ball.application.event.BlockingDomainEventHandler
import io.clroot.ball.domain.event.DomainEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

/**
 * 인메모리 도메인 이벤트 리스너
 *
 * Spring ApplicationEvent로 발행된 도메인 이벤트를 수신하고,
 * 등록된 DomainEventHandler 들에게 직접 전달합니다.
 *
 * 순환 호출 방지:
 * - DomainEventDispatcher를 사용하지 않고 핸들러에 직접 전달
 * - Publisher와 완전히 분리된 책임
 *
 * 이 클래스는 Auto Configuration에 의해 자동으로 등록됩니다.
 */
open class InMemoryEventListener(
    private val handlerRegistry: DomainEventHandlerRegistry,
    private val blockingHandlerRegistry: BlockingDomainEventHandlerRegistry,
    private val properties: InMemoryEventConsumerProperties,
    private val blockingExecutor: Executor
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val eventScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Spring ApplicationEvent로 발행된 도메인 이벤트 처리
     */
    @EventListener
    @Async("eventTaskExecutor")
    open fun handleDomainEvent(wrapper: DomainEventWrapper) {
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
            processEventDirectly(event)
        }
    }

    /**
     * 비동기적 이벤트 처리
     */
    private fun handleEventAsync(event: DomainEvent) {
        eventScope.launch {
            processEventDirectly(event)
        }
    }

    /**
     * 이벤트를 핸들러에 직접 전달 (Dispatcher 거치지 않음)
     * suspend 핸들러와 blocking 핸들러를 모두 처리
     */
    private suspend fun processEventDirectly(event: DomainEvent) {
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
            processHandlersInParallel(event, suspendHandlers, blockingHandlers)
        } else {
            // 핸들러들을 순차적으로 실행
            processHandlersSequentially(event, suspendHandlers, blockingHandlers)
        }
    }

    /**
     * 핸들러들을 병렬로 실행
     */
    private suspend fun processHandlersInParallel(
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
                        executeHandlerDirectly(handler, event)
                    }
                }
            }

            // blocking 핸들러들 처리 (IO 디스패처 사용)
            val blockingJobs = blockingHandlers.map { handler ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        executeBlockingHandlerDirectly(handler, event)
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
    private suspend fun processHandlersSequentially(
        event: DomainEvent,
        suspendHandlers: List<DomainEventHandler<*>>,
        blockingHandlers: List<BlockingDomainEventHandler<*>>
    ) {
        // suspend 핸들러들 순차 실행
        suspendHandlers.forEach { handler ->
            executeHandlerDirectly(handler, event)
        }

        // blocking 핸들러들 순차 실행 (IO 디스패처 사용)
        blockingHandlers.forEach { handler ->
            withContext(Dispatchers.IO) {
                executeBlockingHandlerDirectly(handler, event)
            }
        }
    }

    /**
     * 개별 핸들러를 직접 실행 (Dispatcher 거치지 않음)
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun executeHandlerDirectly(
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
                handleRetry(handler, event, e)
            }
        }
    }

    /**
     * Blocking 핸들러를 직접 실행 (IO 디스패처에서 실행)
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun executeBlockingHandlerDirectly(
        handler: BlockingDomainEventHandler<*>,
        event: DomainEvent
    ) {
        try {
            val startTime = System.currentTimeMillis()

            // blocking handler는 suspend 함수가 아니므로 타임아웃 처리가 다름
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
     * 재시도 처리 (suspend 핸들러용)
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
    }

    /**
     * 재시도 처리 (blocking 핸들러용)
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
    }

    /**
     * 이벤트 처리 오류 핸들링
     */
    private fun handleEventError(event: DomainEvent, error: Exception) {
        // 여기서 데드 레터 큐나 오류 알림 등을 처리할 수 있음
        log.error("Unhandled error processing event {} (ID: {})", event.type, event.id, error)
    }
}
