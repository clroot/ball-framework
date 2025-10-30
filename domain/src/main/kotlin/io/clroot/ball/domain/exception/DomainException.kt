package io.clroot.ball.domain.exception

/**
 * 도메인 계층의 기본 예외.
 *
 * @param message 즉시 확인 가능한 기본 메시지
 * @param errorType 전달 계층(HTTP, gRPC 등)에서 상태/코드 매핑에 활용할 힌트
 * @param errorCode 어댑터/클라이언트에서 기계적으로 처리할 수 있는 식별자 (공백 불허)
 * @param messageKey 다국어/관계자별 맞춤 메시지를 구성할 때 사용할 템플릿 키
 * @param messageArgs 템플릿 메시지를 렌더링할 때 함께 사용할 인자 맵
 * @param metadata 로깅·추적·응답 확장을 위한 구조화된 부가 정보 (예: `entityId`, `systemName`)
 * @param cause 근본 원인 예외
 *
 * 공통 필드 계약을 통해 도메인 예외를 가공하는 각 계층이 일관된 데이터를 활용할 수 있다.
 */
abstract class DomainException(
    message: String,
    val errorType: ErrorType = ErrorType.UNPROCESSABLE,
    val errorCode: String = DomainErrorCodes.DOMAIN_ERROR,
    val messageKey: String? = null,
    val messageArgs: Map<String, Any?> = emptyMap(),
    val metadata: Map<String, Any?> = emptyMap(),
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
    init {
        require(errorCode.isNotBlank()) { "errorCode must not be blank" }
    }
}
