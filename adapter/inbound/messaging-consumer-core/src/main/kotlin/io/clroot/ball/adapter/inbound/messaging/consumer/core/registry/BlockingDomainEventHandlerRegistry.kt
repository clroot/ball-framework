package io.clroot.ball.adapter.inbound.messaging.consumer.core.registry

import io.clroot.ball.application.event.BlockingDomainEventHandler
import io.clroot.ball.domain.event.DomainEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Blocking 도메인 이벤트 핸들러 레지스트리
 * 
 * 스프링 컨텍스트에서 BlockingDomainEventHandler 구현체들을 자동으로 발견하고,
 * 이벤트 타입별로 매핑하여 관리합니다.
 * 
 * JPA, JDBC 등 blocking I/O를 사용하는 핸들러들을 위한 레지스트리입니다.
 * 이 클래스는 messaging-consumer-core 모듈의 공통 컴포넌트입니다.
 */
@Component
class BlockingDomainEventHandlerRegistry(
    handlers: List<BlockingDomainEventHandler<*>>
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private lateinit var handlerMap: Map<Class<out DomainEvent>, List<BlockingDomainEventHandler<*>>>

    init {
        handlerMap = buildHandlerMap(handlers)
        log.info("Registered {} blocking domain event handlers for {} event types", 
            handlers.size, handlerMap.size)
        
        if (log.isDebugEnabled) {
            handlerMap.forEach { (eventType, handlers) ->
                log.debug("Event type: {} -> Blocking Handlers: {}", 
                    eventType.simpleName, 
                    handlers.map { it.javaClass.simpleName })
            }
        }
    }

    /**
     * 특정 이벤트 타입에 대한 블로킹 핸들러들 반환
     */
    fun getHandlers(eventType: Class<out DomainEvent>): List<BlockingDomainEventHandler<*>> {
        return handlerMap[eventType] ?: emptyList()
    }

    /**
     * 등록된 모든 이벤트 타입 반환
     */
    fun getRegisteredEventTypes(): Set<Class<out DomainEvent>> {
        return handlerMap.keys
    }

    /**
     * 핸들러 개수 반환
     */
    fun getHandlerCount(): Int {
        return handlerMap.values.sumOf { it.size }
    }

    /**
     * 특정 이벤트 타입의 핸들러 개수 반환
     */
    fun getHandlerCount(eventType: Class<out DomainEvent>): Int {
        return handlerMap[eventType]?.size ?: 0
    }

    /**
     * 핸들러 맵 구성
     */
    private fun buildHandlerMap(handlers: List<BlockingDomainEventHandler<*>>): Map<Class<out DomainEvent>, List<BlockingDomainEventHandler<*>>> {
        val map = mutableMapOf<Class<out DomainEvent>, MutableList<BlockingDomainEventHandler<*>>>()

        handlers.forEach { handler ->
            val eventTypes = resolveEventTypes(handler)
            eventTypes.forEach { eventType ->
                map.computeIfAbsent(eventType) { mutableListOf() }.add(handler)
                log.debug("Registered blocking handler {} for event type {}", 
                    handler.javaClass.simpleName, eventType.simpleName)
            }
        }

        return map.mapValues { it.value.toList() }
    }

    /**
     * 핸들러가 처리할 수 있는 이벤트 타입들 추출
     */
    @Suppress("UNCHECKED_CAST")
    private fun resolveEventTypes(handler: BlockingDomainEventHandler<*>): List<Class<out DomainEvent>> {
        val eventTypes = mutableListOf<Class<out DomainEvent>>()

        // 클래스의 모든 인터페이스 확인
        val allInterfaces = getAllInterfaces(handler.javaClass)
        
        allInterfaces.forEach { interfaceType ->
            if (interfaceType is ParameterizedType) {
                val rawType = interfaceType.rawType
                if (rawType == BlockingDomainEventHandler::class.java) {
                    val typeArguments = interfaceType.actualTypeArguments
                    if (typeArguments.isNotEmpty()) {
                        val eventType = typeArguments[0]
                        if (eventType is Class<*> && DomainEvent::class.java.isAssignableFrom(eventType)) {
                            eventTypes.add(eventType as Class<out DomainEvent>)
                        }
                    }
                }
            }
        }

        if (eventTypes.isEmpty()) {
            log.warn("Could not resolve event types for blocking handler: {}", handler.javaClass.name)
        }

        return eventTypes
    }

    /**
     * 클래스의 모든 인터페이스(제네릭 정보 포함) 추출
     */
    private fun getAllInterfaces(clazz: Class<*>): Set<Type> {
        val interfaces = mutableSetOf<Type>()
        
        // 직접 구현한 인터페이스들
        interfaces.addAll(clazz.genericInterfaces)
        
        // 상위 클래스들의 인터페이스들
        var currentClass = clazz.superclass
        while (currentClass != null && currentClass != Object::class.java) {
            interfaces.addAll(currentClass.genericInterfaces)
            currentClass = currentClass.superclass
        }
        
        return interfaces
    }
}
