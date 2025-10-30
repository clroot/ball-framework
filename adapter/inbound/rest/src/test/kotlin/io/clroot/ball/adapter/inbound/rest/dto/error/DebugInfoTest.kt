package io.clroot.ball.adapter.inbound.rest.dto.error

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.clroot.ball.adapter.inbound.rest.support.DebugInfo
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class DebugInfoTest :
    DescribeSpec({

        val objectMapper =
            ObjectMapper().apply {
                registerModule(KotlinModule.Builder().build())
            }

        describe("DebugInfo") {

            describe("기본 생성") {

                it("모든 필드를 null로 기본 생성할 수 있어야 한다") {
                    // when
                    val debugInfo = DebugInfo()

                    // then
                    debugInfo.path shouldBe null
                    debugInfo.method shouldBe null
                    debugInfo.exceptionType shouldBe null
                    debugInfo.stackTrace shouldBe null
                    debugInfo.location shouldBe null
                }

                it("개별 필드를 지정해서 생성할 수 있어야 한다") {
                    // given
                    val path = "/api/users"
                    val method = "POST"
                    val exceptionType = "ValidationException"
                    val stackTrace = "Full stack trace..."
                    val location = "UserController.createUser:45"

                    // when
                    val debugInfo =
                        DebugInfo(
                            path = path,
                            method = method,
                            exceptionType = exceptionType,
                            stackTrace = stackTrace,
                            location = location,
                        )

                    // then
                    debugInfo.path shouldBe path
                    debugInfo.method shouldBe method
                    debugInfo.exceptionType shouldBe exceptionType
                    debugInfo.stackTrace shouldBe stackTrace
                    debugInfo.location shouldBe location
                }

                it("일부 필드만 지정해서 생성할 수 있어야 한다") {
                    // given
                    val path = "/api/test"
                    val method = "GET"

                    // when
                    val debugInfo = DebugInfo(path = path, method = method)

                    // then
                    debugInfo.path shouldBe path
                    debugInfo.method shouldBe method
                    debugInfo.exceptionType shouldBe null
                    debugInfo.stackTrace shouldBe null
                    debugInfo.location shouldBe null
                }
            }

            describe("JSON 직렬화") {

                it("모든 필드가 있는 경우 JSON으로 직렬화되어야 한다") {
                    // given
                    val debugInfo =
                        DebugInfo(
                            path = "/api/users/123",
                            method = "DELETE",
                            exceptionType = "BusinessRuleViolationException",
                            stackTrace = "com.example.UserService.deleteUser:123\n\tat com.example.UserController.delete:45",
                            location = "UserService.deleteUser:123",
                        )

                    // when
                    val json = objectMapper.writeValueAsString(debugInfo)

                    // then
                    json shouldContain "\"path\":\"/api/users/123\""
                    json shouldContain "\"method\":\"DELETE\""
                    json shouldContain "\"exceptionType\":\"BusinessRuleViolationException\""
                    json shouldContain "\"stackTrace\""
                    json shouldContain "\"location\":\"UserService.deleteUser:123\""
                }

                it("null 필드는 JSON에서 제외되어야 한다") {
                    // given
                    val debugInfo =
                        DebugInfo(
                            path = "/api/test",
                            method = "GET",
                            // exceptionType, stackTrace, location은 null
                        )

                    // when
                    val json = objectMapper.writeValueAsString(debugInfo)

                    // then
                    json shouldContain "\"path\":\"/api/test\""
                    json shouldContain "\"method\":\"GET\""
                    json shouldNotContain "\"exceptionType\""
                    json shouldNotContain "\"stackTrace\""
                    json shouldNotContain "\"location\""
                }

                it("모든 필드가 null인 경우 빈 객체로 직렬화되어야 한다") {
                    // given
                    val debugInfo = DebugInfo()

                    // when
                    val json = objectMapper.writeValueAsString(debugInfo)

                    // then
                    json shouldBe "{}"
                }
            }

            describe("JSON 역직렬화") {

                it("JSON에서 DebugInfo로 역직렬화할 수 있어야 한다") {
                    // given
                    val json =
                        """
                        {
                            "path": "/api/products",
                            "method": "PUT",
                            "exceptionType": "InvalidIdException",
                            "stackTrace": "Full stack trace here...",
                            "location": "ProductService.update:89"
                        }
                        """.trimIndent()

                    // when
                    val debugInfo = objectMapper.readValue(json, DebugInfo::class.java)

                    // then
                    debugInfo.path shouldBe "/api/products"
                    debugInfo.method shouldBe "PUT"
                    debugInfo.exceptionType shouldBe "InvalidIdException"
                    debugInfo.stackTrace shouldBe "Full stack trace here..."
                    debugInfo.location shouldBe "ProductService.update:89"
                }

                it("일부 필드만 있는 JSON도 역직렬화할 수 있어야 한다") {
                    // given
                    val json =
                        """
                        {
                            "path": "/api/health",
                            "method": "GET"
                        }
                        """.trimIndent()

                    // when
                    val debugInfo = objectMapper.readValue(json, DebugInfo::class.java)

                    // then
                    debugInfo.path shouldBe "/api/health"
                    debugInfo.method shouldBe "GET"
                    debugInfo.exceptionType shouldBe null
                    debugInfo.stackTrace shouldBe null
                    debugInfo.location shouldBe null
                }

                it("빈 JSON 객체도 역직렬화할 수 있어야 한다") {
                    // given
                    val json = "{}"

                    // when
                    val debugInfo = objectMapper.readValue(json, DebugInfo::class.java)

                    // then
                    debugInfo.path shouldBe null
                    debugInfo.method shouldBe null
                    debugInfo.exceptionType shouldBe null
                    debugInfo.stackTrace shouldBe null
                    debugInfo.location shouldBe null
                }
            }

            describe("데이터 클래스 기능") {

                it("copy 함수가 올바르게 동작해야 한다") {
                    // given
                    val original =
                        DebugInfo(
                            path = "/original",
                            method = "POST",
                            exceptionType = "OriginalException",
                        )

                    // when
                    val copied =
                        original.copy(
                            path = "/updated",
                            location = "UpdatedLocation:123",
                        )

                    // then
                    copied.path shouldBe "/updated"
                    copied.method shouldBe original.method
                    copied.exceptionType shouldBe original.exceptionType
                    copied.stackTrace shouldBe original.stackTrace
                    copied.location shouldBe "UpdatedLocation:123"
                }

                it("equality가 올바르게 동작해야 한다") {
                    // given
                    val debugInfo1 = DebugInfo(path = "/test", method = "GET")
                    val debugInfo2 = DebugInfo(path = "/test", method = "GET")
                    val debugInfo3 = DebugInfo(path = "/test", method = "POST")

                    // then
                    (debugInfo1 == debugInfo2) shouldBe true
                    (debugInfo1 == debugInfo3) shouldBe false
                }

                it("toString이 올바르게 동작해야 한다") {
                    // given
                    val debugInfo =
                        DebugInfo(
                            path = "/api/test",
                            method = "POST",
                            exceptionType = "TestException",
                        )

                    // when
                    val toString = debugInfo.toString()

                    // then
                    toString shouldContain "path=/api/test"
                    toString shouldContain "method=POST"
                    toString shouldContain "exceptionType=TestException"
                }
            }

            describe("실제 사용 시나리오") {

                it("HTTP 요청 컨텍스트 정보를 담을 수 있어야 한다") {
                    // given
                    val debugInfo =
                        DebugInfo(
                            path = "/api/users/123/orders",
                            method = "POST",
                        )

                    // when
                    val json = objectMapper.writeValueAsString(debugInfo)

                    // then
                    json shouldContain "/api/users/123/orders"
                    json shouldContain "POST"
                }

                it("예외 정보를 담을 수 있어야 한다") {
                    // given
                    val debugInfo =
                        DebugInfo(
                            exceptionType = "jakarta.validation.ConstraintViolationException",
                            location = "UserService.validateUser:234",
                        )

                    // when
                    val json = objectMapper.writeValueAsString(debugInfo)

                    // then
                    json shouldContain "jakarta.validation.ConstraintViolationException"
                    json shouldContain "UserService.validateUser:234"
                }

                it("전체 디버그 컨텍스트를 담을 수 있어야 한다") {
                    // given
                    val stackTrace =
                        """
                        io.clroot.ball.domain.exception.ValidationException: Email format is invalid
                            at io.clroot.ball.domain.model.user.Email.<init>(Email.kt:15)
                            at io.clroot.ball.application.user.CreateUserUseCase.execute(CreateUserUseCase.kt:25)
                            at io.clroot.ball.adapter.inbound.rest.UserController.createUser(UserController.kt:45)
                        """.trimIndent()

                    val debugInfo =
                        DebugInfo(
                            path = "/api/users",
                            method = "POST",
                            exceptionType = "ValidationException",
                            stackTrace = stackTrace,
                            location = "Email.<init>:15",
                        )

                    // when
                    val json = objectMapper.writeValueAsString(debugInfo)
                    val deserialized = objectMapper.readValue(json, DebugInfo::class.java)

                    // then
                    deserialized.path shouldBe "/api/users"
                    deserialized.method shouldBe "POST"
                    deserialized.exceptionType shouldBe "ValidationException"
                    deserialized.stackTrace shouldBe stackTrace
                    deserialized.location shouldBe "Email.<init>:15"
                }
            }
        }
    })
