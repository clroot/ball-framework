package io.clroot.ball.adapter.inbound.event.consumer.domain.config

import io.clroot.ball.adapter.inbound.event.consumer.domain.DomainEventConsumerProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

/**
 * 도메인 이벤트 소비자 설정
 * 
 * 도메인 이벤트 처리에 필요한 모든 컴포넌트들을 설정합니다.
 */
@Configuration
@EnableConfigurationProperties(DomainEventConsumerProperties::class)
@ComponentScan(basePackages = ["io.clroot.ball.adapter.inbound.event.consumer.domain"])
@Import(DomainEventListenerConfiguration::class)
class DomainEventConsumerConfiguration
