package com.luisete.queda.feature.inventory

import com.luisete.queda.core.model.quantity.ExactQuantity
import com.luisete.queda.core.model.quantity.MeasurementUnit
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class ExactQuantityUiFormatterTest {
    @Test
    fun integerHasNoDecimalSeparator() {
        val quantity = ExactQuantity.of(BigDecimal("6"), MeasurementUnit.UNIT)
        val formatted = ExactQuantityUiFormatter.format(quantity)
        assertEquals("6", formatted)
    }

    @Test
    fun dotIsDisplayedAsComma() {
        val quantity = ExactQuantity.of(BigDecimal("1.5"), MeasurementUnit.KILOGRAM)
        val formatted = ExactQuantityUiFormatter.format(quantity)
        assertEquals("1,5", formatted)
    }

    @Test
    fun threeDecimalsArePreserved() {
        val quantity = ExactQuantity.of(BigDecimal("0.001"), MeasurementUnit.GRAM)
        val formatted = ExactQuantityUiFormatter.format(quantity)
        assertEquals("0,001", formatted)
    }

    @Test
    fun trailingZerosAreNotAdded() {
        val quantity = ExactQuantity.of(BigDecimal("1.500"), MeasurementUnit.LITER)
        val formatted = ExactQuantityUiFormatter.format(quantity)
        assertEquals("1,5", formatted)
    }

    @Test
    fun formatterDoesNotRound() {
        val quantity = ExactQuantity.of(BigDecimal("1.234"), MeasurementUnit.MILLILITER)
        val formatted = ExactQuantityUiFormatter.format(quantity)
        assertEquals("1,234", formatted)
    }

    @Test
    fun formatterDoesNotIncludeUnitText() {
        val quantity = ExactQuantity.of(BigDecimal("500"), MeasurementUnit.GRAM)
        val formatted = ExactQuantityUiFormatter.format(quantity)
        assertEquals("500", formatted)
    }
}
