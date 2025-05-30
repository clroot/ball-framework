package io.clroot.ball.application.orchestration

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.clroot.ball.application.port.outbound.EventProducerPort
import io.clroot.ball.domain.event.DomainEvent
import io.clroot.ball.domain.model.core.StateMachine
import io.clroot.ball.domain.model.core.StateTransitionError

/**
 * 프로세스 매니저 (Process Manager)
 *
 * 여러 집합체에 걸친 복잡한 워크플로를 조정하는 프로세스 매니저
 * 도메인 이벤트를 구독하고 적절한 명령을 실행하여 프로세스를 진행
 *
 * @param T 프로세스 상태 타입
 * @param E 오류 타입
 */
abstract class ProcessManager<T, E> {
    /**
     * 현재 프로세스 상태
     */
    protected abstract var state: T
    
    /**
     * 도메인 이벤트 발행자
     */
    protected abstract val eventPublisher: EventProducerPort
    
    /**
     * 프로세스 시작
     *
     * @param initialState 초기 상태
     * @return 성공 시 Right(Unit), 실패 시 Left(오류)
     */
    abstract suspend fun start(initialState: T): Either<E, Unit>
    
    /**
     * 도메인 이벤트 처리
     *
     * @param event 처리할 도메인 이벤트
     * @return 성공 시 Right(Unit), 실패 시 Left(오류)
     */
    abstract suspend fun handle(event: DomainEvent): Either<E, Unit>
    
    /**
     * 프로세스 완료 여부 확인
     *
     * @return 완료되었으면 true, 아니면 false
     */
    abstract fun isCompleted(): Boolean
    
    /**
     * 프로세스 상태 변경
     *
     * @param newState 새로운 상태
     */
    protected fun updateState(newState: T) {
        state = newState
    }
    
    /**
     * 도메인 이벤트 발행
     *
     * @param event 발행할 도메인 이벤트
     */
    protected suspend fun publishEvent(event: DomainEvent) {
        eventPublisher.produce(event)
    }
}

/**
 * 상태 기반 프로세스 매니저
 *
 * 상태 기계를 사용하여 프로세스 상태를 관리하는 프로세스 매니저
 *
 * @param S 상태 타입
 * @param E 이벤트 타입
 * @param X 오류 타입
 * @param stateMachine 상태 기계
 * @param eventPublisher 도메인 이벤트 발행자
 */
abstract class StateMachineProcessManager<S, E, X>(
    private val stateMachine: StateMachine<S, E>,
    override val eventPublisher: EventProducerPort
) : ProcessManager<S, X>() {
    override var state: S = stateMachine.getCurrentState()
    
    /**
     * 상태 전이 이벤트 처리
     *
     * @param event 상태 전이 이벤트
     * @return 성공 시 Right(Unit), 실패 시 Left(오류)
     */
    protected suspend fun transition(event: E): Either<X, Unit> {
        return when (val result = stateMachine.fire(event)) {
            is Either.Right -> {
                updateState(result.value)
                Unit.right()
            }
            is Either.Left -> handleTransitionError(result.value).left()
        }
    }
    
    /**
     * 상태 전이 오류 처리
     *
     * @param error 상태 전이 오류
     * @return 변환된 오류
     */
    protected abstract fun handleTransitionError(error: StateTransitionError): X
    
    override fun isCompleted(): Boolean {
        return isFinalState(state)
    }
    
    /**
     * 최종 상태 여부 확인
     *
     * @param state 확인할 상태
     * @return 최종 상태이면 true, 아니면 false
     */
    protected abstract fun isFinalState(state: S): Boolean
}

/**
 * 프로세스 매니저 팩토리
 *
 * 프로세스 매니저 인스턴스를 생성하는 팩토리
 *
 * @param T 프로세스 매니저 타입
 * @param ID 프로세스 ID 타입
 */
interface ProcessManagerFactory<T : ProcessManager<*, *>, ID> {
    /**
     * 프로세스 매니저 생성
     *
     * @param id 프로세스 ID
     * @return 생성된 프로세스 매니저
     */
    fun create(id: ID): T
    
    /**
     * 프로세스 매니저 조회
     *
     * @param id 프로세스 ID
     * @return 조회된 프로세스 매니저 또는 null
     */
    fun find(id: ID): T?
}