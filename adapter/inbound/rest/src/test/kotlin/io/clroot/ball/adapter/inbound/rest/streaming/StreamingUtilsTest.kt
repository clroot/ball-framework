package io.clroot.ball.adapter.inbound.rest.streaming

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.http.MediaType
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.ByteArrayOutputStream
import java.util.function.Consumer

class StreamingUtilsTest : FunSpec({

    test("createChunkedResponse should create a streaming response with JSON array") {
        // Given
        val data = sequenceOf("item1", "item2", "item3")

        // When
        val response = StreamingUtils.createChunkedResponse<String>(
            producer = { data }
        )

        // Then
        response.statusCode.is2xxSuccessful shouldBe true
        response.headers.contentType shouldBe MediaType.APPLICATION_JSON

        // Test the streaming response body
        val outputStream = ByteArrayOutputStream()
        val responseBody = response.body as StreamingResponseBody
        responseBody.writeTo(outputStream)

        val result = outputStream.toString()
        result shouldBe "[item1,item2,item3]"
    }

    test("createSseStream should create an SSE emitter") {
        // Given
        val emitterSlot = slot<SseEmitter>()
        val producer = mockk<Consumer<SseEmitter>>()
        every { producer.accept(capture(emitterSlot)) } answers {
            // Simulate sending events
            val emitter = emitterSlot.captured
            emitter.send("event1")
            emitter.send("event2")
            emitter.complete()
        }

        // When
        val response = StreamingUtils.createSseStream<String>(producer = producer)

        // Then
        response.statusCode.is2xxSuccessful shouldBe true
        response.body.shouldBeInstanceOf<SseEmitter>()

        // Verify the producer was called with the emitter
        verify { producer.accept(any()) }
    }

    test("createAsyncResponse should create a response body emitter") {
        // Given
        val emitterSlot = slot<ResponseBodyEmitter>()
        val producer = mockk<Consumer<ResponseBodyEmitter>>()
        every { producer.accept(capture(emitterSlot)) } answers {
            // Simulate sending data
            val emitter = emitterSlot.captured
            emitter.send("data1")
            emitter.send("data2")
            emitter.complete()
        }

        // When
        val response = StreamingUtils.createAsyncResponse<String>(producer = producer)

        // Then
        response.statusCode.is2xxSuccessful shouldBe true
        response.body.shouldBeInstanceOf<ResponseBodyEmitter>()

        // Verify the producer was called with the emitter
        verify { producer.accept(any()) }
    }

    test("createCsvStreamingResponse should create a CSV streaming response") {
        // Given
        val headers = listOf("Name", "Age", "Email")
        val data = sequenceOf(
            listOf("John", "30", "john@example.com"),
            listOf("Jane", "25", "jane@example.com")
        )

        // When
        val response = StreamingUtils.createCsvStreamingResponse<List<String>>(headers, { data })

        // Then
        response.statusCode.is2xxSuccessful shouldBe true
        response.headers.contentType?.toString() shouldBe "text/csv"
        response.headers.getFirst("Content-Disposition") shouldBe "attachment; filename=\"export.csv\""

        // Test the streaming response body
        val outputStream = ByteArrayOutputStream()
        val responseBody = response.body as StreamingResponseBody
        responseBody.writeTo(outputStream)

        val result = outputStream.toString()
        val expectedCsv = "Name,Age,Email\nJohn,30,john@example.com\nJane,25,jane@example.com\n"
        result shouldBe expectedCsv
    }

    test("createCsvStreamingResponse should use custom filename") {
        // Given
        val headers = listOf("Name", "Age")
        val data = sequenceOf(listOf("John", "30"))
        val filename = "users.csv"

        // When
        val response = StreamingUtils.createCsvStreamingResponse<List<String>>(headers, { data }, filename)

        // Then
        response.headers.getFirst("Content-Disposition") shouldBe "attachment; filename=\"users.csv\""
    }
})
