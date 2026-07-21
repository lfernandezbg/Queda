package com.luisete.queda.core.model.barcode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeTest {
    @Test
    fun validEan8() {
        val result = Barcode.create("73513537")
        assertTrue(result is BarcodeCreationResult.Success)
        assertEquals("73513537", (result as BarcodeCreationResult.Success).barcode.value)
    }

    @Test
    fun invalidEan8CheckDigit() {
        assertEquals(BarcodeCreationResult.InvalidCheckDigit, Barcode.create("73513538"))
    }

    @Test
    fun validUpcA() {
        val result = Barcode.create("036000291452")
        assertTrue(result is BarcodeCreationResult.Success)
        assertEquals("036000291452", (result as BarcodeCreationResult.Success).barcode.value)
    }

    @Test
    fun invalidUpcACheckDigit() {
        assertEquals(BarcodeCreationResult.InvalidCheckDigit, Barcode.create("036000291453"))
    }

    @Test
    fun validEan13() {
        val result = Barcode.create("4006381333931")
        assertTrue(result is BarcodeCreationResult.Success)
        assertEquals("4006381333931", (result as BarcodeCreationResult.Success).barcode.value)
    }

    @Test
    fun invalidEan13CheckDigit() {
        assertEquals(BarcodeCreationResult.InvalidCheckDigit, Barcode.create("4006381333932"))
    }

    @Test
    fun validItf14() {
        val result = Barcode.create("05011013500055")
        assertTrue(result is BarcodeCreationResult.Success)
        assertEquals("05011013500055", (result as BarcodeCreationResult.Success).barcode.value)
    }

    @Test
    fun invalidItf14CheckDigit() {
        assertEquals(BarcodeCreationResult.InvalidCheckDigit, Barcode.create("05011013500056"))
    }

    @Test
    fun blankInput() {
        assertEquals(BarcodeCreationResult.Blank, Barcode.create(""))
        assertEquals(BarcodeCreationResult.Blank, Barcode.create("   "))
    }

    @Test
    fun surroundingWhitespaceNormalization() {
        val result = Barcode.create("  4006381333931  ")
        assertTrue(result is BarcodeCreationResult.Success)
        assertEquals("4006381333931", (result as BarcodeCreationResult.Success).barcode.value)
    }

    @Test
    fun nonDigitInput() {
        assertEquals(BarcodeCreationResult.NonDigit, Barcode.create("400638133393A"))
    }

    @Test
    fun unsupportedShortLength() {
        assertEquals(BarcodeCreationResult.UnsupportedFormat, Barcode.create("1234567"))
    }

    @Test
    fun unsupportedLongLength() {
        assertEquals(BarcodeCreationResult.UnsupportedFormat, Barcode.create("123456789012345"))
    }

    @Test
    fun leadingZeroPreservation() {
        val result = Barcode.create("036000291452")
        assertTrue(result is BarcodeCreationResult.Success)
        assertEquals("036000291452", (result as BarcodeCreationResult.Success).barcode.value)
    }

    @Test
    fun canonicalEquality() {
        val b1 = (Barcode.create("4006381333931") as BarcodeCreationResult.Success).barcode
        val b2 = (Barcode.create(" 4006381333931 ") as BarcodeCreationResult.Success).barcode
        val b3 = (Barcode.create("73513537") as BarcodeCreationResult.Success).barcode

        assertEquals(b1, b2)
        assertEquals(b1.hashCode(), b2.hashCode())
        assertNotEquals(b1, b3)
    }

    @Test
    fun allZeroValidCode() {
        // EAN-8 0000000 -> Sum 0 -> CD 0
        val result = Barcode.create("00000000")
        assertTrue(result is BarcodeCreationResult.Success)
        assertEquals("00000000", (result as BarcodeCreationResult.Success).barcode.value)
    }

    @Test
    fun changingValidFinalCheckDigitInvalidatesIt() {
        val valid = "4006381333931"
        for (i in 0..9) {
            val candidate = valid.substring(0, 12) + i
            if (i == 1) {
                assertTrue(Barcode.create(candidate) is BarcodeCreationResult.Success)
            } else {
                assertTrue(Barcode.create(candidate) is BarcodeCreationResult.InvalidCheckDigit)
            }
        }
    }
}
