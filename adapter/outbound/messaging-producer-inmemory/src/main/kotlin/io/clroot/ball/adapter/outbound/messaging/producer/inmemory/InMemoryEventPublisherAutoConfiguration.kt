package io.clroot.ball.adapter.outbound.messaging.producer.inmemory

import io.clroot.ball.application.port.outbound.DomainEventPublisher
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean

/**
 * InMemory Event Publisher Auto Configuration
 * 
 * 이 모듈을 의존성에 추가하면 자동으로 InMemory Event Publisher가 활성화됩니다.
 * 
 * 활성화 조건:
 * 1. DomainEventPublisher 클래스가 클래스패스에 존재
 * 2. ball.event.publisher.type=inmemory 또는 다른 publisher가 없을 때
 * 3. DomainEventPublisher Bean이 없을 때 자동 생성
 */
@AutoConfiguration
@ConditionalOnClass(DomainEventPublisher::class)
@ConditionalOnProperty(
    name = ["ball.event.publisher.type"],
    havingValue = "inmemory",
    matchIfMissing = true  // 다른 publisher가 없으면 기본으로 사용
)
@EnableConfigurationProperties(InMemoryEventPublisherProperties::class)
class InMemoryEventPublisherAutoConfiguration {

    /**
     * InMemory Event Publisher 자동 설정
     * 
     * ApplicationEventPublisher만 사용하여 단순하고 안전한 이벤트 발행을 제공합니다.
     * 순환 호출 문제를 방지하기 위해 DomainEventDispatcher에 의존하지 않습니다.
     */
    @Bean
    @ConditionalOnMissingBean(DomainEventPublisher::class)
    fun inMemoryEventPublisher(
        applicationEventPublisher: ApplicationEventPublisher,
        properties: InMemoryEventPublisherProperties
    ): DomainEventPublisher {
        return InMemoryEventPublisher(
            applicationEventPublisher = applicationEventPublisher,
            properties = properties
        )
    }
}
