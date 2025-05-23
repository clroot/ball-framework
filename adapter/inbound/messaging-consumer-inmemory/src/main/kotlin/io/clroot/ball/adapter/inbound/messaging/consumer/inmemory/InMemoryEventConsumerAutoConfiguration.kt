package io.clroot.ball.adapter.inbound.messaging.consumer.inmemory

import io.clroot.ball.adapter.inbound.messaging.consumer.core.executor.DomainEventHandlerExecutor
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
 * 
 * Core 모듈의 공통 컴포넌트들을 재사용하여 InMemory 전용 구현을 제공합니다.
 */
@AutoConfiguration
@ConditionalOnClass(DomainEventHandler::class)
@ConditionalOnProperty(
    name = ["ball.event.consumer.inmemory.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@EnableConfigurationProperties(InMemoryEventConsumerProperties::class)
@ComponentScan(basePackages = [
    "io.clroot.ball.adapter.inbound.messaging.consumer.core",  // Core 컴포넌트들
    "io.clroot.ball.adapter.inbound.messaging.consumer.inmemory"  // InMemory 전용 컴포넌트들
])
@EnableAsync
class InMemoryEventConsumerAutoConfiguration {

    /**
     * InMemory Event Listener 자동 설정
     *
     * Core 모듈의 DomainEventHandlerExecutor를 사용하여 이벤트를 처리합니다.
     */
    @Bean
    @ConditionalOnMissingBean
    fun inMemoryEventListener(
        handlerExecutor: DomainEventHandlerExecutor,
        properties: InMemoryEventConsumerProperties
    ): InMemoryEventListener {
        return InMemoryEventListener(handlerExecutor, properties)
    }

    /**
     * 이벤트 처리용 스레드 풀 자동 설정
     *
     * Spring ApplicationEvent를 비동기로 처리하기 위한 전용 스레드 풀을 생성합니다.
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

    /**
     * Blocking 작업용 스레드 풀 자동 설정
     *
     * JPA, JDBC 등 blocking I/O 작업을 위한 전용 스레드 풀을 생성합니다.
     * Core 모듈의 DomainEventHandlerExecutor에서 사용됩니다.
     */
    @Bean("blockingTaskExecutor")
    @ConditionalOnMissingBean(name = ["blockingTaskExecutor"])
    fun blockingTaskExecutor(properties: InMemoryEventConsumerProperties): Executor {
        val executor = ThreadPoolTaskExecutor()

        // blocking 작업을 위한 더 많은 스레드 할당
        executor.corePoolSize = minOf(properties.maxConcurrency * 2, 20)
        executor.maxPoolSize = properties.maxConcurrency * 3
        executor.queueCapacity = 200
        executor.keepAliveSeconds = 120

        // 스레드 이름 설정
        executor.setThreadNamePrefix("inmemory-blocking-")
        executor.setWaitForTasksToCompleteOnShutdown(true)
        executor.setAwaitTerminationSeconds(60)

        executor.initialize()
        return executor
    }
}
