package io.clroot.ball.adapter.inbound.rest.streaming

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.IOException
import java.util.concurrent.Executors
import java.util.function.Consumer

/**
 * 스트리밍 응답 유틸리티
 *
 * 이 클래스는 대용량 데이터를 스트리밍하기 위한 유틸리티 메서드를 제공합니다.
 */
object StreamingUtils {

    private val executor = Executors.newCachedThreadPool()

    /**
     * 청크 응답 생성
     *
     * @param producer 데이터 생성 함수
     * @param chunkSize 청크 크기
     * @return ResponseEntity<StreamingResponseBody>
     */
    fun <T> createChunkedResponse(
        producer: () -> Sequence<T>,
        chunkSize: Int = 100
    ): ResponseEntity<StreamingResponseBody> {
        val responseBody = StreamingResponseBody { outputStream ->
            val writer = outputStream.bufferedWriter()
            writer.write("[")

            var first = true
            producer().chunked(chunkSize).forEach { chunk ->
                chunk.forEach { item ->
                    if (!first) {
                        writer.write(",")
                    }
                    first = false
                    writer.write(item.toString())
                    writer.flush()
                }
            }

            writer.write("]")
            writer.flush()
        }

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(responseBody)
    }

    /**
     * SSE 이벤트 스트림 생성
     *
     * @param timeout 타임아웃 (밀리초)
     * @param producer 데이터 생성 함수
     * @return ResponseEntity<SseEmitter>
     */
    fun <T> createSseStream(
        timeout: Long = 30000,
        producer: Consumer<SseEmitter>
    ): ResponseEntity<SseEmitter> {
        val emitter = SseEmitter(timeout)

        executor.execute {
            try {
                producer.accept(emitter)
                emitter.complete()
            } catch (e: Exception) {
                emitter.completeWithError(e)
            }
        }

        return ResponseEntity.ok(emitter)
    }

    /**
     * 비동기 응답 생성
     *
     * @param timeout 타임아웃 (밀리초)
     * @param producer 데이터 생성 함수
     * @return ResponseEntity<ResponseBodyEmitter>
     */
    fun <T> createAsyncResponse(
        timeout: Long = 30000,
        producer: Consumer<ResponseBodyEmitter>
    ): ResponseEntity<ResponseBodyEmitter> {
        val emitter = ResponseBodyEmitter(timeout)

        executor.execute {
            try {
                producer.accept(emitter)
                emitter.complete()
            } catch (e: Exception) {
                emitter.completeWithError(e)
            }
        }

        return ResponseEntity.ok(emitter)
    }

    /**
     * CSV 스트리밍 응답 생성
     *
     * @param headers CSV 헤더
     * @param producer 데이터 생성 함수
     * @param filename 다운로드 파일명
     * @return ResponseEntity<StreamingResponseBody>
     */
    fun <T> createCsvStreamingResponse(
        headers: List<String>,
        producer: () -> Sequence<List<String>>,
        filename: String = "export.csv"
    ): ResponseEntity<StreamingResponseBody> {
        val responseBody = StreamingResponseBody { outputStream ->
            val writer = outputStream.bufferedWriter()

            // 헤더 작성
            writer.write(headers.joinToString(","))
            writer.newLine()

            // 데이터 작성
            producer().forEach { row ->
                writer.write(row.joinToString(","))
                writer.newLine()
                writer.flush()
            }
        }

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv"))
            .header("Content-Disposition", "attachment; filename=\"$filename\"")
            .body(responseBody)
    }
}

/**
 * 스트리밍 응답 예외
 */
class StreamingException(message: String, cause: Throwable? = null) : IOException(message, cause)