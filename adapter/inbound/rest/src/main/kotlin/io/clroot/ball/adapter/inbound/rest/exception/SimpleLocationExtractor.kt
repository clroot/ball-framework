package io.clroot.ball.adapter.inbound.rest.exception

object SimpleLocationExtractor {
    private val frameworkPackages = setOf(
        // Java/Kotlin 기본
        "java.", "kotlin.", "jdk.", "sun.", "com.sun.",

        // Spring
        "org.springframework.",

        // ball-framework 자체
        "io.clroot.ball.",

        // 서버
        "org.apache.catalina.", "org.apache.tomcat.", "org.eclipse.jetty.",

        // 테스트
        "org.junit.", "org.mockito.", "org.testcontainers."
    )

    fun extractLocation(exception: Throwable): String? {
        return exception.stackTrace
            .firstOrNull { !isFrameworkCode(it.className) }
            ?.let { formatLocation(it) }
    }

    private fun isFrameworkCode(className: String): Boolean {
        return frameworkPackages.any { className.startsWith(it) }
    }

    private fun formatLocation(element: StackTraceElement): String {
        val shortClassName = element.className.substringAfterLast('.')
        return "$shortClassName.${element.methodName}:${element.lineNumber}"
    }
}