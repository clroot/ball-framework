package io.clroot.ball.adapter.inbound.messaging.core

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * 메시지 소비자를 관리하는 레지스트리
 * 애플리케이션 내 모든 메시지 소비자를 등록하고 토픽별로 조회
 */
@Component
class MessageConsumerRegistry {
    private val log = LoggerFactory.getLogger(javaClass)
    
    // 토픽별 메시지 소비자 맵
    private val consumersByTopic = ConcurrentHashMap<String, MessageConsumer<*>>()

    /**
     * 메시지 소비자 등록
     *
     * @param consumer 등록할 메시지 소비자
     * @throws IllegalArgumentException 이미 해당 토픽에 소비자가 등록되어 있는 경우
     */
    @Suppress("UNCHECKED_CAST")
    fun <P> registerConsumer(consumer: MessageConsumer<P>) {
        val topic = consumer.getTopicName()
        
        if (topic.isBlank()) {
            throw IllegalArgumentException("Topic name cannot be blank")
        }
        
        val existing = consumersByTopic.putIfAbsent(topic, consumer)
        if (existing != null) {
            throw IllegalArgumentException(
                "Consumer for topic '$topic' is already registered: ${existing.javaClass.name}"
            )
        }
        
        log.info("Registered message consumer for topic '{}': {}", topic, consumer.javaClass.name)
    }

    /**
     * 토픽에 해당하는 메시지 소비자 조회
     *
     * @param topic 토픽 이름
     * @return 메시지 소비자 또는 null (해당 토픽에 등록된 소비자가 없는 경우)
     */
    @Suppress("UNCHECKED_CAST")
    fun <P> getConsumer(topic: String): MessageConsumer<P>? {
        return consumersByTopic[topic] as? MessageConsumer<P>
    }

    /**
     * 등록된 모든 토픽 목록 조회
     *
     * @return 등록된 토픽 집합
     */
    fun getRegisteredTopics(): Set<String> {
        return consumersByTopic.keys.toSet()
    }

    /**
     * 토픽에 해당하는 메시지 소비자 제거
     *
     * @param topic 토픽 이름
     * @return 제거된 소비자 또는 null (해당 토픽에 등록된 소비자가 없는 경우)
     */
    @Suppress("UNCHECKED_CAST")
    fun <P> unregisterConsumer(topic: String): MessageConsumer<P>? {
        val removed = consumersByTopic.remove(topic) as? MessageConsumer<P>
        if (removed != null) {
            log.info("Unregistered message consumer for topic '{}': {}", topic, removed.javaClass.name)
        }
        return removed
    }

    /**
     * 등록된 소비자 수 반환
     *
     * @return 등록된 소비자 수
     */
    fun size(): Int {
        return consumersByTopic.size
    }

    /**
     * 모든 소비자 등록 해제
     */
    fun clear() {
        consumersByTopic.clear()
        log.info("Cleared all registered message consumers")
    }
}