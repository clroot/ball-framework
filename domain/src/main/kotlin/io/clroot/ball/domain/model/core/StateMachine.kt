package io.clroot.ball.domain.model.core

import arrow.core.Either
import arrow.core.left
import arrow.core.right

/**
 * 상태 기계 (State Machine)
 *
 * 상태 전이 규칙을 명시적으로 정의하는 상태 기계 인터페이스
 * 엔티티의 상태 변화를 관리하고 유효한 상태 전이만 허용
 *
 * @param S 상태 타입
 * @param E 이벤트 타입
 */
interface StateMachine<S, E> {
    /**
     * 현재 상태 조회
     *
     * @return 현재 상태
     */
    fun getCurrentState(): S

    /**
     * 이벤트 발생 가능 여부 확인
     *
     * @param event 발생시킬 이벤트
     * @return 이벤트 발생 가능 여부
     */
    fun canFire(event: E): Boolean

    /**
     * 이벤트 발생
     *
     * @param event 발생시킬 이벤트
     * @return 성공 시 새로운 상태, 실패 시 오류
     */
    fun fire(event: E): Either<StateTransitionError, S>
}

/**
 * 상태 전이 오류
 */
sealed class StateTransitionError {
    /**
     * 유효하지 않은 상태 전이
     *
     * @param currentState 현재 상태
     * @param event 발생시킨 이벤트
     */
    data class InvalidTransition(
        val currentState: Any,
        val event: Any
    ) : StateTransitionError() {
        override fun toString(): String =
            "Cannot transition from $currentState with event ${event::class.simpleName}"
    }
}

/**
 * 상태 전이 정의
 *
 * @param S 상태 타입
 * @param E 이벤트 타입
 * @param fromState 시작 상태
 * @param event 이벤트
 * @param toState 목표 상태
 */
data class StateTransition<S, E>(
    val fromState: S,
    val event: E,
    val toState: S
)

/**
 * 기본 상태 기계 구현
 *
 * @param S 상태 타입
 * @param E 이벤트 타입
 * @param initialState 초기 상태
 * @param transitions 허용된 상태 전이 목록
 */
open class DefaultStateMachine<S, E>(
    initialState: S,
    private val transitions: List<StateTransition<S, E>>
) : StateMachine<S, E> {
    private var currentState: S = initialState

    override fun getCurrentState(): S = currentState

    override fun canFire(event: E): Boolean =
        transitions.any { it.fromState == currentState && it.event == event }

    override fun fire(event: E): Either<StateTransitionError, S> {
        if (!canFire(event)) {
            return StateTransitionError.InvalidTransition(
                currentState as Any,
                event as Any
            ).left()
        }

        val transition = transitions.first { it.fromState == currentState && it.event == event }
        currentState = transition.toState

        return currentState.right()
    }
}
