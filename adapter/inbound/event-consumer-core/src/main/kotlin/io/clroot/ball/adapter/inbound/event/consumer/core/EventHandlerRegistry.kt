package io.clroot.ball.adapter.inbound.event.consumer.core

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
 * 이벤트 타입별로 핸들러들을 매핑하여 효율적인 이벤트 처리를 지원합니다.
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

        val handlerBeans = applicationContext.getBeansWithAnnotation(Component::class.java)
        var totalHandlers = 0

        for ((beanName, bean) in handlerBeans) {
            val handlerMethods = findEventHandlerMethods(bean)

            if (handlerMethods.isNotEmpty()) {
                log.debug("Found {} event handlers in bean: {}", handlerMethods.size, beanName)

                for (handlerMethod in handlerMethods) {
                    registerHandler(handlerMethod.eventType, handlerMethod)
                    totalHandlers++
                }
            }
        }

        log.info(
            "Event handler scanning completed. Registered {} handlers for {} event types",
            totalHandlers, handlerMap.size
        )
    }

    /**
     * 빈에서 이벤트 핸들러 메서드들을 찾기
     */
    private fun findEventHandlerMethods(bean: Any): List<EventHandlerMethod> {
        val handlers = mutableListOf<EventHandlerMethod>()
        val beanClass = bean.javaClass

        for (method in beanClass.declaredMethods) {
            // @EventHandler 어노테이션이 있는 메서드 찾기
            if (method.isAnnotationPresent(EventHandler::class.java)) {
                val eventType = extractEventTypeFromMethod(method)
                if (eventType != null) {
                    handlers.add(
                        EventHandlerMethod(
                            bean = bean,
                            method = method,
                            eventType = eventType,
                            methodName = "${beanClass.simpleName}.${method.name}"
                        )
                    )
                }
            }
        }

        return handlers
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
                "Event handler method parameter is not a DomainEvent: {} ({})",
                method.name, firstParameter.simpleName
            )
            null
        }
    }
}

