package io.clroot.ball.adapter.inbound.rest.exception

object ErrorCodes {
    // 도메인 에러
    const val VALIDATION_FAILED = "VALIDATION_FAILED"
    const val BUSINESS_RULE_VIOLATION = "BUSINESS_RULE_VIOLATION"
    const val INVALID_ID = "INVALID_ID"
    const val PRECONDITION_FAILED = "PRECONDITION_FAILED"
    const val RESOURCE_GONE = "RESOURCE_GONE"

    // 시스템 에러
    const val INTERNAL_ERROR = "INTERNAL_ERROR"
    const val DATABASE_ERROR = "DATABASE_ERROR"
    const val EXTERNAL_SYSTEM_ERROR = "EXTERNAL_SYSTEM_ERROR"
    const val EXTERNAL_TIMEOUT = "EXTERNAL_TIMEOUT"

    // HTTP 상태 관련
    const val NOT_FOUND = "NOT_FOUND"
    const val DUPLICATE_ENTITY = "DUPLICATE_ENTITY"
}
