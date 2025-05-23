package io.clroot.ball.adapter.inbound.messaging.consumer.inmemory

import io.clroot.ball.adapter.inbound.messaging.consumer.inmemory.registry.BlockingDomainEventHandlerRegistry
import io.clroot.ball.adapter.inbound.messaging.consumer.inmemory.registry.DomainEventHandlerRegistry
import io.clroot.ball.application.event.BlockingDomainEventHandler
import io.clroot.ball.application.event.DomainEventHandler
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

/**
 * InMemory Event Consumer Auto Configuration
 *
 * 이 모듈을 의존성에 추가하면 자동으로 InMemory Event Consumer가 활성화됩니다.
 *
 * 활성화 조건:
 * 1. DomainEventHandler 클래스가 클래스패스에 존재
 * 2. ball.event.consumer.inmemory.enabled=true (기본값)
 * 3. 필요한 Bean 들이 없을 때 자동 생성
 */
@AutoConfiguration
@ConditionalOnClass(DomainEventHandler::class)
@ConditionalOnProperty(
    name = ["ball.event.consumer.inmemory.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@EnableConfigurationProperties(InMemoryEventConsumerProperties::class)
@ComponentScan(basePackages = ["io.clroot.ball.adapter.inbound.messaging.consumer.inmemory"])
@EnableAsync
class InMemoryEventConsumerAutoConfiguration {

    /**
     * 도메인 이벤트 핸들러 레지스트리 자동 설정
     *
     * Spring context에서 모든 DomainEventHandler 구현체를 찾아서 자동으로 등록합니다.
     */
    @Bean
    @ConditionalOnMissingBean
    fun domainEventHandlerRegistry(
        handlers: List<DomainEventHandler<*>>
    ): DomainEventHandlerRegistry {
        return DomainEventHandlerRegistry(handlers)
    }

    /**
     * Blocking 도메인 이벤트 핸들러 레지스트리 자동 설정
     *
     * Spring context에서 모든 BlockingDomainEventHandler 구현체를 찾아서 자동으로 등록합니다.
     */
    @Bean
    @ConditionalOnMissingBean
    fun blockingDomainEventHandlerRegistry(
        handlers: List<BlockingDomainEventHandler<*>>
    ): BlockingDomainEventHandlerRegistry {
        return BlockingDomainEventHandlerRegistry(handlers)
    }

    /**
     * InMemory Event Listener 자동 설정
     *
     * ApplicationEventPublisher로 발행된 DomainEventWrapper를 수신하여 처리합니다.
     */
    @Bean
    @ConditionalOnMissingBean
    fun inMemoryEventListener(
        handlerRegistry: DomainEventHandlerRegistry,
        blockingHandlerRegistry: BlockingDomainEventHandlerRegistry,
        properties: InMemoryEventConsumerProperties,
        eventTaskExecutor: Executor
    ): InMemoryEventListener {
        return InMemoryEventListener(handlerRegistry, blockingHandlerRegistry, properties, eventTaskExecutor)
    }

    /**
     * 이벤트 처리용 스레드 풀 자동 설정
     *
     * 비동기 이벤트 처리를 위한 전용 스레드 풀을 생성합니다.
     */
    @Bean("eventTaskExecutor")
    @ConditionalOnMissingBean(name = ["eventTaskExecutor"])
    fun eventTaskExecutor(properties: InMemoryEventConsumerProperties): Executor {
        val executor = ThreadPoolTaskExecutor()

        // 스레드 풀 크기 설정
        val corePoolSize = if (properties.parallel) {
            minOf(properties.maxConcurrency, 5)
        } else {
            1
        }

        executor.corePoolSize = corePoolSize
        executor.maxPoolSize = properties.maxConcurrency
        executor.queueCapacity = 100
        executor.keepAliveSeconds = 60

        // 스레드 이름 설정
        executor.setThreadNamePrefix("inmemory-event-")
        executor.setWaitForTasksToCompleteOnShutdown(true)
        executor.setAwaitTerminationSeconds(30)

        executor.initialize()
        return executor
    }


}
