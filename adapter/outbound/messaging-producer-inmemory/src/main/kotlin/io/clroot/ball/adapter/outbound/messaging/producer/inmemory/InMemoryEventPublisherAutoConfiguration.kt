package io.clroot.ball.adapter.outbound.messaging.producer.inmemory

import io.clroot.ball.application.event.DomainEventDispatcher
import io.clroot.ball.application.port.outbound.DomainEventPublisher
import io.clroot.ball.domain.event.DomainEvent
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableAsync

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
@EnableAsync
class InMemoryEventPublisherAutoConfiguration {

    /**
     * InMemory Event Publisher 자동 설정
     * 
     * ApplicationEventPublisher를 사용하여 단일 프로세스 내에서 이벤트를 발행합니다.
     * DomainEventDispatcher가 있으면 함께 사용하고, 없으면 ApplicationEventPublisher만 사용합니다.
     */
    @Bean
    @ConditionalOnMissingBean(DomainEventPublisher::class)
    fun inMemoryEventPublisher(
        applicationEventPublisher: ApplicationEventPublisher,
        properties: InMemoryEventPublisherProperties,
        domainEventDispatcher: DomainEventDispatcher? = null  // Optional 의존성
    ): DomainEventPublisher {
        return InMemoryEventPublisher(
            applicationEventPublisher = applicationEventPublisher,
            domainEventDispatcher = domainEventDispatcher ?: createNoOpDispatcher(),
            properties = properties
        )
    }

    /**
     * DomainEventDispatcher가 없을 때 사용하는 No-Op 구현체
     */
    private fun createNoOpDispatcher(): DomainEventDispatcher {
        return object : DomainEventDispatcher {
            override fun <T : DomainEvent> registerHandler(
                handler: io.clroot.ball.application.event.DomainEventHandler<T>,
                eventType: Class<T>
            ) {
                // No-Op: Consumer 모듈이 없으면 핸들러 등록하지 않음
            }

            override suspend fun dispatch(event: DomainEvent) {
                // No-Op: Consumer 모듈이 없으면 디스패치하지 않음
                // ApplicationEventPublisher로만 이벤트 발행
            }
        }
    }
}
