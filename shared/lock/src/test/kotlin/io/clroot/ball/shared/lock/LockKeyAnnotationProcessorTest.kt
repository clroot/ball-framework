package io.clroot.ball.shared.lock

import io.clroot.ball.shared.lock.exception.LockKeyResolutionException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

/**
 * @LockKey 어노테이션 기반 락 키 해석기 테스트
 */
class LockKeyAnnotationProcessorTest : BehaviorSpec({

    val processor = LockKeyAnnotationProcessor()

    given("@LockKey 어노테이션이 적용된 메서드") {

        `when`("단순한 문자열 파라미터를 사용하는 경우") {
            val method =
                TestService::class.java.getDeclaredMethod("simpleStringLock", String::class.java, String::class.java)
            val annotation = method.getAnnotation(DistributedLock::class.java)
            val args = arrayOf<Any?>("user123", "update")
            val paramNames = arrayOf("userId", "operation")

            then("올바른 락 키가 생성되어야 한다") {
                annotation?.let {
                    val result = processor.resolveKey(it, method, args, paramNames)
                    result shouldBe "user-user123-update"
                }
            }
        }

        `when`("객체의 프로퍼티를 사용하는 경우") {
            val method = TestService::class.java.getDeclaredMethod(
                "objectPropertyLock", TestUser::class.java, String::class.java
            )
            val annotation = method.getAnnotation(DistributedLock::class.java)
            val user = TestUser("user456", "john@example.com")
            val args = arrayOf<Any?>(user, "metadata")
            val paramNames = arrayOf("user", "data")

            then("객체의 id 프로퍼티가 추출되어야 한다") {
                annotation?.let {
                    val result = processor.resolveKey(it, method, args, paramNames)
                    result shouldBe "user-user456"
                }
            }
        }

        `when`("중첩된 프로퍼티를 사용하는 경우") {
            val method = TestService::class.java.getDeclaredMethod(
                "nestedPropertyLock", TestOrder::class.java, TestOrder::class.java
            )
            val annotation = method.getAnnotation(DistributedLock::class.java)
            val user = TestUser("user789", "jane@example.com")
            val order = TestOrder("order123", user)
            val args = arrayOf<Any?>(order, order)
            val paramNames = arrayOf("order", "sameOrder")

            then("중첩된 프로퍼티가 올바르게 추출되어야 한다") {
                annotation?.let {
                    val result = processor.resolveKey(it, method, args, paramNames)
                    result shouldBe "order-order123-user789"
                }
            }
        }

        `when`("null 값이 포함된 경우") {
            val method = TestService::class.java.getDeclaredMethod(
                "nullValueLock", String::class.java, String::class.javaObjectType
            )
            val annotation = method.getAnnotation(DistributedLock::class.java)
            val args = arrayOf<Any?>("user123", null)
            val paramNames = arrayOf("userId", "operation")

            then("기본 null 값이 사용되어야 한다") {
                annotation?.let {
                    val result = processor.resolveKey(it, method, args, paramNames)
                    result shouldBe "user-user123-null"
                }
            }
        }

        `when`("커스텀 null 값이 설정된 경우") {
            val method = TestService::class.java.getDeclaredMethod(
                "customNullValueLock", String::class.java, String::class.javaObjectType
            )
            val annotation = method.getAnnotation(DistributedLock::class.java)
            val args = arrayOf<Any?>("user123", null)
            val paramNames = arrayOf("userId", "operation")

            then("커스텀 null 값이 사용되어야 한다") {
                annotation?.let {
                    val result = processor.resolveKey(it, method, args, paramNames)
                    result shouldBe "user-user123-UNKNOWN"
                }
            }
        }

        `when`("치환되지 않은 플레이스홀더가 있는 경우") {
            val method = TestService::class.java.getDeclaredMethod("unresolvedPlaceholderLock", String::class.java)
            val annotation = method.getAnnotation(DistributedLock::class.java)
            val args = arrayOf<Any?>("user123")
            val paramNames = arrayOf("userId")

            then("LockKeyResolutionException이 발생해야 한다") {
                annotation?.let {
                    shouldThrow<LockKeyResolutionException> {
                        processor.resolveKey(it, method, args, paramNames)
                    }
                }
            }
        }

        `when`("존재하지 않는 프로퍼티에 접근하는 경우") {
            val method = TestService::class.java.getDeclaredMethod("invalidPropertyLock", TestUser::class.java)
            val annotation = method.getAnnotation(DistributedLock::class.java)
            val user = TestUser("user123", "test@example.com")
            val args = arrayOf<Any?>(user)
            val paramNames = arrayOf("user")

            then("커스텀 nullValue가 사용되어야 한다") {
                annotation?.let {
                    val result = processor.resolveKey(it, method, args, paramNames)
                    result shouldBe "user-MISSING"  // nonExistent 프로퍼티가 커스텀 nullValue로 처리됨
                }
            }
        }
    }
})

// 테스트용 서비스 클래스
class TestService {

    @DistributedLock(key = "user-{userId}-{operation}")
    fun simpleStringLock(@LockKey("userId") id: String, @LockKey("operation") op: String) {
        // 단순한 문자열 파라미터 테스트
    }

    @DistributedLock(key = "user-{id}")
    fun objectPropertyLock(@LockKey(value = "id", property = "id") user: TestUser, data: String) {
        // 객체 프로퍼티 추출 테스트
    }

    @DistributedLock(key = "order-{orderId}-{userId}")
    fun nestedPropertyLock(
        @LockKey(value = "orderId", property = "id") order: TestOrder,
        @LockKey(value = "userId", property = "user.id") sameOrder: TestOrder
    ) {
        // 중첩 프로퍼티 테스트
    }

    @DistributedLock(key = "user-{userId}-{operation}")
    fun nullValueLock(@LockKey("userId") id: String, @LockKey("operation") op: String?) {
        // null 값 처리 테스트
    }

    @DistributedLock(key = "user-{userId}-{operation}")
    fun customNullValueLock(
        @LockKey("userId") id: String, @LockKey(value = "operation", nullValue = "UNKNOWN") op: String?
    ) {
        // 커스텀 null 값 테스트
    }

    @DistributedLock(key = "user-{userId}-{missingKey}")
    fun unresolvedPlaceholderLock(@LockKey("userId") id: String) {
        // 치환되지 않은 플레이스홀더 테스트
    }

    @DistributedLock(key = "user-{nonExistentProperty}")
    fun invalidPropertyLock(
        @LockKey(
            value = "nonExistentProperty", property = "nonExistent", nullValue = "MISSING"
        ) user: TestUser
    ) {
        // 존재하지 않는 프로퍼티 테스트
    }
}

// 테스트용 데이터 클래스
data class TestUser(
    val id: String, val email: String
)

data class TestOrder(
    val id: String, val user: TestUser
)
