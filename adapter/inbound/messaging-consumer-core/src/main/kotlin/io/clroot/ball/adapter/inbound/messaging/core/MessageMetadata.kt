package io.clroot.ball.adapter.inbound.messaging.core

import java.time.Instant

/**
 * 메시지 메타데이터를 저장하는 데이터 클래스
 *
 * @property messageId 메시지 고유 식별자
 * @property timestamp 메시지 발행 시간
 * @property headers 메시지 헤더 맵
 * @property source 메시지 소스 (발신자 식별 정보)
 * @property eventType 이벤트 타입
 */
data class MessageMetadata(
    val messageId: String,
    val timestamp: Instant,
    val headers: Map<String, String> = emptyMap(),
    val source: String? = null,
    val eventType: String? = null
) {
    companion object {
        /**
         * 기본 메타데이터 생성
         *
         * @param messageId 메시지 ID (기본값: 랜덤 UUID)
         * @param timestamp 타임스탬프 (기본값: 현재 시간)
         * @return 기본 메타데이터 인스턴스
         */
        fun createDefault(
            messageId: String = java.util.UUID.randomUUID().toString(),
            timestamp: Instant = Instant.now()
        ): MessageMetadata {
            return MessageMetadata(
                messageId = messageId,
                timestamp = timestamp
            )
        }
    }
}