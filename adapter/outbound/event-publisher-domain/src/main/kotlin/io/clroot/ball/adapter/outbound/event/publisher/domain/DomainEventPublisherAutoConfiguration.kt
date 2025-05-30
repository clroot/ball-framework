package io.clroot.ball.adapter.outbound.event.publisher.domain

import io.clroot.ball.application.port.outbound.EventProducerPort
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

/**
 * Spring 도메인 이벤트 발행자 자동 설정
 * 
 * 이 설정 클래스는 다음 조건을 만족할 때 활성화됩니다:
 * - ball.events.domain.enabled = true (기본값)
 * - EventProducerPort 빈이 아직 정의되지 않은 경우
 * 
 * 자동으로 다음 빈들을 등록합니다:
 * - SpringDomainEventProducer: Spring ApplicationEvent 기반 도메인 이벤트 발행 구현체
 * 
 * 사용 예시:
 * ```kotlin
 * @Service
 * class UserService(
 *     private val eventProducer: EventProducerPort
 * ) {
 *     fun createUser(request: CreateUserRequest) {  // ThreadPool 기반
 *         // ... 비즈니스 로직
 *         eventProducer.produce(UserCreatedEvent(user.id, user.email))
 *     }
 * }
 * ```
 */
@AutoConfiguration
@EnableConfigurationProperties(DomainEventPublisherProperties::class)
@ConditionalOnProperty(
    prefix = "ball.events.domain",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class DomainEventPublisherAutoConfiguration {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Spring 도메인 이벤트 발행자 빈 등록
     * 
     * @Primary 어노테이션을 통해 기본 EventProducerPort로 설정됩니다.
     * 다른 EventProducerPort 구현체가 있을 경우 이 빈이 우선적으로 사용됩니다.
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(EventProducerPort::class)
    fun eventProducerPort(
        applicationEventPublisher: ApplicationEventPublisher,
        properties: DomainEventPublisherProperties
    ): EventProducerPort {
        
        log.info("Configuring Spring Domain Event Producer with properties: async={}, retry={}, metrics={}", 
            properties.async, properties.enableRetry, properties.enableMetrics)
        
        if (properties.enableDebugLogging) {
            log.debug("Spring Domain Event Producer debug logging is enabled")
        }
        
        return SpringDomainEventProducer(applicationEventPublisher, properties)
    }
    
    /**
     * 호환성을 위한 DomainEventPublisher 별칭
     */
    @Bean
    @ConditionalOnMissingBean(name = ["domainEventPublisher"])
    fun domainEventPublisher(eventProducerPort: EventProducerPort): EventProducerPort {
        return eventProducerPort
    }

    /**
     * 설정 검증 및 경고
     */
    @Bean
    fun domainEventPublisherConfigurationValidator(
        properties: DomainEventPublisherProperties
    ): DomainEventPublisherConfigurationValidator {
        return DomainEventPublisherConfigurationValidator(properties)
    }
    
    /**
     * Domain Event Publisher 설정 검증기
     * 
     * 잘못된 설정이 있을 경우 경고 로그를 출력합니다.
     */
    class DomainEventPublisherConfigurationValidator(
        private val properties: DomainEventPublisherProperties
    ) {
        
        private val log = LoggerFactory.getLogger(javaClass)
        
        init {
            validateConfiguration()
        }
        
        private fun validateConfiguration() {
            // 재시도 설정 검증
            if (properties.enableRetry && properties.maxRetryAttempts <= 0) {
                log.warn("Domain event retry is enabled but maxRetryAttempts is {}, " +
                    "should be greater than 0", properties.maxRetryAttempts)
            }
            
            // 타임아웃 설정 검증
            if (properties.timeoutMs < 0) {
                log.warn("Domain event timeout is negative: {}ms, this may cause issues", 
                    properties.timeoutMs)
            }
            
            // 재시도 지연 설정 검증
            if (properties.enableRetry && properties.retryDelayMs <= 0) {
                log.warn("Domain event retry is enabled but retryDelayMs is {}ms, " +
                    "should be greater than 0", properties.retryDelayMs)
            }
            
            // 운영 환경에서 디버그 로깅 경고
            if (properties.enableDebugLogging) {
                log.warn("Domain event debug logging is enabled, this may impact performance in production")
            }
            
            // 유효성 검증 설정 확인
            if (properties.validation.requiredFields.isEmpty()) {
                log.warn("Domain event validation has no required fields, " +
                    "this may allow invalid events to be published")
            }
            
            log.info("Domain Event Publisher configuration validated successfully")
        }
    }
}
