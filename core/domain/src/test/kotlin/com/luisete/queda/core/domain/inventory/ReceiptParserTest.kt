package com.luisete.queda.core.domain.inventory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptParserTest {
    @Test
    fun pricesNeverBecomeStockQuantities() {
        val line = ReceiptParser.parse("YOGUR NATURAL 0,85 1,70").lines.single()
        assertEquals("YOGUR NATURAL", line.name)
        assertEquals("1", line.quantity)
        assertFalse(line.selected)
        assertFalse(line.reviewed)
    }

    @Test
    fun explicitLeadingCountIsOnlyASuggestion() {
        val line = ReceiptParser.parse("2 LECHE ENTERA 1,20 2,40").lines.single()
        assertEquals("LECHE ENTERA", line.name)
        assertEquals("2", line.quantity)
        assertFalse(line.reviewed)
    }

    @Test
    fun totalsAndPaymentInformationAreExcluded() {
        val draft = ReceiptParser.parse("MERCADONA\nPAN 0,90\nTOTAL 12,40\nTARJETA 1234\nIVA 10%\nGRACIAS")
        assertEquals(listOf("PAN"), draft.lines.map { it.name })
    }

    @Test
    fun whitespaceAndCaseDoNotChangeFingerprintButReceiptDateDoes() {
        val first = ReceiptParser.parse("FECHA 2026-10-03\nPAN 0,90")
        val same = ReceiptParser.parse(" fecha   2026-10-03 \n pan 0,90 ")
        val different = ReceiptParser.parse("FECHA 2026-10-04\nPAN 0,90")
        assertEquals(first.fingerprint, same.fingerprint)
        assertTrue(first.fingerprint != different.fingerprint)
        assertTrue(first.id != same.id)
    }

    @Test
    fun emptyAndOversizedDocumentsAreBounded() {
        assertTrue(ReceiptParser.parse("12,30\n1234").lines.isEmpty())
        assertEquals(120, ReceiptParser.parse(List(200) { "PAN 0,90" }.joinToString("\n")).lines.size)
    }
}
