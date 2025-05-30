package io.clroot.ball.adapter.inbound.event.consumer.domain

import io.clroot.ball.adapter.inbound.event.consumer.core.EventHandlerRegistry
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.ThreadPoolExecutor

/**
 * 도메인 이벤트 소비자 자동 설정
 *
 * 이 설정 클래스는 다음 조건을 만족할 때 활성화됩니다:
 * - ball.events.domain.consumer.enabled = true (기본값)
 * - DomainEventConsumer 빈이 아직 정의되지 않은 경우
 *
 * 자동으로 다음 빈들을 등록합니다:
 * - DomainEventConsumer: 도메인 이벤트 소비 구현체
 * - EventHandlerRegistry: 이벤트 핸들러 등록 및 관리
 * - domainEventTaskExecutor: 비동기 이벤트 처리용 TaskExecutor
 *
 * 사용 예시:
 * ```kotlin
 * @Component
 * class UserEventHandler {
 *
 *     @EventHandler
 *     fun handleUserCreated(event: UserCreatedEvent) {
 *         // 이벤트 처리 로직
 *     }
 * }
 * ```
 */
@AutoConfiguration
@EnableAsync
@EnableConfigurationProperties(DomainEventConsumerProperties::class)
@ConditionalOnProperty(
    prefix = "ball.events.domain.consumer",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class DomainEventConsumerAutoConfiguration {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 이벤트 핸들러 레지스트리 빈 등록
     *
     * 애플리케이션 컨텍스트에서 @EventHandler 어노테이션이 있는 메서드들을 찾아 등록합니다.
     */
    @Bean
    @ConditionalOnMissingBean(EventHandlerRegistry::class)
    fun eventHandlerRegistry(): EventHandlerRegistry {
        log.info("Configuring Event Handler Registry for domain events")
        return EventHandlerRegistry()
    }

    /**
     * Spring 도메인 이벤트 소비자 빈 등록
     */
    @Bean
    @ConditionalOnMissingBean(SpringDomainEventConsumer::class)
    fun springDomainEventConsumer(
        properties: DomainEventConsumerProperties,
        eventHandlerRegistry: EventHandlerRegistry
    ): SpringDomainEventConsumer {

        log.info(
            "Configuring Spring Domain Event Consumer with properties: async={}, retry={}, metrics={}",
            properties.async, properties.enableRetry, properties.enableMetrics
        )

        if (properties.enableDebugLogging) {
            log.debug("Spring Domain Event Consumer debug logging is enabled")
        }

        return SpringDomainEventConsumer(properties, eventHandlerRegistry)
    }
    
    /**
     * 호환성을 위한 DomainEventConsumer 별칭
     */
    @Bean
    @ConditionalOnMissingBean(name = ["domainEventConsumer"])
    fun domainEventConsumer(springDomainEventConsumer: SpringDomainEventConsumer): SpringDomainEventConsumer {
        return springDomainEventConsumer
    }

    /**
     * 도메인 이벤트 처리용 TaskExecutor 빈 등록
     *
     * 비동기 이벤트 처리를 위한 전용 스레드 풀을 제공합니다.
     */
    @Bean("domainEventTaskExecutor")
    @ConditionalOnMissingBean(name = ["domainEventTaskExecutor"])
    fun domainEventTaskExecutor(
        properties: DomainEventConsumerProperties
    ): ThreadPoolTaskExecutor {

        // 도메인 이벤트 처리용 기본 스레드 풀 설정
        val coreSize = Runtime.getRuntime().availableProcessors()
        val maxSize = coreSize * 2
        val queueCapacity = 100

        log.info(
            "Configuring Domain Event TaskExecutor: core={}, max={}, queue={}",
            coreSize, maxSize, queueCapacity
        )

        return ThreadPoolTaskExecutor().apply {
            setCorePoolSize(coreSize)
            setMaxPoolSize(maxSize)
            setKeepAliveSeconds(60)
            setQueueCapacity(queueCapacity)
            setThreadNamePrefix("domain-event-")

            // 큐가 가득 찬 경우 호출자 스레드에서 실행
            setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())

            // 우아한 종료를 위한 설정
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(60)

            initialize()
        }
    }

    /**
     * 설정 검증 및 경고
     */
    @Bean
    fun domainEventConsumerConfigurationValidator(
        properties: DomainEventConsumerProperties
    ): DomainEventConsumerConfigurationValidator {
        return DomainEventConsumerConfigurationValidator(properties)
    }

    /**
     * Domain Event Consumer 설정 검증기
     *
     * 잘못된 설정이 있을 경우 경고 로그를 출력합니다.
     */
    class DomainEventConsumerConfigurationValidator(
        private val properties: DomainEventConsumerProperties
    ) {

        private val log = LoggerFactory.getLogger(javaClass)

        init {
            validateConfiguration()
        }

        private fun validateConfiguration() {
            // 재시도 설정 검증
            if (properties.enableRetry && properties.maxRetryAttempts <= 0) {
                log.warn(
                    "Domain event consumer retry is enabled but maxRetryAttempts is {}, " +
                            "should be greater than 0", properties.maxRetryAttempts
                )
            }

            // 타임아웃 설정 검증
            if (properties.timeoutMs < 0) {
                log.warn(
                    "Domain event consumer timeout is negative: {}ms, this may cause issues",
                    properties.timeoutMs
                )
            }

            // 재시도 지연 설정 검증
            if (properties.enableRetry && properties.retryDelayMs <= 0) {
                log.warn(
                    "Domain event consumer retry is enabled but retryDelayMs is {}ms, " +
                            "should be greater than 0", properties.retryDelayMs
                )
            }

            // 운영 환경에서 디버그 로깅 경고
            if (properties.enableDebugLogging) {
                log.warn(
                    "Domain event consumer debug logging is enabled, " +
                            "this may impact performance in production"
                )
            }

            // 트랜잭션 설정 검증
            if (!properties.processInTransaction && !properties.processAfterCommit) {
                log.warn(
                    "Domain event consumer is configured to process neither in transaction nor after commit, " +
                            "events may not be processed at all"
                )
            }

            if (properties.processInTransaction && properties.processAfterCommit) {
                log.warn(
                    "Domain event consumer is configured to process both in transaction and after commit, " +
                            "events will be processed twice"
                )
            }

            log.info("Domain Event Consumer configuration validated successfully")
        }
    }
}
