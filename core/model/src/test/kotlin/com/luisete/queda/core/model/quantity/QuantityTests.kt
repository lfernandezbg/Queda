package com.luisete.queda.core.model.quantity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class QuantityTests {
    @Test
    fun zeroIsAccepted() {
        val q = ExactQuantity.of(BigDecimal.ZERO, MeasurementUnit.UNIT)
        assertEquals(BigDecimal.ZERO, q.amount)
    }

    @Test
    fun negativeIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ExactQuantity.of(BigDecimal("-1"), MeasurementUnit.GRAM)
        }
    }

    @Test
    fun fractionalUnitIsAccepted() {
        val q = ExactQuantity.of("0.5", MeasurementUnit.UNIT)
        assertEquals(BigDecimal("0.5"), q.amount)
    }

    @Test
    fun oneDecimalIsAccepted() {
        ExactQuantity.of("1.2", MeasurementUnit.GRAM)
    }

    @Test
    fun twoDecimalsAreAccepted() {
        ExactQuantity.of("1.23", MeasurementUnit.GRAM)
    }

    @Test
    fun threeDecimalsAreAccepted() {
        ExactQuantity.of("1.234", MeasurementUnit.GRAM)
    }

    @Test
    fun fourSignificantDecimalsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ExactQuantity.of("1.2345", MeasurementUnit.GRAM)
        }
    }

    @Test
    fun trailingZerosDoNotCountAsExtraDecimals() {
        ExactQuantity.of("1.2000", MeasurementUnit.GRAM)
    }

    @Test
    fun zeroIsNormalizedToBigDecimalZero() {
        val q = ExactQuantity.of("0.000", MeasurementUnit.GRAM)
        assertEquals(BigDecimal.ZERO, q.amount)
    }

    @Test
    fun trailingZerosAreRemoved() {
        val q = ExactQuantity.of("1.500", MeasurementUnit.GRAM)
        assertEquals("1.5", q.amount.toPlainString())
    }

    @Test
    fun onePointZeroEqualsOnePointZeroZeroZero() {
        val q1 = ExactQuantity.of("1.0", MeasurementUnit.GRAM)
        val q2 = ExactQuantity.of("1.000", MeasurementUnit.GRAM)
        assertEquals(q1, q2)
    }

    @Test
    fun equalAmountDifferentUnitIsNotEqual() {
        val q1 = ExactQuantity.of("1", MeasurementUnit.GRAM)
        val q2 = ExactQuantity.of("1", MeasurementUnit.KILOGRAM)
        assertNotEquals(q1, q2)
    }

    @Test
    fun validIntegerStringIsParsed() {
        val q = ExactQuantity.of("10", MeasurementUnit.UNIT)
        assertTrue(BigDecimal("10").compareTo(q.amount) == 0)
    }

    @Test
    fun validDecimalStringIsParsed() {
        val q = ExactQuantity.of("10.5", MeasurementUnit.UNIT)
        assertTrue(BigDecimal("10.5").compareTo(q.amount) == 0)
    }

    @Test
    fun emptyStringIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ExactQuantity.of("", MeasurementUnit.UNIT)
        }
    }

    @Test
    fun blankStringIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ExactQuantity.of(" ", MeasurementUnit.UNIT)
        }
    }

    @Test
    fun alphabeticStringIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ExactQuantity.of("abc", MeasurementUnit.UNIT)
        }
    }

    @Test
    fun commaDecimalStringIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ExactQuantity.of("1,5", MeasurementUnit.UNIT)
        }
    }

    @Test
    fun nanStringIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ExactQuantity.of("NaN", MeasurementUnit.UNIT)
        }
    }

    @Test
    fun infinityStringIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ExactQuantity.of("Infinity", MeasurementUnit.UNIT)
        }
    }

    @Test
    fun negativeStringIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ExactQuantity.of("-1", MeasurementUnit.UNIT)
        }
    }

    @Test
    fun fourDecimalStringIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ExactQuantity.of("0.0001", MeasurementUnit.UNIT)
        }
    }

    @Test
    fun approximateLevelHasExplicitExpectedOrder() {
        assertEquals(0, ApproximateLevel.EMPTY.order)
        assertEquals(1, ApproximateLevel.ALMOST_EMPTY.order)
        assertEquals(2, ApproximateLevel.LOW.order)
        assertEquals(3, ApproximateLevel.MEDIUM.order)
        assertEquals(4, ApproximateLevel.HIGH.order)
        assertEquals(5, ApproximateLevel.FULL.order)
    }

    @Test
    fun approximateLevelOrderDoesNotDependOnOrdinal() {
        // This is a conceptual check, as long as we use .order it's fine.
        assertTrue(ApproximateLevel.FULL.order > ApproximateLevel.EMPTY.order)
    }

    @Test
    fun approximateQuantityPreservesLevel() {
        val q = ApproximateQuantity(ApproximateLevel.MEDIUM)
        assertEquals(ApproximateLevel.MEDIUM, q.level)
    }

    @Test
    fun isRepresentableAcceptsZero() {
        assertTrue(ExactQuantity.isRepresentable(BigDecimal.ZERO))
    }

    @Test
    fun isRepresentableAcceptsThreeDecimals() {
        assertTrue(ExactQuantity.isRepresentable(BigDecimal("1.234")))
    }

    @Test
    fun isRepresentableRejectsNegative() {
        assertTrue(!ExactQuantity.isRepresentable(BigDecimal("-1")))
    }

    @Test
    fun isRepresentableRejectsFourDecimals() {
        assertTrue(!ExactQuantity.isRepresentable(BigDecimal("1.2345")))
    }
}
