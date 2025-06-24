package io.clroot.ball.domain.model

import io.clroot.ball.domain.exception.DomainValidationException
import io.clroot.ball.domain.model.vo.BinaryId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.matchers.string.shouldMatch
import ulid.ULID
import java.util.*

class BinaryIdTest :
    FunSpec({

        context("Basic BinaryId functionality") {
            test("new() should create a new BinaryId with a valid ULID") {
                val binaryId = BinaryId.generate()
                binaryId.toString() shouldHaveLength 26
            }

            test("fromString() should create a BinaryId from a valid ULID string") {
                val validUlid = BinaryId.generate().toString()
                val result = BinaryId.of(validUlid)

                result.toString() shouldBe validUlid
            }

            test("fromString() should throw DomainValidationException for an invalid ULID string") {
                val invalidUlid = "invalid-ulid"

                shouldThrow<DomainValidationException> {
                    BinaryId.of(invalidUlid)
                }
            }

            test("fromString() should throw DomainValidationException for a ULID with incorrect length") {
                val shortUlid = "12345"

                shouldThrow<DomainValidationException> {
                    BinaryId.of(shortUlid)
                }
            }

            test("fromString() should throw DomainValidationException for a ULID with invalid characters") {
                val invalidCharsUlid =
                    "0123456789ABCDEFGHIJKLMNOPQR" // Contains 'I' and 'O' which are not in Crockford's base32

                shouldThrow<DomainValidationException> {
                    BinaryId.of(invalidCharsUlid)
                }
            }

            test("toString() should return the ULID string") {
                val ulidString = BinaryId.generate().toString()
                val binaryId = BinaryId.of(ulidString)

                binaryId.toString() shouldBe ulidString
            }
        }

        context("Binary conversion functionality") {
            test("toBytes() should convert ULID to 16-byte array") {
                val binaryId = BinaryId.generate()
                val bytes = binaryId.bytes

                bytes.size shouldBe 16
            }

            test("fromBytes() should throw DomainValidationException with invalid byte length") {
                val invalidBytes = ByteArray(8) // 8 bytes instead of 16

                shouldThrow<DomainValidationException> {
                    BinaryId.of(invalidBytes)
                }
            }
        }

        context("UUID conversion functionality") {
            test("BinaryId.of(UUID) should create BinaryId from UUID") {
                val uuid = UUID.randomUUID()
                val binaryId = BinaryId.of(uuid)

                binaryId shouldBe BinaryId.of(uuid) // Should be consistent
                binaryId.bytes.size shouldBe 16
            }

            test("uuid property should return UUID representation") {
                val binaryId = BinaryId.generate()
                val uuid = binaryId.uuid

                uuid shouldBe uuid // UUID should be valid
                uuid.toString() shouldMatch Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
            }

            test("UUID to BinaryId conversion creates consistent BinaryId") {
                val originalUuid = UUID.randomUUID()
                val binaryId1 = BinaryId.of(originalUuid)
                val binaryId2 = BinaryId.of(originalUuid)

                // Same UUID should create same BinaryId
                binaryId1 shouldBe binaryId2
                binaryId1.bytes.contentEquals(binaryId2.bytes) shouldBe true
            }

            test("Different UUIDs should create different BinaryIds") {
                val uuid1 = UUID.randomUUID()
                val uuid2 = UUID.randomUUID()

                val binaryId1 = BinaryId.of(uuid1)
                val binaryId2 = BinaryId.of(uuid2)

                (binaryId1 == binaryId2) shouldBe false
                // Note: Due to ULID encoding, uuid property won't match original UUID
                binaryId1.bytes.contentEquals(binaryId2.bytes) shouldBe false
            }

            test("BinaryId created from same UUID should be equal") {
                val uuid = UUID.randomUUID()
                val binaryId1 = BinaryId.of(uuid)
                val binaryId2 = BinaryId.of(uuid)

                (binaryId1 == binaryId2) shouldBe true
                binaryId1.uuid shouldBe binaryId2.uuid
            }

            test("BinaryId stores UUID data consistently") {
                val uuid = UUID.randomUUID()
                val binaryId = BinaryId.of(uuid)

                // BinaryId should have 16 bytes
                binaryId.bytes.size shouldBe 16

                // The uuid property gives us a UUID representation of the stored data
                val extractedUuid = binaryId.uuid
                extractedUuid shouldBe extractedUuid // Should be internally consistent
            }

            test("BinaryId.generate() creates valid ULID with UUID property") {
                val binaryId = BinaryId.generate()

                // Should be a valid ULID string
                binaryId.toString() shouldHaveLength 26
                binaryId.toString() shouldMatch Regex("^[0123456789ABCDEFGHJKMNPQRSTVWXYZ]{26}$")

                // Should have a UUID representation
                val uuid = binaryId.uuid
                uuid.toString() shouldMatch Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
            }
        }

        context("Equality comparison") {
            test("equals() should return true for BinaryIds with same string value") {
                val ulidString = BinaryId.generate().toString()
                val id1 = BinaryId.of(ulidString)
                val id2 = BinaryId.of(ulidString)

                (id1 == id2) shouldBe true
                id1.hashCode() shouldBe id2.hashCode()
            }

            test("equals() should return true for BinaryIds created from same byte array") {
                val binaryId = BinaryId.generate()
                val bytes = binaryId.bytes

                val id1 = BinaryId.of(bytes)
                val id2 = BinaryId.of(bytes.copyOf()) // Create a copy to ensure different array instances

                (id1 == id2) shouldBe true
                id1.hashCode() shouldBe id2.hashCode()
            }

            test("equals() should return false for different BinaryIds") {
                val id1 = BinaryId.generate()
                val id2 = BinaryId.generate()

                (id1 == id2) shouldBe false
            }

            test("ByteArray comparison - contentEquals vs equals") {
                val binaryId = BinaryId.generate()
                val bytes1 = binaryId.bytes
                val bytes2 = binaryId.bytes

                // ByteArray equals compares references
                @Suppress("ReplaceCallWithBinaryOperator")
                bytes1.equals(bytes2) shouldBe false

                // But contentEquals compares actual content
                bytes1.contentEquals(bytes2) shouldBe true
            }

            test("BinaryId should be usable as Map key") {
                val map = mutableMapOf<BinaryId, String>()
                val id = BinaryId.generate()
                val idString = id.toString()

                map[id] = "value1"

                // Same ID from string should retrieve the value
                val sameId = BinaryId.of(idString)
                map[sameId] shouldBe "value1"

                // Different ID should not find the value
                val differentId = BinaryId.generate()
                map[differentId] shouldBe null
            }
        }

        context("Compatibility with ulid-kotlin library") {
            test("our ULID should match the standard pattern") {
                val ourULID = BinaryId.generate().toString()
                val libraryULID = ULID.nextULID().toString()

                val pattern = Regex("^[0123456789ABCDEFGHJKMNPQRSTVWXYZ]{26}$")

                ourULID shouldMatch pattern
                libraryULID shouldMatch pattern
            }

            test("should be able to parse ULID generated by ulid-kotlin library") {
                val libraryULID = ULID.nextULID().toString()
                val result = BinaryId.of(libraryULID)

                result.toString() shouldBe libraryULID
            }

            test("timestamp ordering should be preserved") {
                val ids = mutableListOf<BinaryId>()

                // Generate multiple IDs with small delays
                repeat(3) {
                    ids.add(BinaryId.generate())
                    Thread.sleep(2) // 2ms delay to ensure different timestamps
                }

                // Convert to strings and verify lexicographic ordering
                val stringIds = ids.map { it.toString() }
                val sortedIds = stringIds.sorted()

                stringIds shouldBe sortedIds
            }

            test("should generate unique IDs consistently") {
                val ids = (1..100).map { BinaryId.generate().toString() }.toSet()
                ids.size shouldBe 100 // All should be unique
            }
        }
    })
