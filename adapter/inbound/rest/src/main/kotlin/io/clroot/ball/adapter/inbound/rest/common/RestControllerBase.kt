package io.clroot.ball.adapter.inbound.rest.common

import arrow.core.Either
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

/**
 * 모든 REST 컨트롤러의 기본 클래스
 * 
 * 이 클래스는 REST 컨트롤러에서 공통적으로 사용되는 유틸리티 메서드를 제공합니다.
 */
abstract class RestControllerBase {

    /**
     * Either를 ResponseEntity로 변환
     * 
     * @param either Either 객체
     * @param successStatus 성공 시 HTTP 상태 코드 (기본값: 200 OK)
     * @param errorStatus 실패 시 HTTP 상태 코드 (기본값: 400 Bad Request)
     * @return ResponseEntity 객체
     */
    protected fun <E, A> toResponseEntity(
        either: Either<E, A>,
        successStatus: HttpStatus = HttpStatus.OK,
        errorStatus: HttpStatus = HttpStatus.BAD_REQUEST
    ): ResponseEntity<Any> {
        return either.fold(
            { error -> ResponseEntity.status(errorStatus).body(error) },
            { result -> ResponseEntity.status(successStatus).body(result) }
        )
    }

    /**
     * 성공 응답 생성
     * 
     * @param body 응답 본문
     * @param status HTTP 상태 코드 (기본값: 200 OK)
     * @return ResponseEntity 객체
     */
    protected fun <T> success(
        body: T,
        status: HttpStatus = HttpStatus.OK
    ): ResponseEntity<T> {
        return ResponseEntity.status(status).body(body)
    }

    /**
     * 에러 응답 생성
     * 
     * @param error 에러 객체
     * @param status HTTP 상태 코드 (기본값: 400 Bad Request)
     * @return ResponseEntity 객체
     */
    protected fun <E> error(
        error: E,
        status: HttpStatus = HttpStatus.BAD_REQUEST
    ): ResponseEntity<E> {
        return ResponseEntity.status(status).body(error)
    }
}