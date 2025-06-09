package io.clroot.ball.domain.model

import io.clroot.ball.domain.event.DomainEvent
import java.time.Instant

/**
 * 집합체 루트 (Aggregate Root)
 *
 * 트랜잭션 일관성을 보장하는 경계를 설정하는 집합체 루트 클래스
 * 도메인 이벤트를 발행하는 메커니즘을 제공
 *
 * @param ID 엔티티 ID 타입
 */
abstract class AggregateRoot<ID : Any>(
    id: ID,
    createdAt: Instant,
    updatedAt: Instant,
    deletedAt: Instant?,
) : EntityBase<ID>(id, createdAt, updatedAt, deletedAt) {
    private val _domainEvents = mutableListOf<DomainEvent>()

    /**
     * 등록된 도메인 이벤트 목록
     * 불변 리스트로 반환하여 외부에서 수정할 수 없도록 함
     */
    val domainEvents: List<DomainEvent> get() = _domainEvents.toList()

    /**
     * 도메인 이벤트 등록
     *
     * @param event 등록할 도메인 이벤트
     */
    protected fun registerEvent(event: DomainEvent) {
        _domainEvents.add(event)
    }

    /**
     * 도메인 이벤트 목록 초기화
     * 이벤트가 처리된 후 호출되어야 함
     */
    fun clearEvents() {
        _domainEvents.clear()
    }
}
