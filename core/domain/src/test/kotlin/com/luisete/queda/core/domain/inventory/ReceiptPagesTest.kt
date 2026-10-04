package com.luisete.queda.core.domain.inventory

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptPagesTest {
    @Test
    fun overlappingPhotographsOfOneTicketDoNotDuplicateSharedRows() =
        runTest {
            val reader =
                object : ReceiptTextReader {
                    override suspend fun recognize(imageUri: String): ReceiptRecognition =
                        ReceiptRecognition.Recognized(
                            ReceiptParser.parse(
                                if (imageUri == "first") "PAN 0,90\nLECHE 1,20" else "LECHE 1,20\nHUEVOS 2,30",
                            ),
                        )
                }
            val result = RecognizeReceiptUseCase(reader)(listOf("first", "second"))
            assertTrue(result is ReceiptRecognition.Recognized)
            val names = (result as ReceiptRecognition.Recognized).draft.lines.map { it.name }
            assertEquals(listOf("PAN", "LECHE", "HUEVOS"), names)
        }
}
