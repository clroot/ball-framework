package io.clroot.ball.adapter.inbound.messaging.consumer.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.clroot.ball.adapter.inbound.messaging.consumer.core.executor.DomainEventHandlerExecutor
import io.clroot.ball.adapter.inbound.messaging.consumer.kafka.config.KafkaConsumerConfiguration
import io.clroot.ball.adapter.inbound.messaging.consumer.kafka.converter.DomainEventKafkaMessageConverter
import io.clroot.ball.adapter.inbound.messaging.consumer.kafka.listener.KafkaEventListener
import io.clroot.ball.application.event.DomainEventHandler
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

/**
 * Kafka Event Consumer Auto Configuration
 *
 * 이 모듈을 의존성에 추가하면 자동으로 Kafka Event Consumer가 활성화됩니다.
 *
 * 활성화 조건:
 * 1. DomainEventHandler 클래스가 클래스패스에 존재
 * 2. Kafka 클래스들이 클래스패스에 존재
 * 3. ball.event.consumer.kafka.enabled=true (기본값)
 * 4. 필요한 Bean 들이 없을 때 자동 생성
 * 
 * Core 모듈의 공통 컴포넌트들을 재사용하여 Kafka 전용 구현을 제공합니다.
 */
@AutoConfiguration
@ConditionalOnClass(
    value = [
        DomainEventHandler::class,
        org.springframework.kafka.core.KafkaTemplate::class  // Kafka 의존성 확인
    ]
)
@ConditionalOnProperty(
    name = ["ball.event.consumer.kafka.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@EnableConfigurationProperties(KafkaEventConsumerProperties::class)
@ComponentScan(basePackages = [
    "io.clroot.ball.adapter.inbound.messaging.consumer.core",  // Core 컴포넌트들
    "io.clroot.ball.adapter.inbound.messaging.consumer.kafka"  // Kafka 전용 컴포넌트들
])
@Import(KafkaConsumerConfiguration::class)  // Kafka Consumer 설정 임포트
@EnableKafka  // Spring Kafka 활성화
class KafkaEventConsumerAutoConfiguration {

    /**
     * Jackson ObjectMapper 자동 설정
     *
     * Kafka 메시지 변환을 위한 JSON 처리기를 생성합니다.
     * Kotlin 지원을 포함합니다.
     */
    @Bean
    @ConditionalOnMissingBean
    fun kafkaObjectMapper(): ObjectMapper {
        return ObjectMapper().apply {
            registerModule(KotlinModule.Builder().build())
            // 추가 Jackson 설정
            findAndRegisterModules()
        }
    }

    /**
     * 도메인 이벤트 Kafka 메시지 변환기 자동 설정
     *
     * Kafka 메시지를 도메인 이벤트로 변환하는 컨버터를 생성합니다.
     */
    @Bean
    @ConditionalOnMissingBean
    fun domainEventKafkaMessageConverter(
        objectMapper: ObjectMapper
    ): DomainEventKafkaMessageConverter {
        return DomainEventKafkaMessageConverter(objectMapper)
    }

    /**
     * Kafka Event Listener 자동 설정
     *
     * Core 모듈의 DomainEventHandlerExecutor를 사용하여 Kafka 이벤트를 처리합니다.
     */
    @Bean
    @ConditionalOnMissingBean
    fun kafkaEventListener(
        handlerExecutor: DomainEventHandlerExecutor,
        properties: KafkaEventConsumerProperties,
        messageConverter: DomainEventKafkaMessageConverter
    ): KafkaEventListener {
        return KafkaEventListener(handlerExecutor, properties, messageConverter)
    }

    /**
     * Kafka 이벤트 처리용 스레드 풀 자동 설정
     *
     * Kafka 메시지를 비동기로 처리하기 위한 전용 스레드 풀을 생성합니다.
     */
    @Bean("kafkaEventTaskExecutor")
    @ConditionalOnMissingBean(name = ["kafkaEventTaskExecutor"])
    fun kafkaEventTaskExecutor(properties: KafkaEventConsumerProperties): Executor {
        val executor = ThreadPoolTaskExecutor()

        // Kafka 메시지 처리를 위한 스레드 풀 크기 설정
        val corePoolSize = if (properties.parallel) {
            minOf(properties.maxConcurrency, 5)
        } else {
            1
        }

        executor.corePoolSize = corePoolSize
        executor.maxPoolSize = properties.maxConcurrency
        executor.queueCapacity = 200  // Kafka는 더 많은 큐 용량 필요
        executor.keepAliveSeconds = 60

        // 스레드 이름 설정
        executor.setThreadNamePrefix("kafka-event-")
        executor.setWaitForTasksToCompleteOnShutdown(true)
        executor.setAwaitTerminationSeconds(30)

        executor.initialize()
        return executor
    }

    /**
     * Kafka Blocking 작업용 스레드 풀 자동 설정
     *
     * JPA, JDBC 등 blocking I/O 작업을 위한 전용 스레드 풀을 생성합니다.
     * Core 모듈의 DomainEventHandlerExecutor에서 사용됩니다.
     */
    @Bean("kafkaBlockingTaskExecutor")
    @ConditionalOnMissingBean(name = ["kafkaBlockingTaskExecutor"])
    fun kafkaBlockingTaskExecutor(properties: KafkaEventConsumerProperties): Executor {
        val executor = ThreadPoolTaskExecutor()

        // Kafka + blocking 작업을 위한 더 많은 스레드 할당
        executor.corePoolSize = minOf(properties.maxConcurrency * 2, 30)
        executor.maxPoolSize = properties.maxConcurrency * 4  // Kafka는 더 많은 스레드 필요
        executor.queueCapacity = 500  // 큰 큐 용량
        executor.keepAliveSeconds = 120

        // 스레드 이름 설정
        executor.setThreadNamePrefix("kafka-blocking-")
        executor.setWaitForTasksToCompleteOnShutdown(true)
        executor.setAwaitTerminationSeconds(60)

        executor.initialize()
        return executor
    }

    /**
     * Kafka Consumer 헬스 체크용 빈 (선택사항)
     * 
     * Kafka Consumer의 상태를 모니터링하기 위한 컴포넌트입니다.
     */
    @Bean
    @ConditionalOnMissingBean
    fun kafkaConsumerHealthIndicator(
        properties: KafkaEventConsumerProperties
    ): KafkaConsumerHealthIndicator {
        return KafkaConsumerHealthIndicator(properties)
    }
}

/**
 * Kafka Consumer 헬스 체크 인디케이터
 * 
 * Spring Boot Actuator와 연동하여 Kafka Consumer의 상태를 확인할 수 있습니다.
 */
class KafkaConsumerHealthIndicator(
    private val properties: KafkaEventConsumerProperties
) {
    
    fun isHealthy(): Boolean {
        // 실제 구현에서는 Kafka 연결 상태, Consumer 상태 등을 확인
        return properties.enabled
    }
    
    fun getHealthInfo(): Map<String, Any> {
        return mapOf(
            "enabled" to properties.enabled,
            "topics" to properties.topics,
            "groupId" to properties.groupId,
            "bootstrapServers" to properties.bootstrapServers,
            "concurrency" to properties.concurrency,
            "maxConcurrency" to properties.maxConcurrency
        )
    }
}
