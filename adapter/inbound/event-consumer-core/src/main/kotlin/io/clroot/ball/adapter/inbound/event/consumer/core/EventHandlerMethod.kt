package io.clroot.ball.adapter.inbound.event.consumer.core

import io.clroot.ball.domain.event.DomainEvent
import java.lang.reflect.Method
import kotlin.coroutines.Continuation

/**
 * 이벤트 핸들러 메서드 정보를 담는 데이터 클래스
 */
data class EventHandlerMethod(
    val bean: Any,
    val method: Method,
    val eventType: Class<out DomainEvent>,
    val methodName: String,
    val async: Boolean = true,
    val order: Int = 0
) : Comparable<EventHandlerMethod> {

    /**
     * 핸들러 메서드 실행
     */
    suspend fun invoke(event: DomainEvent) {
        try {
            method.isAccessible = true
            
            // 메서드 이름과 매개변수에 따라 안전하게 호출
            when {
                method.name == "handleEvent" && method.parameterCount == 1 -> {
                    // 테스트용 non-suspend 메서드는 직접 호출
                    method.invoke(bean, event)
                }
                method.name == "consume" && method.parameterCount == 1 -> {
                    // 일반 consume 함수
                    method.invoke(bean, event)
                }
                method.name == "consume" && method.parameterCount == 2 -> {
                    // suspend consume 함수
                    invokeSuspendFunction(event)
                }
                else -> {
                    // 기타 메서드는 매개변수 개수로 판단
                    if (method.parameterCount == 1) {
                        method.invoke(bean, event)
                    } else {
                        throw IllegalArgumentException("Unsupported method signature: ${method.name} with ${method.parameterCount} parameters")
                    }
                }
            }
            
        } catch (e: Exception) { 
            // 오류 발생 시에만 상세 로그 출력
            println("❌ Method execution failed:")
            println("   Method: ${method.name}(${method.parameterTypes.joinToString { it.simpleName }})")
            println("   Bean: ${bean.javaClass.simpleName}")
            println("   Event: ${event.javaClass.simpleName}")
            println("   Error: ${e.javaClass.simpleName}: ${e.message}")
            if (e.cause != null) {
                println("   Caused by: ${e.cause?.javaClass?.simpleName}: ${e.cause?.message}")
            }
            
            throw EventHandlerExecutionException("Failed to execute event handler: $methodName (${method.name})", e)
        }
    }

    /**
     * Suspend 함수 호출 처리
     */
    private suspend fun invokeSuspendFunction(event: DomainEvent) {
        // Kotlin suspend 함수를 호출하기 위한 Continuation 매개변수 처리
        val parameterTypes = method.parameterTypes
        val parameterCount = parameterTypes.size

        when {
            // suspend 함수는 마지막 매개변수가 Continuation
            parameterCount >= 2 && parameterTypes.last() == Continuation::class.java -> {
                // 코루틴 호출이므로 runBlocking 사용하지 않고 직접 suspend 호출
                val result = kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn<Any?> { continuation ->
                    method.invoke(bean, event, continuation)
                }
                // 결과 처리 (필요시)
                @Suppress("UNUSED_VARIABLE")
                val ignored = result
            }

            else -> {
                // 일반 함수로 처리
                method.invoke(bean, event)
            }
        }
    }

    /**
     * 실행 순서 정렬을 위한 비교 (order 기준)
     */
    override fun compareTo(other: EventHandlerMethod): Int {
        return when (val orderComparison = this.order.compareTo(other.order)) {
            0 -> this.methodName.compareTo(other.methodName) // order가 같으면 이름으로 정렬
            else -> orderComparison
        }
    }

    /**
     * 핸들러 정보 요약
     */
    override fun toString(): String {
        return "EventHandlerMethod(name='$methodName', eventType=${eventType.simpleName}, async=$async, order=$order)"
    }
}
