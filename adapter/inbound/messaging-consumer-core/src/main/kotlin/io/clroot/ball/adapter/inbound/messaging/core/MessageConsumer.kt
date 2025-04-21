package io.clroot.ball.adapter.inbound.messaging.core

import arrow.core.Either
import io.clroot.ball.shared.core.exception.DomainError

/**
 * 인바운드 메시지 소비 작업을 추상화하는 인터페이스
 * 메시징 시스템에 구애받지 않는 일반적인 메시지 소비자 정의
 *
 * @param P 메시지 페이로드 타입 (도메인 객체로 변환 전)
 */
interface MessageConsumer<P> {
    /**
     * 메시지 수신 및 처리
     *
     * @param payload 메시지 페이로드
     * @param metadata 메시지 메타데이터
     * @return 처리 결과 (성공 또는 도메인 에러)
     */
    suspend fun consume(payload: P, metadata: MessageMetadata): Either<DomainError, Unit>

    /**
     * 이 소비자가 처리할 수 있는 메시지 토픽 반환
     *
     * @return 토픽 이름
     */
    fun getTopicName(): String
}