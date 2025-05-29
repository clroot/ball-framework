package io.clroot.ball.adapter.outbound.event.publisher.core

import io.clroot.ball.application.port.outbound.DomainEventPublisher
import io.clroot.ball.domain.event.DomainEvent
import org.slf4j.LoggerFactory

/**
 * 이벤트 발행자 기본 클래스
 * 
 * 모든 이벤트 발행자 구현체의 공통 기능을 제공합니다.
 * 템플릿 메서드 패턴을 사용하여 구체적인 발행 로직은 하위 클래스에서 구현하도록 합니다.
 * 
 * 주요 기능:
 * - 공통 로깅 및 에러 처리
 * - 이벤트 발행 전후 처리
 * - 메트릭 수집 (확장 가능)
 */
abstract class EventPublisherBase : DomainEventPublisher {
    
    protected val log = LoggerFactory.getLogger(javaClass)

    /**
     * 이벤트 발행 (템플릿 메서드)
     * 
     * 공통 전처리/후처리를 수행하고, 실제 발행은 doPublish()에서 처리합니다.
     */
    final override fun publish(event: DomainEvent) {
        log.debug("Publishing event: {} (ID: {})", event.type, event.id)
        
        try {
            // 발행 전 처리
            beforePublish(event)
            
            // 실제 발행 (하위 클래스 구현)
            doPublish(event)
            
            // 발행 후 처리
            afterPublish(event)
            
            log.debug("Event published successfully: {} (ID: {})", event.type, event.id)
            
        } catch (e: Exception) {
            log.error("Failed to publish event: {} (ID: {})", event.type, event.id, e)
            handlePublishError(event, e)
            throw e // 에러를 다시 던져서 상위에서 처리할 수 있도록
        }
    }

    /**
     * 실제 이벤트 발행 구현
     * 
     * 하위 클래스에서 구체적인 메시징 시스템에 맞게 구현해야 합니다.
     * 
     * @param event 발행할 도메인 이벤트
     */
    protected abstract fun doPublish(event: DomainEvent)

    /**
     * 이벤트 발행 전 처리
     * 
     * 하위 클래스에서 필요시 오버라이드할 수 있습니다.
     * 예: 유효성 검증, 메트릭 수집 시작 등
     * 
     * @param event 발행할 도메인 이벤트
     */
    protected open fun beforePublish(event: DomainEvent) {
        // 기본 구현에서는 아무것도 하지 않음
    }

    /**
     * 이벤트 발행 후 처리
     * 
     * 하위 클래스에서 필요시 오버라이드할 수 있습니다.
     * 예: 메트릭 수집 완료, 후속 처리 등
     * 
     * @param event 발행된 도메인 이벤트
     */
    protected open fun afterPublish(event: DomainEvent) {
        // 기본 구현에서는 아무것도 하지 않음
    }

    /**
     * 이벤트 발행 오류 처리
     * 
     * 기본적으로는 로깅만 수행하지만, 하위 클래스에서 재정의 가능합니다.
     * 예: 재시도 로직, 데드 레터 큐 발송, 알림 등
     * 
     * @param event 발행에 실패한 이벤트
     * @param error 발생한 예외
     */
    protected open fun handlePublishError(event: DomainEvent, error: Exception) {
        // 기본 구현에서는 로깅만 수행
        // 하위 클래스에서 재시도 로직, 메트릭 수집, 알림 등을 구현할 수 있음
        log.error("Event publish error handled: {} (ID: {})", event.type, event.id, error)
    }

    /**
     * 이벤트 타입에 기반한 목적지 결정
     * 
     * 기본 구현은 이벤트 클래스 이름을 kebab-case로 변환합니다.
     * 하위 클래스에서 다른 네이밍 전략을 사용할 수 있습니다.
     * 
     * @param event 도메인 이벤트
     * @return 목적지 이름 (토픽, 큐 등)
     */
    protected open fun determineDestination(event: DomainEvent): String {
        return event.javaClass.simpleName
            .replace("Event", "")
            .replace(Regex("([a-z])([A-Z])"), "$1-$2")
            .lowercase()
    }

    /**
     * 이벤트에서 키 추출
     * 
     * 파티셔닝이나 라우팅 키로 사용할 수 있습니다.
     * 기본 구현은 이벤트 타입을 반환합니다.
     * 
     * @param event 도메인 이벤트
     * @return 이벤트 키
     */
    protected open fun extractEventKey(event: DomainEvent): String {
        return event.type
    }
}
