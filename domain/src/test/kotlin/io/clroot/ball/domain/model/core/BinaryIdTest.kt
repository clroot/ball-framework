package io.clroot.ball.domain.model.core

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveLength

class BinaryIdTest : FunSpec({
    test("new() should create a new BinaryId with a valid ULID") {
        val binaryId = BinaryId.new()
        binaryId.toString() shouldHaveLength 26
    }

    test("fromString() should create a BinaryId from a valid ULID string") {
        val validUlid = BinaryId.new().toString()
        val result = BinaryId.fromString(validUlid)

        result.shouldBeRight().toString() shouldBe validUlid
    }

    test("fromString() should return InvalidIdError for an invalid ULID string") {
        val invalidUlid = "invalid-ulid"
        val result = BinaryId.fromString(invalidUlid)

        result.shouldBeLeft(InvalidIdError)
    }

    test("fromString() should return InvalidIdError for a ULID with incorrect length") {
        val shortUlid = "12345"
        val result = BinaryId.fromString(shortUlid)

        result.shouldBeLeft(InvalidIdError)
    }

    test("fromString() should return InvalidIdError for a ULID with invalid characters") {
        val invalidCharsUlid =
            "0123456789ABCDEFGHIJKLMNOPQR" // Contains 'I' and 'O' which are not in Crockford's base32
        val result = BinaryId.fromString(invalidCharsUlid)

        result.shouldBeLeft(InvalidIdError)
    }

    test("toString() should return the ULID string") {
        val ulidString = BinaryId.new().toString()
        val binaryId = BinaryId.fromString(ulidString).getOrNull()

        binaryId?.toString() shouldBe ulidString
    }
})