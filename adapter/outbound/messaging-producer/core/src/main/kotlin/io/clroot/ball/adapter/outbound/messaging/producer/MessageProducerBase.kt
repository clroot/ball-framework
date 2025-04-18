package io.clroot.ball.adapter.outbound.messaging.producer

import io.clroot.ball.domain.event.DomainEvent
import io.clroot.ball.domain.event.DomainEventPublisher
import org.slf4j.LoggerFactory

abstract class MessageProducerBase : DomainEventPublisher {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun publish(event: DomainEvent) {
        try {
            log.debug("Publishing event: {}", event.javaClass.simpleName)
            doPublish(event)
            log.debug("Event published successfully: {}", event.javaClass.simpleName)
        } catch (e: Exception) {
            log.error("Failed to publish event: {}", event.javaClass.simpleName, e)
            handlePublishError(event, e)
        }
    }

    /**
     * 실제 이벤트 발행 구현
     * 하위 클래스에서 구체적인 메시징 시스템에 맞게 구현
     *
     * @param event 발행할 도메인 이벤트
     */
    protected abstract fun doPublish(event: DomainEvent)

    /**
     * 이벤트 발행 오류 처리
     * 기본적으로는 로깅만 수행하지만, 하위 클래스에서 재정의 가능
     *
     * @param event 발행에 실패한 이벤트
     * @param error 발생한 예외
     */
    protected open fun handlePublishError(event: DomainEvent, error: Exception) {
        // 기본 구현에서는 로깅만 수행
        // 하위 클래스에서 재시도 로직, 데드 레터 큐 등을 구현할 수 있음
    }

    /**
     * 이벤트 타입에 기반한 토픽/큐 이름 결정
     *
     * @param event 도메인 이벤트
     * @return 토픽/큐 이름
     */
    protected open fun determineDestination(event: DomainEvent): String {
        // 기본 구현: 이벤트 클래스 이름에서 "Event" 제거하고 kebab-case로 변환 + "-events" 접미사 추가
        return event.javaClass.simpleName
            .replace("Event", "")
            .replace(Regex("([a-z])([A-Z])"), "$1-$2")
            .lowercase() + "-events"
    }
}