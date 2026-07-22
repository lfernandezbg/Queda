package com.luisete.queda.core.model.barcode

import io.kotest.common.ExperimentalKotest
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalKotest::class)
class BarcodePropertyTest {
    private val config = PropTestConfig(seed = 42L, iterations = 100)

    private val supportedLengths = listOf(8, 12, 13, 14)

    private val validPayloadArb =
        arbitrary {
            val length = Arb.element(supportedLengths).bind()
            val payloadLength = length - 1
            (1..payloadLength).map { Arb.int(0, 9).bind() }.joinToString("")
        }

    @Test
    fun generatedValidPayloadPlusCalculatedCheckDigitValidates() =
        runTest {
            checkAll(config, validPayloadArb) { payload ->
                val cd = calculateCheckDigit(payload)
                val fullBarcode = payload + cd
                val result = Barcode.create(fullBarcode)
                assertTrue("Should be valid: $fullBarcode", result is BarcodeCreationResult.Success)
            }
        }

    @Test
    fun changingTheCheckDigitInvalidates() =
        runTest {
            checkAll(config, validPayloadArb) { payload ->
                val cd = calculateCheckDigit(payload)
                val wrongCd = (cd + 1) % 10
                val fullBarcode = payload + wrongCd
                val result = Barcode.create(fullBarcode)
                assertTrue("Should be invalid CD: $fullBarcode", result is BarcodeCreationResult.InvalidCheckDigit)
            }
        }

    @Test
    fun canonicalizationNeverRemovesLeadingZeroes() =
        runTest {
            val payloadWithLeadingZeroArb =
                arbitrary {
                    val payloadLength = 12 // For EAN-13
                    "0" + (1 until payloadLength).map { Arb.int(0, 9).bind() }.joinToString("")
                }
            checkAll(config, payloadWithLeadingZeroArb) { payload ->
                val cd = calculateCheckDigit(payload)
                val fullBarcode = payload + cd
                val result = Barcode.create("  $fullBarcode  ")
                assertTrue(result is BarcodeCreationResult.Success)
                assertEquals(fullBarcode, (result as BarcodeCreationResult.Success).barcode.value)
            }
        }

    @Test
    fun acceptedLengthsAreExactly8_12_13_14() =
        runTest {
            checkAll(config, Arb.int(1, 20)) { length ->
                val payload = (1..length).map { '1' }.joinToString("")
                val result = Barcode.create(payload)
                if (result is BarcodeCreationResult.UnsupportedFormat) {
                    assertTrue(length !in supportedLengths)
                } else if (result is BarcodeCreationResult.Success ||
                    result is BarcodeCreationResult.InvalidCheckDigit
                ) {
                    assertTrue(length in supportedLengths)
                }
            }
        }

    private fun calculateCheckDigit(payload: String): Int {
        val length = payload.length + 1
        var sum = 0
        for (i in payload.indices.reversed()) {
            val digit = payload[i].digitToInt()
            val positionFromRight = length - i
            val weight = if (positionFromRight % 2 == 0) 3 else 1
            sum += digit * weight
        }
        val remainder = sum % 10
        return if (remainder == 0) 0 else 10 - remainder
    }
}
