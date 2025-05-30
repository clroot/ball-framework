package io.clroot.ball.adapter.inbound.event.consumer.core

import io.clroot.ball.application.port.inbound.EventConsumerPort
import io.clroot.ball.domain.event.DomainEvent
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

/**
 * 이벤트 핸들러 레지스트리
 *
 * 애플리케이션 컨텍스트에서 이벤트 핸들러들을 찾아 등록하고 관리합니다.
 *
 * 지원하는 핸들러 유형:
 * 1. EventConsumerPort 구현체 (권장)
 * 2. @DomainEventHandler 어노테이션 메서드 (호환성)
 *
 * 헥사고날 아키텍처에서 어댑터는 순수하게 기술적 연결만 담당하며,
 * 실제 핸들러는 애플리케이션 계층에 위치합니다.
 */
@Component
class EventHandlerRegistry : ApplicationContextAware {

    private val log = LoggerFactory.getLogger(javaClass)
    private lateinit var applicationContext: ApplicationContext

    // 이벤트 타입별 핸들러 매핑
    private val handlerMap = ConcurrentHashMap<Class<out DomainEvent>, MutableList<EventHandlerMethod>>()

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
    }

    @PostConstruct
    fun initialize() {
        scanAndRegisterHandlers()
    }

    /**
     * 특정 이벤트 타입에 대한 핸들러들 반환
     */
    fun getHandlers(eventType: Class<out DomainEvent>): List<EventHandlerMethod> {
        return handlerMap[eventType]?.toList() ?: emptyList()
    }

    /**
     * 모든 핸들러가 처리하는 이벤트 타입들 반환
     */
    fun getAllHandledEventTypes(): Set<Class<out DomainEvent>> {
        return handlerMap.keys.toSet()
    }

    /**
     * 핸들러 등록
     */
    fun registerHandler(eventType: Class<out DomainEvent>, handler: EventHandlerMethod) {
        handlerMap.computeIfAbsent(eventType) { mutableListOf() }.add(handler)
        log.debug("Registered event handler: {} -> {}", eventType.simpleName, handler.methodName)
    }

    /**
     * 핸들러 제거
     */
    fun unregisterHandler(eventType: Class<out DomainEvent>, handler: EventHandlerMethod) {
        handlerMap[eventType]?.remove(handler)
        log.debug("Unregistered event handler: {} -> {}", eventType.simpleName, handler.methodName)
    }

    /**
     * 애플리케이션 컨텍스트에서 이벤트 핸들러들을 스캔하고 등록
     */
    private fun scanAndRegisterHandlers() {
        log.info("Scanning for event handlers...")

        var totalHandlers = 0

        totalHandlers += scanPortBasedHandlers()

        log.info(
            "Event handler scanning completed. Registered {} handlers for {} event types",
            totalHandlers,
            handlerMap.size
        )
    }

    /**
     * EventConsumerPort 구현체들 스캔
     */
    @Suppress("UNCHECKED_CAST")
    private fun scanPortBasedHandlers(): Int {
        val portHandlers = applicationContext.getBeansOfType(EventConsumerPort::class.java)
        var count = 0

        for ((beanName, handler) in portHandlers) {
            try {
                val eventType = handler.eventType.java as Class<out DomainEvent>

                val handlerMethod = EventHandlerMethod(
                    bean = handler,
                    method = handler.javaClass.getMethod("consume", eventType),
                    eventType = eventType,
                    methodName = "${handler.handlerName}.consume",
                    async = handler.async,
                    order = handler.order
                )

                registerHandler(eventType, handlerMethod)
                count++

                log.debug(
                    "Found EventConsumerPort: {} -> {} (async={}, order={})",
                    eventType.simpleName,
                    handler.handlerName,
                    handler.async,
                    handler.order
                )

            } catch (e: Exception) {
                log.warn("Failed to register EventConsumerPort bean: {}", beanName, e)
            }
        }

        log.info("Registered {} EventConsumerPort implementations", count)
        return count
    }

    /**
     * 메서드에서 이벤트 타입 추출
     */
    @Suppress("UNCHECKED_CAST")
    private fun extractEventTypeFromMethod(method: Method): Class<out DomainEvent>? {
        val parameterTypes = method.parameterTypes

        if (parameterTypes.isEmpty()) {
            log.warn("Event handler method has no parameters: {}", method.name)
            return null
        }

        val firstParameter = parameterTypes[0]

        return if (DomainEvent::class.java.isAssignableFrom(firstParameter)) {
            firstParameter as Class<out DomainEvent>
        } else {
            log.warn(
                "Event handler method parameter is not a DomainEvent: {} ({})", method.name, firstParameter.simpleName
            )
            null
        }
    }
}
