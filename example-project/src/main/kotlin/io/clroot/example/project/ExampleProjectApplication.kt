package io.clroot.example.project

import io.clroot.ball.application.event.DomainEventHandler
import io.clroot.ball.application.port.outbound.DomainEventPublisher
import io.clroot.ball.domain.event.DomainEventBase
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.stereotype.Component

@SpringBootApplication
class ExampleProjectApplication

fun main(args: Array<String>) {
    val application = SpringApplication(ExampleProjectApplication::class.java)
    application.run(*args)
}

data class TestEvent(
    val message: String = ""
) : DomainEventBase()

@Component
class AfterStart(
    private val domainEventPublisher: DomainEventPublisher
) : CommandLineRunner {
    private val logger = LoggerFactory.getLogger(javaClass)
    override fun run(vararg args: String?) {
        val event = TestEvent(message = "Hello World!")
        domainEventPublisher.publish(event)
        logger.info("Published event: {}", event)
    }
}

@Component
class TestEventHandler : DomainEventHandler<TestEvent> {
    private val logger = LoggerFactory.getLogger(javaClass)
    override suspend fun handle(event: TestEvent) {
        logger.info("Received event: {}", event)
    }
}