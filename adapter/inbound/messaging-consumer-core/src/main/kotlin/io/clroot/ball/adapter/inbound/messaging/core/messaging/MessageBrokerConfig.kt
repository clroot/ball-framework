package io.clroot.ball.adapter.inbound.messaging.core.messaging

/**
 * 메시지 브로커 기본 설정 인터페이스
 * 다양한 메시징 시스템에 공통으로 적용할 수 있는 기본 설정 속성
 */
interface MessageBrokerConfig {
    /**
     * 브로커 호스트 목록
     */
    val hosts: List<String>
    
    /**
     * 연결당 최대 메시지 수
     */
    val maxMessagesPerConnection: Int
    
    /**
     * 소비자 그룹 ID 접두사
     */
    val consumerGroupIdPrefix: String
    
    /**
     * 기본 재시도 횟수
     */
    val defaultRetryCount: Int
    
    /**
     * DLQ(Dead Letter Queue) 활성화 여부
     */
    val enableDlq: Boolean
    
    /**
     * 추가 설정 맵
     */
    val additionalConfig: Map<String, String>
    
    /**
     * 브로커 타입 (예: "kafka", "rabbitmq")
     */
    val brokerType: String
}