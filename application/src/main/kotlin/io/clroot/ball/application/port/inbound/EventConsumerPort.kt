package io.clroot.ball.application.port.inbound

import io.clroot.ball.domain.event.Event
import kotlin.reflect.KClass

/**
 * 이벤트 소비 포트 (Event Consumer Port) - ThreadPool 기반
 *
 * 이벤트를 수신하고 처리하는 핸들러의 계약을 정의합니다.
 * 헥사고날 아키텍처에서 애플리케이션 계층의 진입점(Inbound Port) 역할을 합니다.
 *
 * ThreadPool 기반 설계:
 * - JPA 등 blocking I/O에 최적화
 * - 단순하고 직관적인 함수 호출
 * - Spring 트랜잭션과 자연스러운 연동
 * - 예측 가능한 리소스 사용
 *
 * 사용 예시:
 * ```kotlin
 * @Component
 * class UserCreatedEventHandler : EventConsumerPort<UserCreatedEvent> {
 *     override val eventType = UserCreatedEvent::class
 *     
 *     override fun consume(event: UserCreatedEvent) {
 *         // 단순하고 직관적인 JPA 호출
 *         userRepository.save(createUser(event))
 *     }
 * }
 * ```
 *
 * @param T 처리할 이벤트 타입 (Event의 하위 타입)
 */
interface EventConsumerPort<T : Event> {

    /**
     * 처리할 이벤트 타입
     */
    val eventType: KClass<T>

    /**
     * 이벤트 소비 (처리) - Blocking 함수
     * 
     * JPA, JDBC 등 blocking I/O를 자연스럽게 사용할 수 있습니다.
     * 각 이벤트는 별도 스레드에서 실행됩니다.
     *
     * @param event 처리할 이벤트
     */
    fun consume(event: T)

    /**
     * 핸들러 실행 순서 (낮을수록 먼저 실행)
     */
    val order: Int get() = 0

    /**
     * 스레드 풀 설정
     */
    val executorConfig: ExecutorConfig get() = ExecutorConfig.default()

    /**
     * 핸들러 이름 (로깅/디버깅용)
     */
    val handlerName: String get() = javaClass.simpleName

    /**
     * 에러 처리 전략
     */
    val errorHandler: EventErrorHandler get() = EventErrorHandler.default()
}

/**
 * 이벤트 핸들러용 스레드 풀 설정
 */
data class ExecutorConfig(
    val corePoolSize: Int = 5,
    val maxPoolSize: Int = 20,
    val queueCapacity: Int = 100,
    val keepAliveSeconds: Long = 60,
    val threadNamePrefix: String = "event-handler",
    val rejectionPolicy: RejectionPolicy = RejectionPolicy.CALLER_RUNS,
    val allowCoreThreadTimeOut: Boolean = false
) {
    companion object {
        fun default() = ExecutorConfig()
        
        /**
         * 고성능 처리용 설정
         */
        fun highThroughput() = ExecutorConfig(
            corePoolSize = 20,
            maxPoolSize = 100,
            queueCapacity = 1000,
            threadNamePrefix = "high-throughput"
        )
        
        /**
         * 보수적 설정 (DB 연결 풀 보호)
         */
        fun conservative() = ExecutorConfig(
            corePoolSize = 3,
            maxPoolSize = 10,
            queueCapacity = 50,
            rejectionPolicy = RejectionPolicy.CALLER_RUNS
        )
        
        /**
         * JPA 최적화 설정
         */
        fun forJpa(connectionPoolSize: Int) = ExecutorConfig(
            corePoolSize = maxOf(1, connectionPoolSize / 2),
            maxPoolSize = connectionPoolSize,
            queueCapacity = connectionPoolSize * 5,
            threadNamePrefix = "jpa-event"
        )
    }
}

enum class RejectionPolicy {
    /**
     * 큐가 가득 찰 때 호출자 스레드에서 실행
     * 백프레셔 효과로 안정적
     */
    CALLER_RUNS,
    
    /**
     * 오래된 작업을 버리고 새 작업 추가
     */
    DISCARD_OLDEST,
    
    /**
     * 새로운 작업을 버림
     */
    DISCARD,
    
    /**
     * 예외를 발생시킴
     */
    ABORT
}

/**
 * 이벤트 에러 처리 전략
 */
interface EventErrorHandler {
    /**
     * 이벤트 처리 중 발생한 오류 처리
     */
    fun handleError(event: Event, exception: Exception, attempt: Int): ErrorAction
    
    companion object {
        fun default(): EventErrorHandler = DefaultEventErrorHandler()
        fun retrying(maxAttempts: Int = 3): EventErrorHandler = RetryingEventErrorHandler(maxAttempts)
        fun logging(): EventErrorHandler = LoggingEventErrorHandler()
    }
}

enum class ErrorAction {
    RETRY,      // 재시도
    SKIP,       // 건너뛰기
    FAIL        // 실패 처리
}

/**
 * 기본 에러 핸들러 - 로그 남기고 건너뛰기
 */
class DefaultEventErrorHandler : EventErrorHandler {
    override fun handleError(event: Event, exception: Exception, attempt: Int): ErrorAction {
        println("⚠️ Event processing failed: ${event.javaClass.simpleName} on attempt $attempt")
        println("   Error: ${exception.javaClass.simpleName}: ${exception.message}")
        return ErrorAction.SKIP
    }
}

/**
 * 재시도 가능한 에러 핸들러
 */
class RetryingEventErrorHandler(
    private val maxAttempts: Int = 3,
    private val backoffMs: Long = 1000
) : EventErrorHandler {
    
    override fun handleError(event: Event, exception: Exception, attempt: Int): ErrorAction {
        return if (attempt < maxAttempts) {
            println("🔄 Event processing failed, retrying... ($attempt/$maxAttempts): ${event.javaClass.simpleName}")
            println("   Error: ${exception.javaClass.simpleName}: ${exception.message}")
            
            // 간단한 백오프
            if (backoffMs > 0) {
                Thread.sleep(backoffMs * attempt)
            }
            
            ErrorAction.RETRY
        } else {
            println("❌ Event processing finally failed after $maxAttempts attempts: ${event.javaClass.simpleName}")
            println("   Final error: ${exception.javaClass.simpleName}: ${exception.message}")
            ErrorAction.FAIL
        }
    }
}

/**
 * 로깅만 하는 에러 핸들러
 */
class LoggingEventErrorHandler : EventErrorHandler {
    override fun handleError(event: Event, exception: Exception, attempt: Int): ErrorAction {
        println("📝 Event processing error logged: ${event.javaClass.simpleName}")
        println("   Error: ${exception.javaClass.simpleName}: ${exception.message}")
        return ErrorAction.SKIP
    }
}
