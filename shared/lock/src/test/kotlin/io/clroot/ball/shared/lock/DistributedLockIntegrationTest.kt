package io.clroot.ball.shared.lock

import io.clroot.ball.shared.lock.exception.LockAcquisitionException
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * 분산 락 통합 테스트 (단순한 직접 테스트)
 */
class DistributedLockIntegrationTest : BehaviorSpec({
    
    val lockProvider = LocalLockProvider()
    val annotationProcessor = LockKeyAnnotationProcessor()
    
    given("분산 락 기본 동작") {
        
        `when`("동일한 키로 동시 락 요청") {
            val counter = AtomicInteger(0)
            val results = mutableListOf<String>()
            val latch = CountDownLatch(2)
            
            repeat(2) { index ->
                thread {
                    try {
                        val key = "test-lock"
                        lockProvider.withLock(key, 1000, 500) {
                            Thread.sleep(100) // 작업 시뮬레이션
                            val value = counter.incrementAndGet()
                            synchronized(results) {
                                results.add("Thread-$index: $value")
                            }
                        }
                    } finally {
                        latch.countDown()
                    }
                }
            }
            
            latch.await(5, TimeUnit.SECONDS)
            
            then("순차적으로 실행되어야 한다") {
                counter.get() shouldBe 2
                results.size shouldBe 2
            }
        }
        
        `when`("다른 키로 동시 락 요청") {
            val counter1 = AtomicInteger(0)
            val counter2 = AtomicInteger(0)
            val latch = CountDownLatch(2)
            
            thread {
                try {
                    lockProvider.withLock("key1", 1000, 500) {
                        Thread.sleep(100)
                        counter1.incrementAndGet()
                    }
                } finally {
                    latch.countDown()
                }
            }
            
            thread {
                try {
                    lockProvider.withLock("key2", 1000, 500) {
                        Thread.sleep(100)
                        counter2.incrementAndGet()
                    }
                } finally {
                    latch.countDown()
                }
            }
            
            latch.await(3, TimeUnit.SECONDS)
            
            then("병렬로 실행되어야 한다") {
                counter1.get() shouldBe 1
                counter2.get() shouldBe 1
            }
        }
        
        `when`("락 타임아웃 테스트") {
            val results = mutableListOf<String>()
            val latch = CountDownLatch(2)
            val startLatch = CountDownLatch(1)
            
            // 첫 번째 스레드: 락을 오래 점유
            thread {
                try {
                    startLatch.await()
                    lockProvider.withLock("timeout-test", 2000, 1000) {
                        Thread.sleep(500) // 500ms 점유
                        synchronized(results) { results.add("success-1") }
                    }
                } catch (e: Exception) {
                    synchronized(results) { results.add("fail-1") }
                } finally {
                    latch.countDown()
                }
            }
            
            // 두 번째 스레드: 짧은 타임아웃으로 실패 예상
            thread {
                try {
                    startLatch.await()
                    Thread.sleep(50) // 첫 번째가 락을 먼저 획득하도록
                    lockProvider.withLock("timeout-test", 100, 100) { // 100ms 타임아웃
                        synchronized(results) { results.add("success-2") }
                    }
                } catch (e: LockAcquisitionException) {
                    synchronized(results) { results.add("fail-2") }
                } finally {
                    latch.countDown()
                }
            }
            
            startLatch.countDown()
            latch.await(5, TimeUnit.SECONDS)
            
            then("하나는 성공, 하나는 실패해야 한다") {
                results.size shouldBe 2
                results.contains("success-1") shouldBe true
                results.contains("fail-2") shouldBe true
            }
        }
    }
    
    given("@LockKey 어노테이션 처리") {
        
        `when`("메서드에서 락 키 추출") {
            val method = SimpleTestService::class.java.getDeclaredMethod("testMethod", String::class.java, Int::class.java)
            val annotation = method.getAnnotation(DistributedLock::class.java)
            
            then("올바른 락 키가 생성되어야 한다") {
                val key = annotationProcessor.resolveKey(
                    annotation,
                    method,
                    arrayOf<Any?>("user123", 456),
                    arrayOf("userId", "orderId")
                )
                key shouldBe "user-user123-order-456"
            }
        }
        
        `when`("객체 프로퍼티에서 락 키 추출") {
            val method = SimpleTestService::class.java.getDeclaredMethod("testObjectMethod", SimpleTestUser::class.java)
            val annotation = method.getAnnotation(DistributedLock::class.java)
            val user = SimpleTestUser("user789", "test@example.com")
            
            then("객체의 프로퍼티가 추출되어야 한다") {
                val key = annotationProcessor.resolveKey(
                    annotation,
                    method,
                    arrayOf<Any?>(user),
                    arrayOf("user")
                )
                key shouldBe "user-user789"
            }
        }
    }
})

/**
 * 테스트용 간단한 서비스
 */
class SimpleTestService {
    
    @DistributedLock(key = "user-{userId}-order-{orderId}")
    fun testMethod(@LockKey("userId") userId: String, @LockKey("orderId") orderId: Int): String {
        return "processed"
    }
    
    @DistributedLock(key = "user-{id}")
    fun testObjectMethod(@LockKey(value = "id", property = "id") user: SimpleTestUser): String {
        return "user processed"
    }
}

/**
 * 테스트용 사용자 클래스
 */
data class SimpleTestUser(
    val id: String,
    val email: String
)
