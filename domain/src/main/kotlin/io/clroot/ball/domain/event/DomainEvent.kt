package io.clroot.ball.domain.event

import io.clroot.ball.domain.model.vo.BinaryId
import java.time.Instant

/**
 * 도메인 이벤트 (Domain Event)
 *
 * 도메인 모델의 상태 변경을 나타내는 이벤트입니다.
 * 
 * 특징:
 * - 프로세스 내에서 즉시 처리
 * - 비즈니스 로직의 결과로 발생
 * - 트랜잭션 컨텍스트 공유
 * - 도메인 이벤트는 과거에 발생한 사실을 나타내므로 불변이어야 함
 * 
 * vs IntegrationEvent:
 * - DomainEvent: 같은 서비스 내 컴포넌트 간 통신
 * - IntegrationEvent: 서로 다른 서비스 간 통신
 */
interface DomainEvent : Event {
    /**
     * 이벤트 타입
     * 기본적으로 클래스 이름을 사용
     */
    override val type: String get() = this::class.simpleName ?: "UnknownDomainEvent"
}

/**
 * 도메인 이벤트 기본 구현
 */
abstract class DomainEventBase : DomainEvent {
    override val id: String = BinaryId.new().toString()
    override val occurredAt: Instant = Instant.now()
}
