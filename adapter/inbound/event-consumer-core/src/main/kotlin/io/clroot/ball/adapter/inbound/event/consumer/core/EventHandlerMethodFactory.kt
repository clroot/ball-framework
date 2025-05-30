package io.clroot.ball.adapter.inbound.event.consumer.core

import io.clroot.ball.application.port.inbound.EventConsumerPort
import io.clroot.ball.domain.event.DomainEvent
import io.clroot.ball.domain.event.Event
import java.lang.reflect.Method

/**
 * ThreadPoolEventHandlerMethod 생성을 위한 팩토리
 */
object EventHandlerMethodFactory {

    fun createFromPort(port: EventConsumerPort<*>): ThreadPoolEventHandlerMethod {
        return try {
            val eventType = port.eventType.java

            val method = findConsumeMethod(port, eventType)
                ?: throw RuntimeException(
                    "Cannot find consume method for port: ${port.javaClass.simpleName} " +
                            "with event type: ${eventType.simpleName}"
                )

            ThreadPoolEventHandlerMethod(
                port = port,
                eventType = eventType as Class<out DomainEvent>,
                methodName = "${port.handlerName}.consume",
                order = port.order
            )
        } catch (e: RuntimeException) {
            throw e
        } catch (e: Exception) {
            throw RuntimeException(
                "Failed to create ThreadPoolEventHandlerMethod for port: ${port.javaClass.simpleName}",
                e
            )
        }
    }

    fun createForTest(
        port: EventConsumerPort<*>,
        eventType: Class<out DomainEvent>
    ): ThreadPoolEventHandlerMethod {
        return try {
            val method = findConsumeMethodForTest(port.javaClass)
                ?: throw RuntimeException("No suitable method found in ${port.javaClass.simpleName}")

            ThreadPoolEventHandlerMethod(
                port = port,
                eventType = eventType,
                methodName = "${port.handlerName}.${method.name}",
                order = port.order
            )
        } catch (e: Exception) {
            println("❌ Failed to create ThreadPoolEventHandlerMethod for ${port.javaClass.simpleName}: ${e.message}")
            throw RuntimeException("Failed to create handler method", e)
        }
    }

    fun createFromMethod(
        bean: Any,
        method: Method,
        eventType: Class<out DomainEvent>,
        order: Int = 0
    ): ThreadPoolEventHandlerMethod {

        val wrappedPort = object : EventConsumerPort<Event> {
            override val eventType = Event::class
            override val order = order
            override val handlerName = "${bean.javaClass.simpleName}.${method.name}"

            override fun consume(event: Event) {
                method.isAccessible = true
                method.invoke(bean, event)
            }
        }

        return ThreadPoolEventHandlerMethod(
            port = wrappedPort,
            eventType = eventType,
            methodName = wrappedPort.handlerName,
            order = order
        )
    }

    private fun findConsumeMethod(port: EventConsumerPort<*>, eventType: Class<*>): Method? {
        val portClass = port.javaClass
        val consumeMethods = portClass.methods.filter { method ->
            method.name == "consume" &&
                    method.parameterCount == 1 &&
                    !method.isSynthetic
        }

        if (consumeMethods.isEmpty()) {
            return null
        }

        for (method in consumeMethods) {
            val paramType = method.parameterTypes[0]
            if (isCompatibleEventType(paramType, eventType)) {
                return method
            }
        }

        return consumeMethods.first()
    }

    private fun findConsumeMethodForTest(clazz: Class<*>): Method? {
        clazz.methods.find {
            it.name == "handleEvent" && it.parameterCount == 1
        }?.let {
            return it
        }

        val consumeMethods = clazz.methods.filter {
            it.name == "consume" && it.parameterCount == 1 && !it.isSynthetic
        }

        return consumeMethods.firstOrNull()
    }

    private fun isCompatibleEventType(paramType: Class<*>, eventType: Class<*>): Boolean {
        return paramType == eventType ||
                paramType.isAssignableFrom(eventType) ||
                eventType.isAssignableFrom(paramType) ||
                paramType.simpleName == eventType.simpleName
    }

    fun extractEventType(port: EventConsumerPort<*>): Class<out Event> {
        return port.eventType.java
    }

    fun validateHandlerMethod(method: Method, expectedEventType: Class<*>): Boolean {
        return method.name == "consume" &&
                method.parameterCount == 1 &&
                !method.isSynthetic &&
                isCompatibleEventType(method.parameterTypes[0], expectedEventType)
    }
}
