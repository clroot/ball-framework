package io.clroot.ball.domain.exception

/**
 * 도메인 에러 타입 (프로토콜 독립적)
 * 
 * 어댑터 레이어에서 적절한 응답 코드로 매핑
 * - HTTP: 400, 404, 409, 422 등
 * - gRPC: INVALID_ARGUMENT, NOT_FOUND, ALREADY_EXISTS 등
 */
enum class ErrorType {
    /** 잘못된 입력값 (HTTP 400, gRPC INVALID_ARGUMENT) */
    BAD_INPUT,
    
    /** 리소스 없음 (HTTP 404, gRPC NOT_FOUND) */
    NOT_FOUND,
    
    /** 중복/충돌 (HTTP 409, gRPC ALREADY_EXISTS) */
    CONFLICT,
    
    /** 비즈니스 규칙 위반 (HTTP 422, gRPC FAILED_PRECONDITION) */
    UNPROCESSABLE,
    
    /** 전제조건 미충족 (HTTP 412, gRPC FAILED_PRECONDITION) */
    PRECONDITION_FAILED,
    
    /** 사용 불가 (HTTP 410, gRPC UNAVAILABLE) */
    GONE,
    
    /** 외부 시스템 오류 (HTTP 502, gRPC UNAVAILABLE) */
    EXTERNAL_ERROR,
    
    /** 외부 시스템 타임아웃 (HTTP 504, gRPC DEADLINE_EXCEEDED) */
    EXTERNAL_TIMEOUT,
}