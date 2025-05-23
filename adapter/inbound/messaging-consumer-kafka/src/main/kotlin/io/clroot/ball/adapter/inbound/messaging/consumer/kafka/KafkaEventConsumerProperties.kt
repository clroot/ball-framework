package io.clroot.ball.adapter.inbound.messaging.consumer.kafka

import io.clroot.ball.adapter.inbound.messaging.consumer.core.properties.EventConsumerProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty

/**
 * Kafka Event Consumer configuration properties.
 * 
 * Core 모듈의 공통 설정을 확장하여 Kafka 전용 설정을 추가합니다.
 * 
 * Configuration prefix: ball.event.consumer.kafka
 * 
 * Example configuration:
 * ```yaml
 * ball:
 *   event:
 *     consumer:
 *       kafka:
 *         enabled: true
 *         # 공통 설정들 (core에서 상속)
 *         async: true
 *         parallel: true
 *         maxConcurrency: 10
 *         timeoutMs: 30000
 *         enableRetry: true
 *         maxRetryAttempts: 3
 *         retryDelayMs: 1000
 *         # Kafka 전용 설정들
 *         topics:
 *           - "domain-events"
 *           - "user-events"
 *         groupId: "ball-framework"
 *         bootstrapServers: "localhost:9092"
 *         autoOffsetReset: "earliest"
 *         enableAutoCommit: false
 *         maxPollRecords: 500
 *         fetchMinBytes: 1
 *         fetchMaxWaitMs: 500
 *         sessionTimeoutMs: 30000
 *         heartbeatIntervalMs: 3000
 *         maxPollIntervalMs: 300000
 *         concurrency: 3
 *         enableDlq: true
 *         dlqTopic: "domain-events-dlq"
 * ```
 */
@ConfigurationProperties(prefix = "ball.event.consumer.kafka")
data class KafkaEventConsumerProperties(
    /**
     * Kafka 컨슈머 활성화 여부
     * 기본값: true
     */
    override val enabled: Boolean = true,

    /**
     * 비동기 이벤트 처리 여부  
     * 기본값: true
     */
    override val async: Boolean = true,

    /**
     * 핸들러 병렬 실행 여부
     * 기본값: true
     */
    override val parallel: Boolean = true,

    /**
     * 최대 동시 실행 수
     * 기본값: 10
     */
    override val maxConcurrency: Int = 10,

    /**
     * 핸들러 실행 타임아웃 (밀리초)
     * 기본값: 30000 (30초)
     */
    override val timeoutMs: Long = 30000,

    /**
     * 재시도 활성화 여부
     * 기본값: true
     */
    override val enableRetry: Boolean = true,

    /**
     * 최대 재시도 횟수
     * 기본값: 3
     */
    override val maxRetryAttempts: Int = 3,

    /**
     * 재시도 지연 시간 (밀리초)
     * 기본값: 1000 (1초)
     */
    override val retryDelayMs: Long = 1000,

    /**
     * 에러 핸들링 설정 (상속)
     */
    @NestedConfigurationProperty
    override val errorHandling: io.clroot.ball.adapter.inbound.messaging.consumer.core.properties.ErrorHandlingProperties = 
        io.clroot.ball.adapter.inbound.messaging.consumer.core.properties.ErrorHandlingProperties(),

    // ========== Kafka 전용 설정 ==========

    /**
     * 구독할 Kafka 토픽 목록
     * 
     * 도메인 이벤트가 발행될 토픽들을 지정합니다.
     * 기본값: ["domain-events"]
     */
    val topics: List<String> = listOf("domain-events"),

    /**
     * Kafka Consumer Group ID
     * 
     * 같은 그룹 ID를 가진 컨슈머들은 토픽의 파티션을 분할하여 처리합니다.
     * 기본값: "ball-framework"
     */
    val groupId: String = "ball-framework",

    /**
     * Kafka Bootstrap Servers
     * 
     * Kafka 클러스터의 브로커 주소 목록입니다.
     * 기본값: "localhost:9092"
     */
    val bootstrapServers: String = "localhost:9092",

    /**
     * Auto Offset Reset 정책
     * 
     * - earliest: 파티션의 가장 처음부터 읽기
     * - latest: 파티션의 가장 최근부터 읽기
     * - none: 오프셋이 없으면 예외 발생
     * 기본값: "earliest"
     */
    val autoOffsetReset: String = "earliest",

    /**
     * Auto Commit 활성화 여부
     * 
     * false로 설정하면 수동으로 오프셋을 커밋해야 합니다.
     * 이벤트 처리 성공 후에만 커밋하여 메시지 손실을 방지합니다.
     * 기본값: false
     */
    val enableAutoCommit: Boolean = false,

    /**
     * 한 번의 poll()에서 가져올 최대 레코드 수
     * 
     * 너무 크면 메모리 사용량이 증가하고, 너무 작으면 처리량이 떨어집니다.
     * 기본값: 500
     */
    val maxPollRecords: Int = 500,

    /**
     * Fetch 최소 바이트 수
     * 
     * 브로커에서 이 크기만큼의 데이터가 쌓일 때까지 기다립니다.
     * 기본값: 1
     */
    val fetchMinBytes: Int = 1,

    /**
     * Fetch 최대 대기 시간 (밀리초)
     * 
     * fetchMinBytes 조건이 충족되지 않아도 이 시간이 지나면 응답합니다.
     * 기본값: 500
     */
    val fetchMaxWaitMs: Int = 500,

    /**
     * 세션 타임아웃 (밀리초)
     * 
     * 컨슈머가 이 시간 동안 하트비트를 보내지 않으면 죽은 것으로 간주됩니다.
     * 기본값: 30000 (30초)
     */
    val sessionTimeoutMs: Int = 30000,

    /**
     * 하트비트 간격 (밀리초)
     * 
     * 일반적으로 sessionTimeoutMs / 3 정도로 설정합니다.
     * 기본값: 3000 (3초)
     */
    val heartbeatIntervalMs: Int = 3000,

    /**
     * 최대 Poll 간격 (밀리초)
     * 
     * poll() 호출 사이의 최대 간격입니다. 이 시간을 초과하면 컨슈머가 죽은 것으로 간주됩니다.
     * 기본값: 300000 (5분)
     */
    val maxPollIntervalMs: Int = 300000,

    /**
     * 컨슈머 동시성 (파티션별 스레드 수)
     * 
     * 각 토픽 파티션마다 별도의 컨슈머 인스턴스를 실행합니다.
     * 파티션 수보다 클 수 없습니다.
     * 기본값: 3
     */
    val concurrency: Int = 3,

    /**
     * Dead Letter Queue 활성화 여부
     * 
     * 처리에 실패한 메시지를 별도의 토픽으로 전송할지 여부입니다.
     * 기본값: true
     */
    val enableDlq: Boolean = true,

    /**
     * Dead Letter Queue 토픽 이름
     * 
     * enableDlq가 true일 때 실패한 메시지가 전송될 토픽입니다.
     * 기본값: "domain-events-dlq"
     */
    val dlqTopic: String = "domain-events-dlq",

    /**
     * Kafka 특화 에러 핸들링 설정
     */
    @NestedConfigurationProperty
    val kafkaErrorHandling: KafkaErrorHandlingProperties = KafkaErrorHandlingProperties()

) : EventConsumerProperties(
    enabled = enabled,
    async = async,
    parallel = parallel,
    maxConcurrency = maxConcurrency,
    timeoutMs = timeoutMs,
    enableRetry = enableRetry,
    maxRetryAttempts = maxRetryAttempts,
    retryDelayMs = retryDelayMs,
    errorHandling = errorHandling
)

/**
 * Kafka 특화 에러 핸들링 설정
 */
data class KafkaErrorHandlingProperties(
    /**
     * 오프셋 커밋 실패 시 재시도 횟수
     * 기본값: 3
     */
    val commitRetryAttempts: Int = 3,

    /**
     * 오프셋 커밋 재시도 간격 (밀리초)
     * 기본값: 100
     */
    val commitRetryDelayMs: Long = 100,

    /**
     * 파티션 리밸런싱 시 처리 중인 메시지 완료 대기 시간 (밀리초)
     * 기본값: 30000 (30초)
     */
    val rebalanceTimeoutMs: Long = 30000,

    /**
     * 컨슈머 재시작 시 지연 시간 (밀리초)
     * 기본값: 5000 (5초)
     */
    val consumerRestartDelayMs: Long = 5000
)
