package io.clroot.ball.domain.model.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StateMachineTest {

    // 테스트용 상태 정의
    enum class OrderStatus {
        CREATED, PAID, SHIPPED, DELIVERED, CANCELLED
    }

    // 테스트용 이벤트 정의
    sealed class OrderEvent {
        data object PaymentReceived : OrderEvent()
        data object Shipped : OrderEvent()
        data object Delivered : OrderEvent()
        data object Cancelled : OrderEvent()
    }

    @Test
    fun `should initialize with correct state`() {
        // given
        val stateMachine = createOrderStateMachine()

        // then
        assertEquals(OrderStatus.CREATED, stateMachine.getCurrentState())
    }

    @Test
    fun `should allow valid transitions`() {
        // given
        val stateMachine = createOrderStateMachine()

        // when & then - CREATED -> PAID
        assertTrue(stateMachine.canFire(OrderEvent.PaymentReceived))
        val result1 = stateMachine.fire(OrderEvent.PaymentReceived)
        assertTrue(result1.isRight())
        result1.map { state ->
            assertEquals(OrderStatus.PAID, state)
        }

        // when & then - PAID -> SHIPPED
        assertTrue(stateMachine.canFire(OrderEvent.Shipped))
        val result2 = stateMachine.fire(OrderEvent.Shipped)
        assertTrue(result2.isRight())
        result2.map { state ->
            assertEquals(OrderStatus.SHIPPED, state)
        }

        // when & then - SHIPPED -> DELIVERED
        assertTrue(stateMachine.canFire(OrderEvent.Delivered))
        val result3 = stateMachine.fire(OrderEvent.Delivered)
        assertTrue(result3.isRight())
        result3.map { state ->
            assertEquals(OrderStatus.DELIVERED, state)
        }
    }

    @Test
    fun `should reject invalid transitions`() {
        // given
        val stateMachine = createOrderStateMachine()

        // when & then - Cannot go from CREATED to SHIPPED
        assertFalse(stateMachine.canFire(OrderEvent.Shipped))
        val result = stateMachine.fire(OrderEvent.Shipped)
        assertTrue(result.isLeft())
        result.mapLeft { error ->
            assertTrue(error is StateTransitionError.InvalidTransition)
            val invalidTransition = error as StateTransitionError.InvalidTransition
            assertEquals(OrderStatus.CREATED, invalidTransition.currentState)
        }
    }

    @Test
    fun `should allow cancellation from valid states`() {
        // given
        val stateMachine = createOrderStateMachine()

        // when & then - Can cancel from CREATED
        assertTrue(stateMachine.canFire(OrderEvent.Cancelled))
        val result1 = stateMachine.fire(OrderEvent.Cancelled)
        assertTrue(result1.isRight())
        result1.map { state ->
            assertEquals(OrderStatus.CANCELLED, state)
        }

        // given - New state machine, move to PAID
        val stateMachine2 = createOrderStateMachine()
        stateMachine2.fire(OrderEvent.PaymentReceived)

        // when & then - Can cancel from PAID
        assertTrue(stateMachine2.canFire(OrderEvent.Cancelled))
        val result2 = stateMachine2.fire(OrderEvent.Cancelled)
        assertTrue(result2.isRight())
        result2.map { state ->
            assertEquals(OrderStatus.CANCELLED, state)
        }
    }

    private fun createOrderStateMachine(): StateMachine<OrderStatus, OrderEvent> {
        val transitions = listOf(
            // 결제 완료
            StateTransition<OrderStatus, OrderEvent>(OrderStatus.CREATED, OrderEvent.PaymentReceived, OrderStatus.PAID),
            // 배송 시작
            StateTransition<OrderStatus, OrderEvent>(OrderStatus.PAID, OrderEvent.Shipped, OrderStatus.SHIPPED),
            // 배송 완료
            StateTransition<OrderStatus, OrderEvent>(OrderStatus.SHIPPED, OrderEvent.Delivered, OrderStatus.DELIVERED),
            // 주문 취소 (생성 또는 결제 상태에서만 가능)
            StateTransition<OrderStatus, OrderEvent>(OrderStatus.CREATED, OrderEvent.Cancelled, OrderStatus.CANCELLED),
            StateTransition<OrderStatus, OrderEvent>(OrderStatus.PAID, OrderEvent.Cancelled, OrderStatus.CANCELLED)
        )

        return DefaultStateMachine<OrderStatus, OrderEvent>(OrderStatus.CREATED, transitions)
    }
}
