package io.clroot.ball.adapter.inbound.event.consumer.domain

import io.clroot.ball.adapter.inbound.event.consumer.core.EventConsumerBase
import io.clroot.ball.adapter.inbound.event.consumer.core.EventHandlerRegistry
import io.clroot.ball.domain.event.DomainEvent
import kotlinx.coroutines.withTimeout
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 도메인 이벤트 소비자
 *
 * Spring ApplicationEvent 메커니즘을 통해 도메인 이벤트를 수신하고 처리합니다.
 * 주로 같은 프로세스 내에서 발생하는 도메인 이벤트들을 즉시 처리하는 역할을 담당합니다.
 *
 * 특징:
 * - Spring ApplicationEvent 기반
 * - 프로세스 내 이벤트 처리
 * - 트랜잭션 내/외 처리 지원
 * - 동기/비동기 처리 지원
 */
@Component
@Order(100) // 다른 EventListener들보다 늦게 실행 (도메인 로직 우선)
class DomainEventConsumer(
    private val domainProperties: DomainEventConsumerProperties,
    private val handlerRegistry: EventHandlerRegistry
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
            log.debug("Domain event consumer is disabled, skipping event: {}", event.type)
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
     * 실제 이벤트 핸들러들을 실행하는 템플릿 메서드 구현
     */
    override suspend fun executeEventHandlers(event: DomainEvent) {
        val handlers = handlerRegistry.getHandlers(event.javaClass)

        if (handlers.isEmpty()) {
            log.debug("No handlers found for domain event: {}", event.type)
            return
        }

        log.debug("Executing {} handlers for domain event: {}", handlers.size, event.type)

        // 타임아웃 적용하여 핸들러 실행
        withTimeout(domainProperties.timeoutMs) {
            for (handler in handlers) {
                try {
                    log.debug("Executing handler: {} for event: {}", handler.methodName, event.type)
                    handler.invoke(event)

                    // 성공 메트릭 기록
                    recordSuccessMetrics(event)

                } catch (e: Exception) {
                    log.error("Handler execution failed: {} for event: {}", handler.methodName, event.type, e)

                    // 개별 핸들러 실패가 다른 핸들러에 영향을 주지 않도록 처리
                    recordHandlerErrorMetrics(event, handler, e)

                    // 전체 처리를 중단할지 결정 (설정에 따라)
                    if (!domainProperties.enableRetry) {
                        throw e
                    }
                }
            }
        }
    }

    /**
     * 도메인 이벤트 전처리
     */
    override fun beforeEventProcessing(event: DomainEvent) {
        super.beforeEventProcessing(event)

        if (domainProperties.enableDebugLogging) {
            log.debug(
                "Starting domain event processing: {} (ID: {})",
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
            log.debug("Completed domain event processing: {} (ID: {})", event.type, event.id)
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
    private fun recordHandlerErrorMetrics(event: DomainEvent, handler: Any, error: Exception) {
        if (domainProperties.enableMetrics) {
            // TODO: Micrometer 메트릭 수집
            // counter("domain.events.handler.errors")
            //     .tag("event.type", event.type)
            //     .tag("handler", handler.methodName)
            //     .tag("error.type", error.javaClass.simpleName)
            //     .increment()
        }
    }
}
