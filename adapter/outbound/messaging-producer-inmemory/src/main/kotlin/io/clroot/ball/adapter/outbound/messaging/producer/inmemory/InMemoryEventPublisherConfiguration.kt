package io.clroot.ball.adapter.outbound.messaging.producer.inmemory

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync

/**
 * 인메모리 이벤트 발행자 자동 설정
 */
@Configuration
@EnableConfigurationProperties(InMemoryEventPublisherProperties::class)
@EnableAsync
class InMemoryEventPublisherConfiguration
