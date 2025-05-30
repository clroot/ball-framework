package io.clroot.ball.adapter.inbound.event.consumer.domain

import io.clroot.ball.adapter.inbound.event.consumer.core.EventHandlerMethodFactory
import io.clroot.ball.adapter.inbound.event.consumer.domain.test.TestDomainEventHandler
import io.kotest.core.spec.style.FunSpec

/**
 * EventHandler 메서드 찾기 디버깅 테스트
 */
class DebugEventHandlerTest : FunSpec({

    test("should debug TestDomainEventHandler methods") {
        val handler = TestDomainEventHandler()
        
        println("=== TestDomainEventHandler 메서드 분석 ===")
        println("클래스: ${handler.javaClass.name}")
        println("이벤트 타입: ${handler.eventType}")
        println("이벤트 타입 (Java): ${handler.eventType.java}")
        
        println("\n모든 메서드:")
        handler.javaClass.methods.forEach { method ->
            println("- ${method.name}(${method.parameterTypes.joinToString { it.simpleName }})")
        }
        
        println("\nconsume 메서드만:")
        handler.javaClass.methods.filter { it.name == "consume" }.forEach { method ->
            println("- ${method.name}(${method.parameterTypes.joinToString { "${it.simpleName}(${it.name})" }})")
            println("  파라미터 개수: ${method.parameterCount}")
            println("  선언된 클래스: ${method.declaringClass}")
        }
        
        try {
            val handlerMethod = EventHandlerMethodFactory.createForTest(
                handler, 
                handler.eventType.java as Class<out io.clroot.ball.domain.event.DomainEvent>
            )
            println("\n✅ EventHandlerMethod 생성 성공!")
            println("메서드: ${handlerMethod.method}")
        } catch (e: Exception) {
            println("\n❌ EventHandlerMethod 생성 실패: ${e.message}")
            e.printStackTrace()
        }
    }
})
