package io.clroot.ball.adapter.inbound.event.consumer.core

import io.clroot.ball.domain.event.DomainEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * ThreadPool 기반 이벤트 소비자 기본 클래스
 * 
 * 코루틴 기반에서 ThreadPool 기반으로 완전히 변경되었습니다.
 * 모든 이벤트 소비자 구현체의 공통 기능을 제공합니다.
 * 템플릿 메서드 패턴을 사용하여 구체적인 이벤트 수신 방식은 하위 클래스에서 구현하도록 합니다.
 * 
 * 주요 기능:
 * - ThreadPool 기반 동기/비동기 이벤트 처리
 * - 공통 에러 핸들링
 * - 스레드 풀 관리
 * - 메트릭 수집 (확장 가능)
 */
abstract class EventConsumerBase(
    protected open val properties: EventConsumerProperties
) {
    
    protected val log: Logger = LoggerFactory.getLogger(javaClass)
    
    // ThreadPool 기반 실행자
    private val asyncExecutor: ScheduledExecutorService = Executors.newScheduledThreadPool(
        Runtime.getRuntime().availableProcessors(), 
        { r -> Thread(r, "event-consumer-${javaClass.simpleName}") }
    )

    /**
     * 도메인 이벤트 처리 (공통 로직)
     * 
     * @param event 처리할 도메인 이벤트
     */
    protected fun processEvent(event: DomainEvent) {
        if (properties.enableDebugLogging) {
            log.debug("Received domain event: {} (ID: {})", event.type, event.id)
        }

        try {
            // 이벤트 처리 전 검증 및 전처리
            beforeEventProcessing(event)
            
            if (properties.async) {
                processEventAsync(event)
            } else {
                processEventSync(event)
            }
            
            // 이벤트 처리 후 후처리
            afterEventProcessing(event)
            
        } catch (e: Exception) {
            log.error("Failed to process domain event: {} (ID: {})", event.type, event.id, e)
            handleEventError(event, e)
        }
    }

    /**
     * 이벤트 처리 전 전처리
     * 
     * 하위 클래스에서 필요시 오버라이드할 수 있습니다.
     * 예: 유효성 검증, 메트릭 수집 시작 등
     * 
     * @param event 처리할 도메인 이벤트
     */
    protected open fun beforeEventProcessing(event: DomainEvent) {
        // 기본 구현에서는 아무것도 하지 않음
    }

    /**
     * 이벤트 처리 후 후처리
     * 
     * 하위 클래스에서 필요시 오버라이드할 수 있습니다.
     * 예: 메트릭 수집 완료, 캐시 갱신 등
     * 
     * @param event 처리된 도메인 이벤트
     */
    protected open fun afterEventProcessing(event: DomainEvent) {
        // 기본 구현에서는 아무것도 하지 않음
    }

    /**
     * 동기적 이벤트 처리 - ThreadPool 기반
     */
    private fun processEventSync(event: DomainEvent) {
        executeEventHandlers(event)
    }

    /**
     * 비동기적 이벤트 처리 - ThreadPool 기반
     */
    private fun processEventAsync(event: DomainEvent) {
        CompletableFuture.runAsync({
            try {
                executeEventHandlers(event)
            } catch (e: Exception) {
                log.error("Async event processing failed for event {} (ID: {})", event.type, event.id, e)
                handleEventError(event, e)
            }
        }, asyncExecutor)
    }

    /**
     * 실제 이벤트 핸들러 실행 - ThreadPool 기반
     * 
     * 하위 클래스에서 구체적인 핸들러 실행 로직을 구현해야 합니다.
     * 
     * @param event 실행할 도메인 이벤트
     */
    protected abstract fun executeEventHandlers(event: DomainEvent)

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
        
        // 메트릭 수집
        recordErrorMetrics(event, error)
        
        // 재시도 로직 (필요시)
        if (properties.enableRetry && canRetry(error)) {
            scheduleRetry(event)
        }
    }

    /**
     * 재시도 가능 여부 판단
     */
    protected open fun canRetry(error: Exception): Boolean {
        return when (error) {
            is IllegalArgumentException -> false
            is IllegalStateException -> false
            // 일시적 오류는 재시도 가능
            else -> true
        }
    }

    /**
     * ThreadPool 기반 재시도 스케줄링
     */
    protected open fun scheduleRetry(event: DomainEvent) {
        val retryDelay = 1L // 1초 후 재시도
        
        log.warn("Scheduling retry for event: {} (ID: {}) in {} seconds", event.type, event.id, retryDelay)
        
        asyncExecutor.schedule({
            try {
                log.info("Retrying event processing: {} (ID: {})", event.type, event.id)
                processEvent(event)
            } catch (e: Exception) {
                log.error("Retry failed for event: {} (ID: {})", event.type, event.id, e)
            }
        }, retryDelay, TimeUnit.SECONDS)
    }

    /**
     * 성공 메트릭 기록
     */
    protected open fun recordSuccessMetrics(event: DomainEvent) {
        if (properties.enableMetrics) {
            // TODO: Micrometer 메트릭 수집
            // counter("events.processed.success")
            //     .tag("event.type", event.type)
            //     .increment()
        }
    }

    /**
     * 에러 메트릭 기록
     */
    protected open fun recordErrorMetrics(event: DomainEvent, error: Exception) {
        if (properties.enableMetrics) {
            // TODO: Micrometer 메트릭 수집
            // counter("events.processed.errors")
            //     .tag("event.type", event.type)
            //     .tag("error.type", error.javaClass.simpleName)
            //     .increment()
        }
    }

    /**
     * 소비자 종료 시 정리 작업 - ThreadPool 기반
     */
    open fun shutdown() {
        log.info("Shutting down ThreadPool-based event consumer...")
        
        try {
            asyncExecutor.shutdown()
            
            if (!asyncExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("ThreadPool did not terminate gracefully, forcing shutdown...")
                asyncExecutor.shutdownNow()
                
                if (!asyncExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.error("ThreadPool did not terminate even after forced shutdown")
                }
            }
            
            log.info("ThreadPool-based event consumer shutdown completed")
            
        } catch (e: InterruptedException) {
            log.warn("Shutdown interrupted, forcing immediate shutdown")
            asyncExecutor.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }
    
    /**
     * ThreadPool 상태 확인
     */
    fun getExecutorStatus(): String {
        return if (asyncExecutor.isShutdown) {
            "SHUTDOWN"
        } else if (asyncExecutor.isTerminated) {
            "TERMINATED"
        } else {
            "RUNNING"
        }
    }
}
