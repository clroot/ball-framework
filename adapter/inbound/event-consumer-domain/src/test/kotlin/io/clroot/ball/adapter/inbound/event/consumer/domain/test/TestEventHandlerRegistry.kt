package io.clroot.ball.adapter.inbound.event.consumer.domain.test

import io.clroot.ball.adapter.inbound.event.consumer.core.ThreadPoolEventHandlerMethod
import io.clroot.ball.adapter.inbound.event.consumer.core.EventHandlerRegistryInterface
import io.clroot.ball.domain.event.DomainEvent

/**
 * 테스트용 ThreadPool 기반 EventHandlerRegistry 구현체
 *
 * ApplicationContext 의존성 없이 동작하는 단순한 레지스트리
 * 코루틴 기반에서 ThreadPool 기반으로 완전히 변경되었습니다.
 */
class TestEventHandlerRegistry : EventHandlerRegistryInterface {

    // 이벤트 타입별 ThreadPool 핸들러 매핑
    private val handlerMap = mutableMapOf<Class<out DomainEvent>, MutableList<ThreadPoolEventHandlerMethod>>()

    /**
     * 특정 이벤트 타입에 대한 ThreadPool 핸들러들 반환
     */
    override fun getHandlers(eventType: Class<out DomainEvent>): List<ThreadPoolEventHandlerMethod> {
        return handlerMap[eventType]?.toList() ?: emptyList()
    }

    /**
     * ThreadPool 핸들러 등록
     */
    override fun registerHandler(eventType: Class<out DomainEvent>, handler: ThreadPoolEventHandlerMethod) {
        handlerMap.computeIfAbsent(eventType) { mutableListOf() }.add(handler)
        println("📝 [TEST] Registered handler: ${eventType.simpleName} -> ${handler.methodName}")
    }

    /**
     * ThreadPool 핸들러 제거
     */
    override fun unregisterHandler(eventType: Class<out DomainEvent>, handler: ThreadPoolEventHandlerMethod) {
        handlerMap[eventType]?.remove(handler)
        handler.shutdown()  // ThreadPool 종료
        println("🗑️ [TEST] Unregistered handler: ${eventType.simpleName} -> ${handler.methodName}")
    }

    /**
     * 모든 핸들러가 처리하는 이벤트 타입들 반환
     */
    override fun getAllHandledEventTypes(): Set<Class<out DomainEvent>> {
        return handlerMap.keys.toSet()
    }
    
    /**
     * 테스트용 편의 메서드들
     */
    fun getHandlerCount(eventType: Class<out DomainEvent>): Int {
        return handlerMap[eventType]?.size ?: 0
    }
    
    fun getTotalHandlerCount(): Int {
        return handlerMap.values.sumOf { it.size }
    }
    
    fun clear() {
        // 모든 핸들러의 ThreadPool 종료
        handlerMap.values.flatten().forEach { handler ->
            try {
                handler.shutdown()
            } catch (e: Exception) {
                println("⚠️ [TEST] Error shutting down handler ${handler.methodName}: ${e.message}")
            }
        }
        handlerMap.clear()
        println("🧹 [TEST] Cleared all handlers")
    }
    
    fun printSummary() {
        println("\n📋 [TEST] Handler Registry Summary:")
        println("=" * 50)
        
        if (handlerMap.isEmpty()) {
            println("No handlers registered.")
            return
        }
        
        handlerMap.forEach { (eventType, handlers) ->
            println("📌 ${eventType.simpleName}: ${handlers.size} handler(s)")
            handlers.sortedBy { it.order }.forEach { handler ->
                println("   - ${handler.methodName} (order=${handler.order})")
            }
        }
        
        println("=" * 50)
        println("📊 Total: ${getTotalHandlerCount()} handlers for ${handlerMap.size} event types")
    }
}

// 유틸리티 확장 함수
private operator fun String.times(n: Int): String = this.repeat(n)
