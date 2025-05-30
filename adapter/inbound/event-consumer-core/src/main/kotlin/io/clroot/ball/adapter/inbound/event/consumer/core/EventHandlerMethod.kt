package io.clroot.ball.adapter.inbound.event.consumer.core

import io.clroot.ball.domain.event.DomainEvent
import kotlinx.coroutines.runBlocking
import java.lang.reflect.Method
import kotlin.coroutines.Continuation
import kotlin.reflect.jvm.kotlinFunction

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
            
            // Kotlin suspend 함수인지 확인
            val kotlinFunction = method.kotlinFunction
            val isSuspendFunction = kotlinFunction?.isSuspend == true
            
            when {
                isSuspendFunction -> {
                    // suspend 함수는 코루틴 컨텍스트에서 호출
                    invokeSuspendFunction(event)
                }
                else -> {
                    // 일반 함수는 직접 호출
                    method.invoke(bean, event)
                }
            }
        } catch (e: Exception) {
            throw EventHandlerExecutionException("Failed to execute event handler: $methodName", e)
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
