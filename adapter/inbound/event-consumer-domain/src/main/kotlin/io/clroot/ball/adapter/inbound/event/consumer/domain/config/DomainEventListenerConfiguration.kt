package io.clroot.ball.adapter.inbound.event.consumer.domain.config

import io.clroot.ball.adapter.inbound.event.consumer.domain.DomainEventConsumerProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ApplicationEventMulticaster
import org.springframework.context.event.SimpleApplicationEventMulticaster
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.ThreadPoolExecutor

/**
 * 도메인 이벤트 리스너 설정
 * 
 * Spring ApplicationEvent 기반의 도메인 이벤트 처리를 위한 설정을 담당합니다.
 */
@Configuration
@EnableAsync
class DomainEventListenerConfiguration(
    private val properties: DomainEventConsumerProperties
) {
    
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 도메인 이벤트 전용 TaskExecutor
     * 
     * 비동기 이벤트 처리가 활성화된 경우에만 생성됩니다.
     */
    @Bean("domainEventTaskExecutor")
    @ConditionalOnProperty(
        prefix = "ball.events.domain.consumer", 
        name = ["async"], 
        havingValue = "true"
    )
    fun domainEventTaskExecutor(): TaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        
        // 코어 스레드 수: CPU 코어 수
        executor.corePoolSize = Runtime.getRuntime().availableProcessors()
        
        // 최대 스레드 수: 코어 수의 2배 (도메인 이벤트는 CPU 집약적)
        executor.maxPoolSize = Runtime.getRuntime().availableProcessors() * 2
        
        // 큐 용량: 100개 (도메인 이벤트는 빠르게 처리되어야 함)
        executor.queueCapacity = 100
        
        // 스레드 이름 접두사
        executor.setThreadNamePrefix("domain-event-")
        
        // 거부 정책: CallerRuns (큐가 가득 찰 경우 호출자 스레드에서 실행)
        executor.setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
        
        // 스레드 풀 종료 대기
        executor.setWaitForTasksToCompleteOnShutdown(true)
        executor.setAwaitTerminationSeconds(30)
        
        executor.initialize()
        
        log.info("Initialized domain event task executor - core: {}, max: {}, queue: {}", 
            executor.corePoolSize, executor.maxPoolSize, executor.queueCapacity)
        
        return executor
    }

    /**
     * 도메인 이벤트 전용 ApplicationEventMulticaster
     * 
     * 비동기 처리가 필요한 경우 전용 TaskExecutor를 사용합니다.
     */
    @Bean("domainEventMulticaster")
    @ConditionalOnProperty(
        prefix = "ball.events.domain.consumer", 
        name = ["async"], 
        havingValue = "true"
    )
    fun domainEventMulticaster(domainEventTaskExecutor: TaskExecutor): ApplicationEventMulticaster {
        val multicaster = SimpleApplicationEventMulticaster()
        multicaster.setTaskExecutor(domainEventTaskExecutor)
        
        log.info("Configured domain event multicaster with async task executor")
        
        return multicaster
    }
}
