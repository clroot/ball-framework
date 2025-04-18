package io.clroot.ball.adapter.inbound.rest.versioning

import org.springframework.web.bind.annotation.RequestMapping
import java.lang.annotation.Inherited

/**
 * API 버전 어노테이션
 * 
 * 이 어노테이션은 컨트롤러 클래스나 메서드에 API 버전을 지정하는 데 사용됩니다.
 * 
 * @property value API 버전 (예: "1", "2" 등)
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
@RequestMapping("/api/v{version}")
annotation class ApiVersion(val value: String = "1")