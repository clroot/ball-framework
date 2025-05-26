package io.clroot.ball.domain.event

import io.clroot.ball.domain.model.vo.BinaryId
import java.time.Instant

/**
 * 도메인 이벤트 (Domain Event)
 *
 * 도메인 모델의 상태 변경을 나타내는 이벤트
 * 도메인 이벤트는 과거에 발생한 사실을 나타내므로 불변이어야 함
 */
interface DomainEvent {
    /**
     * 이벤트 ID
     */
    val id: String

    /**
     * 이벤트 발생 시간
     */
    val occurredAt: Instant

    /**
     * 이벤트 타입
     * 기본적으로 클래스 이름을 사용
     */
    val type: String get() = this::class.simpleName ?: "UnknownEvent"
}

/**
 * 도메인 이벤트 기본 구현
 */
abstract class DomainEventBase : DomainEvent {
    override val id: String = BinaryId.new().toString()
    override val occurredAt: Instant = Instant.now()
}
