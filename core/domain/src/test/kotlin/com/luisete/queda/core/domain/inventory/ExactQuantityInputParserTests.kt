package com.luisete.queda.core.domain.inventory

import com.luisete.queda.core.model.quantity.MeasurementUnit
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class ExactQuantityInputParserTests {
    @Test
    fun integerUnitIsAccepted() {
        val res = ExactQuantityInputParser.parse("6", MeasurementUnit.UNIT) as ExactQuantityInputResult.Success
        assertEquals(0, res.quantity.amount.compareTo(BigDecimal("6")))
        assertEquals(MeasurementUnit.UNIT, res.quantity.unit)
    }

    @Test
    fun dotDecimalIsAccepted() {
        val res = ExactQuantityInputParser.parse("1.5", MeasurementUnit.KILOGRAM) as ExactQuantityInputResult.Success
        assertEquals(0, res.quantity.amount.compareTo(BigDecimal("1.5")))
    }

    @Test
    fun commaDecimalIsAccepted() {
        val res = ExactQuantityInputParser.parse("1,25", MeasurementUnit.LITER) as ExactQuantityInputResult.Success
        assertEquals(0, res.quantity.amount.compareTo(BigDecimal("1.25")))
    }

    @Test
    fun threeDecimalsAreAccepted() {
        val res = ExactQuantityInputParser.parse("0.001", MeasurementUnit.GRAM) as ExactQuantityInputResult.Success
        assertEquals(0, res.quantity.amount.compareTo(BigDecimal("0.001")))
    }

    @Test
    fun surroundingSpacesAreRemoved() {
        val res = ExactQuantityInputParser.parse("  5  ", MeasurementUnit.UNIT) as ExactQuantityInputResult.Success
        assertEquals(0, res.quantity.amount.compareTo(BigDecimal("5")))
    }

    @Test
    fun leadingZerosAreAcceptedAndNormalized() {
        val res = ExactQuantityInputParser.parse("0001,500", MeasurementUnit.UNIT) as ExactQuantityInputResult.Success
        assertEquals(0, res.quantity.amount.compareTo(BigDecimal("1.5")))
    }

    @Test
    fun trailingZerosBeyondThreeAreAcceptedWhenNormalizedScaleFits() {
        val res = ExactQuantityInputParser.parse("1.0000", MeasurementUnit.UNIT) as ExactQuantityInputResult.Success
        assertEquals(0, res.quantity.amount.compareTo(BigDecimal("1")))
        assertEquals(0, res.quantity.amount.scale())
    }

    @Test
    fun blankIsRejected() {
        assertEquals(ExactQuantityInputResult.Blank, ExactQuantityInputParser.parse("", MeasurementUnit.UNIT))
        assertEquals(ExactQuantityInputResult.Blank, ExactQuantityInputParser.parse("   ", MeasurementUnit.UNIT))
    }

    @Test
    fun internalSpaceIsRejected() {
        assertEquals(
            ExactQuantityInputResult.InvalidFormat,
            ExactQuantityInputParser.parse("1 5", MeasurementUnit.UNIT),
        )
    }

    @Test
    fun zeroIsRejected() {
        assertEquals(
            ExactQuantityInputResult.NotPositive,
            ExactQuantityInputParser.parse("0", MeasurementUnit.UNIT),
        )
        assertEquals(
            ExactQuantityInputResult.NotPositive,
            ExactQuantityInputParser.parse("0.000", MeasurementUnit.UNIT),
        )
    }

    @Test
    fun negativeIsRejected() {
        assertEquals(
            ExactQuantityInputResult.InvalidFormat,
            ExactQuantityInputParser.parse("-1", MeasurementUnit.UNIT),
        )
    }

    @Test
    fun plusSignIsRejected() {
        assertEquals(
            ExactQuantityInputResult.InvalidFormat,
            ExactQuantityInputParser.parse("+1", MeasurementUnit.UNIT),
        )
    }

    @Test
    fun alphabeticInputIsRejected() {
        assertEquals(
            ExactQuantityInputResult.InvalidFormat,
            ExactQuantityInputParser.parse("abc", MeasurementUnit.UNIT),
        )
    }

    @Test
    fun scientificNotationIsRejected() {
        assertEquals(
            ExactQuantityInputResult.InvalidFormat,
            ExactQuantityInputParser.parse("1e3", MeasurementUnit.UNIT),
        )
    }

    @Test
    fun nanIsRejected() {
        assertEquals(
            ExactQuantityInputResult.InvalidFormat,
            ExactQuantityInputParser.parse("NaN", MeasurementUnit.UNIT),
        )
    }

    @Test
    fun infinityIsRejected() {
        assertEquals(
            ExactQuantityInputResult.InvalidFormat,
            ExactQuantityInputParser.parse("Infinity", MeasurementUnit.UNIT),
        )
    }

    @Test
    fun pointAndCommaTogetherAreRejected() {
        assertEquals(
            ExactQuantityInputResult.InvalidFormat,
            ExactQuantityInputParser.parse("1,2.3", MeasurementUnit.UNIT),
        )
    }

    @Test
    fun repeatedSeparatorsAreRejected() {
        assertEquals(
            ExactQuantityInputResult.InvalidFormat,
            ExactQuantityInputParser.parse("1..2", MeasurementUnit.UNIT),
        )
        assertEquals(
            ExactQuantityInputResult.InvalidFormat,
            ExactQuantityInputParser.parse("1,,2", MeasurementUnit.UNIT),
        )
    }

    @Test
    fun fourSignificantDecimalsAreRejected() {
        assertEquals(
            ExactQuantityInputResult.TooManyDecimalPlaces,
            ExactQuantityInputParser.parse("1.2345", MeasurementUnit.UNIT),
        )
    }

    @Test
    fun trailingSeparatorIsRejected() {
        assertEquals(
            ExactQuantityInputResult.InvalidFormat,
            ExactQuantityInputParser.parse("1.", MeasurementUnit.UNIT),
        )
        assertEquals(
            ExactQuantityInputResult.InvalidFormat,
            ExactQuantityInputParser.parse("1,", MeasurementUnit.UNIT),
        )
    }

    @Test
    fun leadingSeparatorIsRejected() {
        assertEquals(
            ExactQuantityInputResult.InvalidFormat,
            ExactQuantityInputParser.parse(".5", MeasurementUnit.UNIT),
        )
        assertEquals(
            ExactQuantityInputResult.InvalidFormat,
            ExactQuantityInputParser.parse(",5", MeasurementUnit.UNIT),
        )
    }

    @Test
    fun onlySeparatorIsRejected() {
        assertEquals(
            ExactQuantityInputResult.InvalidFormat,
            ExactQuantityInputParser.parse(".", MeasurementUnit.UNIT),
        )
        assertEquals(
            ExactQuantityInputResult.InvalidFormat,
            ExactQuantityInputParser.parse(",", MeasurementUnit.UNIT),
        )
    }

    @Test
    fun whitespaceAroundValueIsRejected() {
        assertEquals(
            ExactQuantityInputResult.InvalidFormat,
            ExactQuantityInputParser.parse("\t1\t", MeasurementUnit.UNIT),
        )
        assertEquals(
            ExactQuantityInputResult.InvalidFormat,
            ExactQuantityInputParser.parse("\n1\n", MeasurementUnit.UNIT),
        )
        assertEquals(
            ExactQuantityInputResult.InvalidFormat,
            ExactQuantityInputParser.parse("\r1\r", MeasurementUnit.UNIT),
        )
    }
}
