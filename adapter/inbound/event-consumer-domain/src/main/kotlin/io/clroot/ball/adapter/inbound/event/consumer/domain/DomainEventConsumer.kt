package io.clroot.ball.adapter.inbound.event.consumer.domain

import io.clroot.ball.adapter.inbound.event.consumer.core.EventConsumerBase
import io.clroot.ball.adapter.inbound.event.consumer.core.EventHandlerRegistryInterface
import io.clroot.ball.adapter.inbound.event.consumer.core.ThreadPoolEventHandlerMethod
import io.clroot.ball.domain.event.DomainEvent
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Spring ApplicationEvent 기반 도메인 이벤트 소비자 - ThreadPool 기반
 *
 * 코루틴 기반에서 ThreadPool 기반으로 완전히 변경되었습니다.
 * Spring의 ApplicationEventPublisher 메커니즘을 통해 발행된 도메인 이벤트를 수신하고 처리합니다.
 * 주로 같은 프로세스 내에서 발생하는 도메인 이벤트들을 즉시 처리하는 역할을 담당합니다.
 *
 * ThreadPool 기반 특징:
 * - JPA와 자연스러운 연동
 * - 단순한 blocking I/O 처리
 * - 예측 가능한 리소스 관리
 * - 향상된 디버깅 경험
 * 
 * vs 외부 메시징 시스템:
 * - SpringDomainEventConsumer: 프로세스 내, ThreadPool 기반, 높은 성능
 * - KafkaEventConsumer: 프로세스 간, 내구성, 확장성 (미래 구현)
 * 
 * 사용 용도:
 * - 도메인 로직 내부의 이벤트 처리
 * - 같은 프로세스 내의 다른 컴포넌트와의 통신
 * - 즉시 처리가 필요한 비즈니스 이벤트
 * - 개발/테스트 환경에서의 단순한 이벤트 처리
 */
@Component
@Order(100) // 다른 EventListener들보다 늦게 실행 (도메인 로직 우선)
class SpringDomainEventConsumer(
    private val domainProperties: DomainEventConsumerProperties,
    private val handlerRegistry: EventHandlerRegistryInterface
) : EventConsumerBase(domainProperties) {

    /**
     * Spring ApplicationEvent로 발행된 도메인 이벤트 수신
     *
     * 트랜잭션이 활성화된 상태에서 즉시 처리됩니다.
     * @TransactionalEventListener를 사용하지 않으므로 트랜잭션 커밋 전에 실행됩니다.
     */
    @EventListener
    fun handleDomainEvent(event: DomainEvent) {
        if (!domainProperties.enabled) {
            log.debug("Spring domain event consumer is disabled, skipping event: {}", event.type)
            return
        }

        if (domainProperties.processInTransaction && !domainProperties.processAfterCommit) {
            // 트랜잭션 내에서 즉시 처리
            processEvent(event)
        }
    }

    /**
     * 트랜잭션 커밋 후 도메인 이벤트 처리 (선택적)
     *
     * processAfterCommit 설정이 true일 때만 활성화됩니다.
     * 주로 외부 시스템과의 연동이나 사이드 이펙트 처리에 사용됩니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleDomainEventAfterCommit(event: DomainEvent) {
        if (!domainProperties.enabled) return

        if (domainProperties.processAfterCommit) {
            log.debug("Processing domain event after transaction commit: {}", event.type)
            processEvent(event)
        }
    }

    /**
     * 트랜잭션 롤백 시 처리 (필요시)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    fun handleDomainEventAfterRollback(event: DomainEvent) {
        if (!domainProperties.enabled) return

        log.debug("Domain event processing skipped due to transaction rollback: {}", event.type)
        // 롤백 시 보상 로직이 필요하면 여기서 처리
    }

    /**
     * 실제 이벤트 핸들러들을 실행하는 ThreadPool 기반 구현
     */
    override fun executeEventHandlers(event: DomainEvent) {
        val handlers = handlerRegistry.getHandlers(event.javaClass)

        if (handlers.isEmpty()) {
            log.debug("No handlers found for domain event: {}", event.type)
            return
        }

        // 핸들러들을 order 기준으로 정렬 (낮은 order가 먼저 실행)
        val sortedHandlers = handlers.sorted()
        
        log.debug("Executing {} handlers for domain event: {} (order: {})", 
            sortedHandlers.size, event.type, sortedHandlers.map { "${it.methodName}(${it.order})" })

        // ThreadPool 기반 핸들러 실행
        executeHandlersWithThreadPool(event, sortedHandlers)
    }

    /**
     * ThreadPool 기반 핸들러 실행
     */
    private fun executeHandlersWithThreadPool(event: DomainEvent, handlers: List<ThreadPoolEventHandlerMethod>) {
        val futures = mutableListOf<CompletableFuture<Void>>()
        
        for (handler in handlers) {
            try {
                log.debug("Executing handler: {} (order={}) for event: {}",
                    handler.methodName, handler.order, event.type)
                
                // ThreadPool에서 비동기 실행
                val future = if (domainProperties.async) {
                    handler.submit(event)
                } else {
                    // 동기 실행
                    CompletableFuture.runAsync {
                        handler.invoke(event)
                    }
                }
                
                futures.add(future)

                // 성공 메트릭 기록 (비동기로)
                future.thenRun { recordSuccessMetrics(event) }

            } catch (e: Exception) {
                log.error("Handler execution failed: {} for event: {}", handler.methodName, event.type, e)

                // 개별 핸들러 실패가 다른 핸들러에 영향을 주지 않도록 처리
                recordHandlerErrorMetrics(event, handler, e)

                // 전체 처리를 중단할지 결정 (설정에 따라)
                if (!domainProperties.continueOnError) {
                    log.warn("Stopping handler execution due to error and continueOnError=false")
                    throw e
                } else {
                    log.debug("Continuing with next handler despite error (continueOnError=true)")
                }
            }
        }
        
        // 동기 모드인 경우 모든 작업 완료 대기
        if (!domainProperties.async) {
            waitForCompletion(futures, event)
        }
    }

    /**
     * 모든 핸들러 완료 대기 (동기 모드)
     */
    private fun waitForCompletion(futures: List<CompletableFuture<Void>>, event: DomainEvent) {
        try {
            val allTasks = CompletableFuture.allOf(*futures.toTypedArray())
            allTasks.get(domainProperties.timeoutMs, TimeUnit.MILLISECONDS)
            
            log.debug("All handlers completed for event: {}", event.type)
            
        } catch (e: Exception) {
            log.error("Timeout or error waiting for handlers to complete for event: {}", event.type, e)
            throw e
        }
    }

    /**
     * 도메인 이벤트 전처리
     */
    override fun beforeEventProcessing(event: DomainEvent) {
        super.beforeEventProcessing(event)

        if (domainProperties.enableDebugLogging) {
            log.debug(
                "[SPRING-THREADPOOL] Starting domain event processing: {} (ID: {})",
                event.type, event.id
            )
        }
    }

    /**
     * 도메인 이벤트 후처리
     */
    override fun afterEventProcessing(event: DomainEvent) {
        super.afterEventProcessing(event)

        if (domainProperties.enableDebugLogging) {
            log.debug("[SPRING-THREADPOOL] Completed domain event processing: {} (ID: {})", event.type, event.id)
        }

        // 도메인 이벤트 특화 후처리 로직 (예: 캐시 갱신)
        updateDomainEventCache(event)
    }

    /**
     * 도메인 이벤트 캐시 갱신 (필요시)
     */
    private fun updateDomainEventCache(event: DomainEvent) {
        // TODO: 도메인 이벤트 처리 결과에 따른 캐시 갱신 로직
        // 예: 애그리게이트 상태 캐시 갱신
    }

    /**
     * 핸들러별 에러 메트릭 기록
     */
    private fun recordHandlerErrorMetrics(event: DomainEvent, handler: ThreadPoolEventHandlerMethod, error: Exception) {
        if (domainProperties.enableMetrics) {
            // TODO: Micrometer 메트릭 수집
            // counter("spring.domain.events.handler.errors")
            //     .tag("event.type", event.type)
            //     .tag("handler", handler.methodName)
            //     .tag("handler.order", handler.order.toString())
            //     .tag("error.type", error.javaClass.simpleName)
            //     .increment()
        }
    }
    
    /**
     * 모든 핸들러의 메트릭 수집
     */
    fun getAllHandlerMetrics(): Map<String, io.clroot.ball.adapter.inbound.event.consumer.core.EventHandlerMetrics> {
        val metrics = mutableMapOf<String, io.clroot.ball.adapter.inbound.event.consumer.core.EventHandlerMetrics>()
        
        handlerRegistry.getAllHandledEventTypes().forEach { eventType ->
            val handlers = handlerRegistry.getHandlers(eventType)
            handlers.forEach { handler ->
                metrics[handler.methodName] = handler.getMetrics()
            }
        }
        
        return metrics
    }
    
    /**
     * 핸들러 메트릭 요약 출력
     */
    fun printHandlerMetrics() {
        println("\n📊 SpringDomainEventConsumer Handler Metrics:")
        println("=" * 70)
        
        val allMetrics = getAllHandlerMetrics()
        
        if (allMetrics.isEmpty()) {
            println("No handlers found.")
            return
        }
        
        allMetrics.values.forEach { metrics ->
            println(metrics.summary())
        }
        
        println("=" * 70)
    }
}

/**
 * @deprecated SpringDomainEventConsumer를 사용하세요.
 * 
 * 이 별칭은 호환성을 위해 제공되지만, 새로운 코드에서는 SpringDomainEventConsumer를 직접 사용하는 것을 권장합니다.
 */
@Deprecated(
    message = "Use SpringDomainEventConsumer instead",
    replaceWith = ReplaceWith("SpringDomainEventConsumer"),
    level = DeprecationLevel.WARNING
)
typealias DomainEventConsumer = SpringDomainEventConsumer

// 유틸리티 확장 함수
private operator fun String.times(n: Int): String = this.repeat(n)
