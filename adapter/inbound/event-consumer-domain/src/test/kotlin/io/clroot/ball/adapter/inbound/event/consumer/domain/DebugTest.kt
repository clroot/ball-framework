package io.clroot.ball.adapter.inbound.event.consumer.domain

import io.clroot.ball.adapter.inbound.event.consumer.core.EventHandlerMethodFactory
import io.clroot.ball.adapter.inbound.event.consumer.domain.test.TestDomainEventHandler

fun main() {
    val handler = TestDomainEventHandler()
    
    println("Handler class: ${handler.javaClass.simpleName}")
    println("Event type: ${handler.eventType}")
    println("Event type class: ${handler.eventType.java}")
    
    try {
        val handlerMethod = EventHandlerMethodFactory.createFromPort(handler)
        println("Success: $handlerMethod")
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    }
}
