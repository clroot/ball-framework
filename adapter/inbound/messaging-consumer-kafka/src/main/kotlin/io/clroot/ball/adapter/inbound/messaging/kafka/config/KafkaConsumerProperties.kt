package io.clroot.ball.adapter.inbound.messaging.kafka.config

import io.clroot.ball.adapter.inbound.messaging.core.messaging.MessageBrokerConfig
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Kafka 소비자 설정 프로퍼티
 * application.yml/properties에서 설정 가능
 *
 * @property hosts Kafka 브로커 호스트 목록
 * @property maxMessagesPerConnection 연결당 최대 메시지 수
 * @property consumerGroupIdPrefix 소비자 그룹 ID 접두사
 * @property defaultRetryCount 기본 재시도 횟수
 * @property enableDlq DLQ(Dead Letter Queue) 활성화 여부
 * @property additionalConfig 추가 설정 맵
 * @property brokerType 브로커 타입 (항상 "kafka")
 * @property enableAutoCommit 자동 커밋 활성화 여부
 * @property autoCommitIntervalMs 자동 커밋 간격 (밀리초)
 * @property maxPollIntervalMs 최대 폴링 간격 (밀리초)
 * @property sessionTimeoutMs 세션 타임아웃 (밀리초)
 * @property concurrency 동시성 (리스너 스레드 수)
 * @property offsetReset 오프셋 리셋 전략 ("earliest", "latest")
 * @property dlqSuffix DLQ 토픽 접미사
 */
@ConfigurationProperties(prefix = "kafka.consumer")
data class KafkaConsumerProperties(
    override val hosts: List<String> = listOf("localhost:9092"),
    override val maxMessagesPerConnection: Int = 10,
    override val consumerGroupIdPrefix: String = "ball-consumer",
    override val defaultRetryCount: Int = 3,
    override val enableDlq: Boolean = true,
    override val additionalConfig: Map<String, String> = emptyMap(),
    override val brokerType: String = "kafka",

    // Kafka 특화 설정
    val enableAutoCommit: Boolean = false,
    val autoCommitIntervalMs: Int = 5000,
    val maxPollIntervalMs: Int = 300000,
    val sessionTimeoutMs: Int = 30000,
    val concurrency: Int = 1,
    val offsetReset: String = "earliest",
    val dlqSuffix: String = ".dlq"
) : MessageBrokerConfig
