package io.clroot.ball.adapter.inbound.rest.exception

/**
 * 예외 발생 위치 추출기
 * 
 * 스택 트레이스에서 프레임워크 코드를 제외한 
 * 실제 비즈니스 로직의 예외 발생 위치를 추출합니다.
 */
object ExceptionLocationExtractor {
    
    /**
     * 프레임워크나 라이브러리 코드를 제외할 패키지들
     */
    private val frameworkPackages = setOf(
        // Java/Kotlin 기본
        "java.", "kotlin.", "jdk.", "sun.", "com.sun.",

        // Spring Framework
        "org.springframework.",

        // ball-framework 자체 (필요시 제거 가능)
        "io.clroot.ball.",

        // 서버 구현체
        "org.apache.catalina.", "org.apache.tomcat.", "org.eclipse.jetty.",

        // 테스트 프레임워크
        "org.junit.", "org.mockito.", "org.testcontainers."
    )

    /**
     * 예외에서 비즈니스 로직의 실제 발생 위치를 추출
     * 
     * @param exception 분석할 예외
     * @return 발생 위치 정보 (예: "UserService.createUser:42")
     */
    fun extractLocation(exception: Throwable): String? {
        return exception.stackTrace
            .firstOrNull { !isFrameworkCode(it.className) }
            ?.let { formatLocation(it) }
    }

    /**
     * 프레임워크 코드인지 확인
     */
    private fun isFrameworkCode(className: String): Boolean {
        return frameworkPackages.any { className.startsWith(it) }
    }

    /**
     * 스택 트레이스 요소를 읽기 쉬운 형태로 포맷
     */
    private fun formatLocation(element: StackTraceElement): String {
        val shortClassName = element.className.substringAfterLast('.')
        return "$shortClassName.${element.methodName}:${element.lineNumber}"
    }
}