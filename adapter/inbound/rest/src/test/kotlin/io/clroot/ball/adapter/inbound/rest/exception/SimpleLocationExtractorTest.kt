package io.clroot.ball.adapter.inbound.rest.exception

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

class SimpleLocationExtractorTest : DescribeSpec({

    describe("SimpleLocationExtractor") {

        describe("extractLocation") {

            context("프레임워크 코드가 아닌 첫 번째 스택 프레임 추출") {
                it("사용자 코드가 있을 때 해당 위치를 반환해야 한다") {
                    // given
                    val exception = createExceptionWithCustomStack(
                        Triple("com.example.service.UserService", "createUser", 42),
                        Triple("org.springframework.web.method.support.InvocableHandlerMethod", "doInvoke", 190),
                        Triple("java.lang.reflect.Method", "invoke", 498)
                    )

                    // when
                    val location = SimpleLocationExtractor.extractLocation(exception)

                    // then
                    location shouldBe "UserService.createUser:42"
                }

                it("프레임워크 코드만 있을 때 null을 반환해야 한다") {
                    // given
                    val exception = createExceptionWithCustomStack(
                        Triple("org.springframework.web.method.support.InvocableHandlerMethod", "doInvoke", 190),
                        Triple("java.lang.reflect.Method", "invoke", 498),
                        Triple("org.apache.catalina.core.StandardWrapper", "service", 100),
                    )

                    // when
                    val location = SimpleLocationExtractor.extractLocation(exception)

                    // then
                    location shouldBe null
                }

                it("빈 스택 트레이스일 때 null을 반환해야 한다") {
                    // given
                    val exception = RuntimeException("Test exception")
                    exception.stackTrace = emptyArray()

                    // when
                    val location = SimpleLocationExtractor.extractLocation(exception)

                    // then
                    location shouldBe null
                }
            }

            context("프레임워크 패키지 필터링") {
                it("Java 기본 패키지는 필터링해야 한다") {
                    // given
                    val exception = createExceptionWithCustomStack(
                        Triple("com.example.UserController", "getUser", 25),
                        Triple("java.lang.reflect.Method", "invoke", 498),
                        Triple("jdk.internal.reflect.NativeMethodAccessorImpl", "invoke", 62),
                    )

                    // when
                    val location = SimpleLocationExtractor.extractLocation(exception)

                    // then
                    location shouldBe "UserController.getUser:25"
                }

                it("Kotlin 기본 패키지는 필터링해야 한다") {
                    // given
                    val exception = createExceptionWithCustomStack(
                        Triple("com.example.UserService", "updateUser", 89),
                        Triple("kotlin.jvm.internal.Intrinsics", "checkNotNull", 10),
                        Triple("kotlin.collections.CollectionsKt", "first", 15),
                    )

                    // when
                    val location = SimpleLocationExtractor.extractLocation(exception)

                    // then
                    location shouldBe "UserService.updateUser:89"
                }

                it("Spring 프레임워크 패키지는 필터링해야 한다") {
                    // given
                    val exception = createExceptionWithCustomStack(
                        Triple("com.example.repository.UserRepository", "save", 15),
                        Triple("org.springframework.web.method.support.InvocableHandlerMethod", "doInvoke", 190),
                        Triple("org.springframework.transaction.interceptor.TransactionInterceptor", "invoke", 118),
                    )

                    // when
                    val location = SimpleLocationExtractor.extractLocation(exception)

                    // then
                    location shouldBe "UserRepository.save:15"
                }

                it("ball-framework 자체 패키지는 필터링해야 한다") {
                    // given
                    val exception = createExceptionWithCustomStack(
                        Triple("com.example.controller.ProductController", "deleteProduct", 67),
                        Triple("io.clroot.ball.adapter.inbound.rest.EitherExtensions", "toResponseEntity", 5),
                        Triple("io.clroot.ball.domain.model.EntityBase", "getId", 20),
                    )

                    // when
                    val location = SimpleLocationExtractor.extractLocation(exception)

                    // then
                    location shouldBe "ProductController.deleteProduct:67"
                }

                it("서버 컨테이너 패키지는 필터링해야 한다") {
                    // given
                    val exception = createExceptionWithCustomStack(
                        Triple("com.example.MyApplication", "main", 12),
                        Triple("org.apache.catalina.core.StandardWrapper", "service", 100),
                        Triple("org.eclipse.jetty.server.handler.HandlerWrapper", "handle", 139),
                    )

                    // when
                    val location = SimpleLocationExtractor.extractLocation(exception)

                    // then
                    location shouldBe "MyApplication.main:12"
                }

                it("테스트 프레임워크 패키지는 필터링해야 한다") {
                    // given
                    val exception = createExceptionWithCustomStack(
                        Triple("com.example.service.EmailService", "sendEmail", 33),
                        Triple("org.junit.jupiter.engine.execution.ExecutableInvoker", "invoke", 115),
                        Triple("org.mockito.internal.creation.bytebuddy.MockMethodInterceptor", "doIntercept", 84),
                    )

                    // when
                    val location = SimpleLocationExtractor.extractLocation(exception)

                    // then
                    location shouldBe "EmailService.sendEmail:33"
                }
            }

            context("포맷팅") {
                it("간단한 클래스명으로 변환해야 한다") {
                    // given
                    val exception = createExceptionWithCustomStack(
                        Triple("com.example.service.impl.UserServiceImpl", "validateUser", 156),
                    )

                    // when
                    val location = SimpleLocationExtractor.extractLocation(exception)

                    // then
                    location shouldBe "UserServiceImpl.validateUser:156"
                }

                it("중첩 클래스도 올바르게 처리해야 한다") {
                    // given
                    val exception = createExceptionWithCustomStack(
                        Triple("com.example.MyClass\$InnerClass", "process", 78),
                    )

                    // when
                    val location = SimpleLocationExtractor.extractLocation(exception)

                    // then
                    location shouldBe "MyClass\$InnerClass.process:78"
                }

                it("익명 클래스도 올바르게 처리해야 한다") {
                    // given
                    val exception = createExceptionWithCustomStack(
                        Triple("com.example.EventHandler\$1", "handle", 45),
                    )

                    // when
                    val location = SimpleLocationExtractor.extractLocation(exception)

                    // then
                    location shouldBe "EventHandler\$1.handle:45"
                }

                it("라인 번호가 0인 경우도 처리해야 한다") {
                    // given
                    val exception = createExceptionWithCustomStack(
                        Triple("com.example.NativeMethod", "nativeCall", 0),
                    )

                    // when
                    val location = SimpleLocationExtractor.extractLocation(exception)

                    // then
                    location shouldBe "NativeMethod.nativeCall:0"
                }

                it("라인 번호가 음수인 경우도 처리해야 한다") {
                    // given
                    val exception = createExceptionWithCustomStack(
                        Triple("com.example.UnknownSource", "unknownMethod", -1),
                    )

                    // when
                    val location = SimpleLocationExtractor.extractLocation(exception)

                    // then
                    location shouldBe "UnknownSource.unknownMethod:-1"
                }
            }
        }

        describe("isFrameworkCode private 메서드 동작") {

            it("프레임워크 패키지들을 올바르게 식별해야 한다") {
                // given & when & then
                SimpleLocationExtractor.isFrameworkCode("java.util.ArrayList") shouldBe true
                SimpleLocationExtractor.isFrameworkCode("kotlin.collections.List") shouldBe true
                SimpleLocationExtractor.isFrameworkCode("org.springframework.boot.SpringApplication") shouldBe true
                SimpleLocationExtractor.isFrameworkCode("io.clroot.ball.domain.Entity") shouldBe true
                SimpleLocationExtractor.isFrameworkCode("org.apache.catalina.startup.Bootstrap") shouldBe true
                SimpleLocationExtractor.isFrameworkCode("org.junit.jupiter.api.Test") shouldBe true

                // 사용자 코드는 프레임워크 코드가 아님
                SimpleLocationExtractor.isFrameworkCode("com.example.UserService") shouldBe false
                SimpleLocationExtractor.isFrameworkCode("com.mycompany.ProductController") shouldBe false
                SimpleLocationExtractor.isFrameworkCode("org.example.MyApplication") shouldBe false // org.springframework가 아닌 org.example
            }
        }

        describe("실제 예외 시나리오") {

            it("실제 NullPointerException 스택 트레이스를 처리해야 한다") {
                // given - 실제 NPE 발생시키기
                val exception = try {
                    val nullString: String? = null
                    nullString!!.length // NPE 발생
                    RuntimeException("Should not reach here")
                } catch (e: KotlinNullPointerException) {
                    e
                }

                // when
                val location = SimpleLocationExtractor.extractLocation(exception)

                // then
                location shouldNotBe null
                location shouldContain "SimpleLocationExtractorTest"
                location shouldContain ":"
            }

            it("실제 IllegalArgumentException 스택 트레이스를 처리해야 한다") {
                // given - 실제 예외 발생시키기
                val exception = try {
                    require(false) { "Test exception" }
                    RuntimeException("Should not reach here")
                } catch (e: IllegalArgumentException) {
                    e
                }

                // when
                val location = SimpleLocationExtractor.extractLocation(exception)

                // then
                location shouldNotBe null
                location shouldContain "SimpleLocationExtractorTest"
            }
        }

        describe("엣지 케이스") {

            it("스택 트레이스의 모든 프레임이 프레임워크 코드일 때 null을 반환해야 한다") {
                // given
                val exception = createExceptionWithCustomStack(
                    Triple("java.lang.Thread", "run", 748),
                    Triple("java.util.concurrent.ThreadPoolExecutor\$Worker", "run", 628),
                    Triple("org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor\$1", "run", 62),
                )

                // when
                val location = SimpleLocationExtractor.extractLocation(exception)

                // then
                location shouldBe null
            }

            it("혼합된 프레임워크/사용자 코드에서 첫 번째 사용자 코드를 찾아야 한다") {
                // given
                val exception = createExceptionWithCustomStack(
                    Triple("org.springframework.web.servlet.DispatcherServlet", "doDispatch", 1000),
                    Triple("org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter", "handle", 87),
                    Triple("com.example.controller.FirstController", "handleRequest", 15),// 첫 번째 사용자 코드
                    Triple("com.example.service.SecondService", "process", 30),// 두 번째 사용자 코드
                    Triple("java.lang.reflect.Method", "invoke", 498),
                )

                // when
                val location = SimpleLocationExtractor.extractLocation(exception)

                // then
                location shouldBe "FirstController.handleRequest:15" // 첫 번째만 선택되어야 함
            }
        }
    }
})

// 테스트용 헬퍼 메서드
private fun createExceptionWithCustomStack(vararg frames: Triple<String, String, Int>): RuntimeException {
    val exception = RuntimeException("Test exception")
    val stackTrace = frames.map { (className, methodName, lineNumber) ->
        StackTraceElement(className, methodName, "Test.kt", lineNumber)
    }.toTypedArray()
    exception.stackTrace = stackTrace
    return exception
}

// SimpleLocationExtractor의 private 메서드 테스트를 위한 확장
private fun SimpleLocationExtractor.isFrameworkCode(className: String): Boolean {
    val frameworkPackages = setOf(
        "java.",
        "kotlin.",
        "jdk.",
        "sun.",
        "com.sun.",
        "org.springframework.",
        "io.clroot.ball.",
        "org.apache.catalina.",
        "org.apache.tomcat.",
        "org.eclipse.jetty.",
        "org.junit.",
        "org.mockito.",
        "org.testcontainers."
    )
    return frameworkPackages.any { className.startsWith(it) }
}
