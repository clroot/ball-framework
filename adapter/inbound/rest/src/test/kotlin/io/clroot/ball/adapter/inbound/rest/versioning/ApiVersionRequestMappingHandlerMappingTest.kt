package io.clroot.ball.adapter.inbound.rest.versioning

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition
import org.springframework.web.servlet.mvc.method.RequestMappingInfo

class ApiVersionRequestMappingHandlerMappingTest : FunSpec({

    test("should replace {version} placeholder with actual version from ApiVersion annotation") {
        // Given
        val handlerMapping = ApiVersionRequestMappingHandlerMapping()
        val apiVersion = ApiVersion("2")
        val originalInfo = createMockRequestMappingInfo("/test/{version}/resource")

        // When
        val createApiVersionInfoMethod = ApiVersionRequestMappingHandlerMapping::class.java
            .getDeclaredMethod("createApiVersionInfo", ApiVersion::class.java, RequestMappingInfo::class.java)
            .apply { isAccessible = true }

        val result = createApiVersionInfoMethod.invoke(handlerMapping, apiVersion, originalInfo) as RequestMappingInfo

        // Then
        result.patternsCondition!!.patterns.first() shouldBe "/test/2/resource"
    }

    test("should find ApiVersion annotation on method") {
        // Given
        val method = TestController::class.java.getMethod("methodWithApiVersion")

        // When
        val annotation = method.getAnnotation(ApiVersion::class.java)

        // Then
        annotation shouldBe kotlin.test.assertNotNull(annotation)
        annotation.value shouldBe "2"
    }

    test("should find ApiVersion annotation on class") {
        // Given
        val clazz = VersionedController::class.java

        // When
        val annotation = clazz.getAnnotation(ApiVersion::class.java)

        // Then
        annotation shouldBe kotlin.test.assertNotNull(annotation)
        annotation.value shouldBe "3"
    }

}) {
    // Test classes
    class TestController {
        @ApiVersion("2")
        @RequestMapping("/test/{version}/resource")
        fun methodWithApiVersion(): String = "test"

        fun methodWithoutMapping(): String = "test"
    }

    @ApiVersion("3")
    class VersionedController {
        @RequestMapping("/test/{version}/resource")
        fun methodWithoutApiVersion(): String = "test"
    }

    // Helper function to create mock RequestMappingInfo
    companion object {
        private fun createMockRequestMappingInfo(pattern: String): RequestMappingInfo {
            val patternsCondition = PatternsRequestCondition(pattern)
            val methodsCondition = RequestMethodsRequestCondition()

            return RequestMappingInfo(
                patternsCondition,
                methodsCondition,
                null,
                null,
                null,
                null,
                null
            )
        }
    }
}
