package io.clroot.ball.adapter.inbound.event.consumer.core

import io.clroot.ball.application.port.inbound.EventConsumerPort
import io.clroot.ball.domain.event.DomainEvent
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * ThreadPool 기반 이벤트 핸들러 레지스트리
 *
 * 코루틴 기반에서 ThreadPool 기반으로 완전히 변경되었습니다.
 * - 단순하고 직관적인 스레드 풀 관리
 * - JPA와 자연스러운 연동
 * - 예측 가능한 리소스 사용
 */
@Component
class EventHandlerRegistry : ApplicationContextAware, EventHandlerRegistryInterface {

    private lateinit var applicationContext: ApplicationContext

    // 이벤트 타입별 ThreadPool 핸들러 매핑
    private val handlerMap = ConcurrentHashMap<Class<out DomainEvent>, MutableList<ThreadPoolEventHandlerMethod>>()

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
    }

    @PostConstruct
    fun initialize() {
        scanAndRegisterHandlers()
    }
    
    @PreDestroy
    fun cleanup() {
        println("🛑 Shutting down all event handler thread pools...")
        handlerMap.values.flatten().forEach { handler ->
            try {
                handler.shutdown()
            } catch (e: Exception) {
                println("⚠️ Error shutting down handler ${handler.methodName}: ${e.message}")
            }
        }
    }

    /**
     * 특정 이벤트 타입에 대한 핸들러들 반환
     */
    override fun getHandlers(eventType: Class<out DomainEvent>): List<ThreadPoolEventHandlerMethod> {
        return handlerMap[eventType]?.toList() ?: emptyList()
    }

    /**
     * 모든 핸들러가 처리하는 이벤트 타입들 반환
     */
    override fun getAllHandledEventTypes(): Set<Class<out DomainEvent>> {
        return handlerMap.keys.toSet()
    }

    /**
     * 핸들러 등록
     */
    override fun registerHandler(eventType: Class<out DomainEvent>, handler: ThreadPoolEventHandlerMethod) {
        handlerMap.computeIfAbsent(eventType) { mutableListOf() }.add(handler)
        println("📝 Registered event handler: ${eventType.simpleName} -> ${handler.methodName}")
    }

    /**
     * 핸들러 제거
     */
    override fun unregisterHandler(eventType: Class<out DomainEvent>, handler: ThreadPoolEventHandlerMethod) {
        handlerMap[eventType]?.remove(handler)
        handler.shutdown()
        println("🗑️ Unregistered event handler: ${eventType.simpleName} -> ${handler.methodName}")
    }

    /**
     * 애플리케이션 컨텍스트에서 이벤트 핸들러들을 스캔하고 등록
     */
    private fun scanAndRegisterHandlers() {
        println("🔍 Scanning for ThreadPool-based event handlers...")

        val totalHandlers = scanPortBasedHandlers()

        println("✅ Event handler scanning completed. Registered $totalHandlers handlers for ${handlerMap.size} event types")
        
        // 등록된 핸들러 요약 출력
        handlerMap.forEach { (eventType, handlers) ->
            println("   📋 ${eventType.simpleName}: ${handlers.size} handler(s)")
            handlers.sortedBy { it.order }.forEach { handler ->
                println("      - ${handler.methodName} (order=${handler.order})")
            }
        }
    }

    /**
     * EventConsumerPort 구현체들 스캔 (ThreadPool 기반)
     */
    @Suppress("UNCHECKED_CAST")
    private fun scanPortBasedHandlers(): Int {
        val portHandlers = applicationContext.getBeansOfType(EventConsumerPort::class.java)
        var count = 0

        for ((beanName, handler) in portHandlers) {
            try {
                val eventType = handler.eventType.java as Class<out DomainEvent>

                // ThreadPool 기반 핸들러 메서드 생성
                val handlerMethod = EventHandlerMethodFactory.createFromPort(handler)

                registerHandler(eventType, handlerMethod)
                count++

                println("✅ Found EventConsumerPort: ${eventType.simpleName} -> ${handler.handlerName} " +
                        "(corePool=${handler.executorConfig.corePoolSize}, maxPool=${handler.executorConfig.maxPoolSize}, order=${handler.order})")

            } catch (e: RuntimeException) {
                println("❌ Failed to create ThreadPoolEventHandlerMethod for bean: $beanName - ${e.message}")
            } catch (e: Exception) {
                println("⚠️ Failed to register EventConsumerPort bean: $beanName - ${e.message}")
            }
        }

        println("📊 Registered $count EventConsumerPort implementations")
        return count
    }
    
    /**
     * 모든 핸들러의 메트릭 수집
     */
    fun getAllMetrics(): Map<String, EventHandlerMetrics> {
        val metrics = mutableMapOf<String, EventHandlerMetrics>()
        
        handlerMap.values.flatten().forEach { handler ->
            metrics[handler.methodName] = handler.getMetrics()
        }
        
        return metrics
    }
    
    /**
     * 메트릭 요약 출력
     */
    fun printMetricsSummary() {
        println("\n📊 Event Handler Metrics Summary:")
        println("=" * 80)
        
        val allMetrics = getAllMetrics()
        
        if (allMetrics.isEmpty()) {
            println("No handlers found.")
            return
        }
        
        allMetrics.values.forEach { metrics ->
            println(metrics.summary())
        }
        
        // 전체 통계
        val totalProcessed = allMetrics.values.sumOf { it.processedCount }
        val totalErrors = allMetrics.values.sumOf { it.errorCount }
        val totalRetries = allMetrics.values.sumOf { it.retryCount }
        val totalSkipped = allMetrics.values.sumOf { it.skippedCount }
        val totalActiveThreads = allMetrics.values.sumOf { it.activeThreads }
        
        println("-" * 80)
        println("📈 Overall Statistics:")
        println("   Total Processed: $totalProcessed")
        println("   Total Errors: $totalErrors")
        println("   Total Retries: $totalRetries") 
        println("   Total Skipped: $totalSkipped")
        println("   Active Threads: $totalActiveThreads")
        
        val overallSuccessRate = if (totalProcessed + totalErrors > 0) {
            totalProcessed.toDouble() / (totalProcessed + totalErrors) * 100
        } else 0.0
        println("   Overall Success Rate: ${String.format("%.1f", overallSuccessRate)}%")
        
        println("=" * 80)
    }
}

// 호환성을 위한 타입 별칭 (기존 코드와의 호환성)
typealias EventHandlerMethod = ThreadPoolEventHandlerMethod

// 유틸리티 확장 함수
private operator fun String.times(n: Int): String = this.repeat(n)
