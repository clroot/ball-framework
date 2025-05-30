package io.clroot.ball.adapter.inbound.event.consumer.core

import io.clroot.ball.application.port.inbound.ErrorAction
import io.clroot.ball.application.port.inbound.EventConsumerPort
import io.clroot.ball.domain.event.DomainEvent
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.atomic.AtomicLong

/**
 * 스레드 풀 기반 이벤트 핸들러 메서드
 * 
 * 기존 코루틴 기반에서 ThreadPool 기반으로 완전히 변경되었습니다.
 * - 단순하고 직관적인 블로킹 I/O 지원
 * - JPA와 자연스러운 연동
 * - 예측 가능한 리소스 관리
 */
class ThreadPoolEventHandlerMethod(
    val port: EventConsumerPort<*>,  // private 제거하여 테스트 접근 가능
    val eventType: Class<out DomainEvent>,
    val methodName: String,
    val order: Int = 0
) : Comparable<ThreadPoolEventHandlerMethod> {

    // 전용 스레드 풀
    private val executor: ThreadPoolTaskExecutor by lazy {
        createThreadPoolExecutor()
    }
    
    // 메트릭 수집
    private val processedCount = AtomicLong(0)
    private val errorCount = AtomicLong(0)
    private val retryCount = AtomicLong(0)
    private val skippedCount = AtomicLong(0)

    /**
     * 이벤트 처리 (비동기) - 실제 운영에서 사용
     */
    fun submit(event: DomainEvent): CompletableFuture<Void> {
        return CompletableFuture.runAsync({
            processEventWithRetry(event)
        }, executor)
    }
    
    /**
     * 이벤트 처리 (동기) - 테스트에서 사용
     */
    fun invoke(event: DomainEvent) {
        processEventWithRetry(event)
    }

    /**
     * 재시도 로직을 포함한 이벤트 처리
     */
    private fun processEventWithRetry(event: DomainEvent) {
        var attempt = 1
        var lastException: Throwable? = null
        
        while (attempt <= 10) { // 최대 10회 시도 (에러 핸들러에서 제어)
            try {
                // 실제 이벤트 처리
                @Suppress("UNCHECKED_CAST")
                (port as EventConsumerPort<DomainEvent>).consume(event)
                
                processedCount.incrementAndGet()
                
                if (attempt > 1) {
                    println("✅ Event processing succeeded on attempt $attempt: ${event.javaClass.simpleName}")
                }
                return
                
            } catch (e: Exception) {
                lastException = e
                
                val errorAction = port.errorHandler.handleError(event, e, attempt)
                
                when (errorAction) {
                    ErrorAction.RETRY -> {
                        retryCount.incrementAndGet()
                        attempt++
                        continue
                    }
                    ErrorAction.SKIP -> {
                        skippedCount.incrementAndGet()
                        println("⏭️ Skipping event after error: ${event.javaClass.simpleName}")
                        return
                    }
                    ErrorAction.FAIL -> {
                        errorCount.incrementAndGet()
                        throw EventHandlerExecutionException(
                            "Event processing failed: $methodName", e
                        )
                    }
                }
            }
        }
        
        // 모든 재시도 실패
        errorCount.incrementAndGet()
        throw EventHandlerExecutionException(
            "Event processing failed after $attempt attempts: $methodName", 
            lastException ?: IllegalStateException("Event processing failed after $attempt attempts: $methodName")
        )
    }

    /**
     * 전용 스레드 풀 생성
     */
    private fun createThreadPoolExecutor(): ThreadPoolTaskExecutor {
        return ThreadPoolTaskExecutor().apply {
            val config = port.executorConfig
            
            corePoolSize = config.corePoolSize
            maxPoolSize = config.maxPoolSize
            queueCapacity = config.queueCapacity
            keepAliveSeconds = config.keepAliveSeconds.toInt()
            setThreadNamePrefix("${config.threadNamePrefix}-${port.handlerName}-")
            setAllowCoreThreadTimeOut(config.allowCoreThreadTimeOut)
            
            // Rejection Policy 설정
            when (config.rejectionPolicy) {
                io.clroot.ball.application.port.inbound.RejectionPolicy.CALLER_RUNS -> 
                    setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
                io.clroot.ball.application.port.inbound.RejectionPolicy.DISCARD_OLDEST -> 
                    setRejectedExecutionHandler(ThreadPoolExecutor.DiscardOldestPolicy())
                io.clroot.ball.application.port.inbound.RejectionPolicy.DISCARD -> 
                    setRejectedExecutionHandler(ThreadPoolExecutor.DiscardPolicy())
                io.clroot.ball.application.port.inbound.RejectionPolicy.ABORT -> 
                    setRejectedExecutionHandler(ThreadPoolExecutor.AbortPolicy())
            }
            
            afterPropertiesSet()
            
            println("🧵 Created thread pool for handler: ${port.handlerName} (core=${config.corePoolSize}, max=${config.maxPoolSize}, queue=${config.queueCapacity})")
        }
    }

    /**
     * 핸들러 메트릭 조회
     */
    fun getMetrics(): EventHandlerMetrics {
        val poolExecutor = executor.threadPoolExecutor
        
        return EventHandlerMetrics(
            handlerName = methodName,
            processedCount = processedCount.get(),
            errorCount = errorCount.get(),
            retryCount = retryCount.get(),
            skippedCount = skippedCount.get(),
            activeThreads = executor.activeCount,
            poolSize = executor.poolSize,
            corePoolSize = executor.corePoolSize,
            maxPoolSize = executor.maxPoolSize,
            queueSize = poolExecutor.queue.size,
            completedTaskCount = poolExecutor.completedTaskCount
        )
    }

    /**
     * 실행 순서 정렬을 위한 비교 (order 기준)
     */
    override fun compareTo(other: ThreadPoolEventHandlerMethod): Int {
        return when (val orderComparison = this.order.compareTo(other.order)) {
            0 -> this.methodName.compareTo(other.methodName)
            else -> orderComparison
        }
    }

    /**
     * 스레드 풀 종료
     */
    fun shutdown() {
        executor.shutdown()
        println("🛑 Shutdown thread pool for handler: ${port.handlerName}")
    }

    /**
     * 핸들러 정보 요약
     */
    override fun toString(): String {
        return "ThreadPoolEventHandlerMethod(name='$methodName', eventType=${eventType.simpleName}, order=$order)"
    }
}

/**
 * 이벤트 핸들러 메트릭
 */
data class EventHandlerMetrics(
    val handlerName: String,
    val processedCount: Long,
    val errorCount: Long,
    val retryCount: Long,
    val skippedCount: Long,
    val activeThreads: Int,
    val poolSize: Int,
    val corePoolSize: Int,
    val maxPoolSize: Int,
    val queueSize: Int,
    val completedTaskCount: Long
) {
    val successRate: Double = if (processedCount + errorCount > 0) {
        processedCount.toDouble() / (processedCount + errorCount) * 100
    } else 0.0
    
    val avgRetryRate: Double = if (processedCount > 0) {
        retryCount.toDouble() / processedCount
    } else 0.0
    
    fun summary(): String {
        return "EventHandlerMetrics(handler=$handlerName, processed=$processedCount, errors=$errorCount, " +
                "retries=$retryCount, skipped=$skippedCount, successRate=${String.format("%.1f", successRate)}%, " +
                "activeThreads=$activeThreads/$maxPoolSize, queueSize=$queueSize)"
    }
}
