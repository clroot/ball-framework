package io.clroot.ball.adapter.inbound.messaging.consumer.inmemory

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

/**
 * 인메모리 이벤트 컨슈머 자동 설정
 */
@Configuration
@EnableConfigurationProperties(InMemoryEventConsumerProperties::class)
@EnableAsync
class InMemoryEventConsumerConfiguration(
    private val properties: InMemoryEventConsumerProperties
) {

    /**
     * 이벤트 처리용 스레드 풀 설정
     */
    @Bean("eventTaskExecutor")
    fun eventTaskExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        
        // 스레드 풀 크기 설정
        val corePoolSize = if (properties.parallel) {
            minOf(properties.maxConcurrency, 5)
        } else {
            1
        }
        
        executor.corePoolSize = corePoolSize
        executor.maxPoolSize = properties.maxConcurrency
        executor.queueCapacity = 100
        executor.keepAliveSeconds = 60
        
        // 스레드 이름 설정
        executor.setThreadNamePrefix("inmemory-event-")
        executor.setWaitForTasksToCompleteOnShutdown(true)
        executor.setAwaitTerminationSeconds(30)
        
        executor.initialize()
        return executor
    }
}
