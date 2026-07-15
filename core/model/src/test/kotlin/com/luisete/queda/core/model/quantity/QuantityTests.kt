package com.luisete.queda.core.model.quantity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.math.BigDecimal

@Suppress("SwallowedException")
class QuantityTests {
    @Test
    fun `ExactQuantity rejects negative amount`() {
        try {
            ExactQuantity.of(BigDecimal("-1"), MeasurementUnit.GRAM)
            throw AssertionError("Should have rejected negative amount")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun `ExactQuantity accepts zero`() {
        val q = ExactQuantity.of(BigDecimal.ZERO, MeasurementUnit.UNIT)
        assertEquals(BigDecimal.ZERO, q.amount)
    }

    @Test
    fun `ExactQuantity accepts three decimals`() {
        ExactQuantity.of("1.123", MeasurementUnit.LITER)
    }

    @Test
    fun `ExactQuantity rejects more than three decimals`() {
        try {
            ExactQuantity.of("1.1234", MeasurementUnit.LITER)
            throw AssertionError("Should have rejected more than 3 decimals")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun `ExactQuantity normalizes trailing zeros`() {
        val q1 = ExactQuantity.of("1.100", MeasurementUnit.GRAM)
        val q2 = ExactQuantity.of("1.1", MeasurementUnit.GRAM)
        assertEquals(q1, q2)
        assertEquals(1, q1.amount.scale())
    }

    @Test
    fun `ExactQuantity equality by normalized value and unit`() {
        val q1 = ExactQuantity.of("1.0", MeasurementUnit.GRAM)
        val q2 = ExactQuantity.of("1", MeasurementUnit.GRAM)
        val q3 = ExactQuantity.of("1.0", MeasurementUnit.KILOGRAM)

        assertEquals(q1, q2)
        assertNotEquals(q1, q3)
    }

    @Test
    fun `ExactQuantity parsing from string`() {
        val q = ExactQuantity.of("0.5", MeasurementUnit.UNIT)
        assertEquals(BigDecimal("0.5"), q.amount)
    }
}
