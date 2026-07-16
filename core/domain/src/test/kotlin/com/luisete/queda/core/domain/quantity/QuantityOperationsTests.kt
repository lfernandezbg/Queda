package com.luisete.queda.core.domain.quantity

import com.luisete.queda.core.domain.result.DomainError
import com.luisete.queda.core.model.quantity.ApproximateLevel
import com.luisete.queda.core.model.quantity.ApproximateQuantity
import com.luisete.queda.core.model.quantity.ExactQuantity
import com.luisete.queda.core.model.quantity.MeasurementUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class QuantityOperationsTests {
    private val oneGram = ExactQuantity.of("1", MeasurementUnit.GRAM)
    private val oneKg = ExactQuantity.of("1", MeasurementUnit.KILOGRAM)
    private val oneLiter = ExactQuantity.of("1", MeasurementUnit.LITER)
    private val oneUnit = ExactQuantity.of("1", MeasurementUnit.UNIT)

    private fun assertAmount(
        expected: String,
        actual: BigDecimal,
    ) {
        assertTrue(
            "Expected $expected but was ${actual.toPlainString()}",
            BigDecimal(expected).compareTo(actual) == 0,
        )
    }

    // CONVERT (1-13)
    @Test fun convertSameUnitPreservesValue() {
        assertEquals(oneGram, QuantityOperations.convert(oneGram, MeasurementUnit.GRAM).successValue())
    }

    @Test fun convert1000GramTo1Kilogram() {
        assertEquals(
            oneKg,
            QuantityOperations.convert(
                ExactQuantity.of("1000", MeasurementUnit.GRAM),
                MeasurementUnit.KILOGRAM,
            ).successValue(),
        )
    }

    @Test fun convert1KilogramTo1000Gram() {
        assertEquals(
            ExactQuantity.of("1000", MeasurementUnit.GRAM),
            QuantityOperations.convert(oneKg, MeasurementUnit.GRAM).successValue(),
        )
    }

    @Test fun convert1000MilliliterTo1Liter() {
        assertEquals(
            oneLiter,
            QuantityOperations.convert(
                ExactQuantity.of("1000", MeasurementUnit.MILLILITER),
                MeasurementUnit.LITER,
            ).successValue(),
        )
    }

    @Test fun convert1LiterTo1000Milliliter() {
        assertEquals(
            ExactQuantity.of("1000", MeasurementUnit.MILLILITER),
            QuantityOperations.convert(oneLiter, MeasurementUnit.MILLILITER).successValue(),
        )
    }

    @Test fun convertUnitToUnit() {
        assertEquals(oneUnit, QuantityOperations.convert(oneUnit, MeasurementUnit.UNIT).successValue())
    }

    @Test fun convertUnitToGramFails() {
        assertEquals(
            DomainError.IncompatibleQuantityDimensions,
            QuantityOperations.convert(oneUnit, MeasurementUnit.GRAM).failureError(),
        )
    }

    @Test fun convertGramToUnitFails() {
        assertEquals(
            DomainError.IncompatibleQuantityDimensions,
            QuantityOperations.convert(oneGram, MeasurementUnit.UNIT).failureError(),
        )
    }

    @Test fun convertGramToMilliliterFails() {
        assertEquals(
            DomainError.IncompatibleQuantityDimensions,
            QuantityOperations.convert(oneGram, MeasurementUnit.MILLILITER).failureError(),
        )
    }

    @Test fun convertLiterToKilogramFails() {
        assertEquals(
            DomainError.IncompatibleQuantityDimensions,
            QuantityOperations.convert(oneLiter, MeasurementUnit.KILOGRAM).failureError(),
        )
    }

    @Test fun convert0001GramToKilogramFailsByPrecision() {
        assertEquals(
            DomainError.TooManyDecimalPlaces,
            QuantityOperations.convert(
                ExactQuantity.of("0.001", MeasurementUnit.GRAM),
                MeasurementUnit.KILOGRAM,
            ).failureError(),
        )
    }

    @Test fun convert0001MilliliterToLiterFailsByPrecision() {
        assertEquals(
            DomainError.TooManyDecimalPlaces,
            QuantityOperations.convert(
                ExactQuantity.of("0.001", MeasurementUnit.MILLILITER),
                MeasurementUnit.LITER,
            ).failureError(),
        )
    }

    @Test fun convertDoesNotModifyInput() {
        val q = ExactQuantity.of("1", MeasurementUnit.KILOGRAM)
        QuantityOperations.convert(q, MeasurementUnit.GRAM)
        assertAmount("1", q.amount)
    }

    // ADD (14-24)
    @Test fun addSameUnitMass() {
        assertEquals(
            ExactQuantity.of("30", MeasurementUnit.GRAM),
            QuantityOperations.add(
                ExactQuantity.of("10", MeasurementUnit.GRAM),
                ExactQuantity.of("20", MeasurementUnit.GRAM),
            ).successValue(),
        )
    }

    @Test fun addSameUnitVolume() {
        assertEquals(
            ExactQuantity.of("30", MeasurementUnit.MILLILITER),
            QuantityOperations.add(
                ExactQuantity.of("10", MeasurementUnit.MILLILITER),
                ExactQuantity.of("20", MeasurementUnit.MILLILITER),
            ).successValue(),
        )
    }

    @Test fun addSameUnitCount() {
        assertEquals(
            ExactQuantity.of("30", MeasurementUnit.UNIT),
            QuantityOperations.add(
                ExactQuantity.of("10", MeasurementUnit.UNIT),
                ExactQuantity.of("20", MeasurementUnit.UNIT),
            ).successValue(),
        )
    }

    @Test fun add1KilogramAnd500GramReturns15Kilogram() {
        assertEquals(
            ExactQuantity.of("1.5", MeasurementUnit.KILOGRAM),
            QuantityOperations.add(
                oneKg,
                ExactQuantity.of("500", MeasurementUnit.GRAM),
            ).successValue(),
        )
    }

    @Test fun add1KilogramAnd05GramReturns10005Gram() {
        assertEquals(
            ExactQuantity.of("1000.5", MeasurementUnit.GRAM),
            QuantityOperations.add(
                oneKg,
                ExactQuantity.of("0.5", MeasurementUnit.GRAM),
            ).successValue(),
        )
    }

    @Test fun add1LiterAnd500MilliliterReturns15Liter() {
        assertEquals(
            ExactQuantity.of("1.5", MeasurementUnit.LITER),
            QuantityOperations.add(
                oneLiter,
                ExactQuantity.of("500", MeasurementUnit.MILLILITER),
            ).successValue(),
        )
    }

    @Test fun add1LiterAnd05MilliliterReturns10005Milliliter() {
        assertEquals(
            ExactQuantity.of("1000.5", MeasurementUnit.MILLILITER),
            QuantityOperations.add(
                oneLiter,
                ExactQuantity.of("0.5", MeasurementUnit.MILLILITER),
            ).successValue(),
        )
    }

    @Test fun addZeroPreservesLeftQuantity() {
        assertEquals(
            oneKg,
            QuantityOperations.add(
                oneKg,
                ExactQuantity.of("0", MeasurementUnit.GRAM),
            ).successValue(),
        )
    }

    @Test fun addMassAndVolumeFails() {
        assertEquals(
            DomainError.IncompatibleQuantityDimensions,
            QuantityOperations.add(oneGram, oneLiter).failureError(),
        )
    }

    @Test fun addCountAndMassFails() {
        assertEquals(
            DomainError.IncompatibleQuantityDimensions,
            QuantityOperations.add(oneUnit, oneGram).failureError(),
        )
    }

    @Test fun addDoesNotModifyInputs() {
        val q1 = ExactQuantity.of("1", MeasurementUnit.KILOGRAM)
        val q2 = ExactQuantity.of("500", MeasurementUnit.GRAM)
        QuantityOperations.add(q1, q2)
        assertAmount("1", q1.amount)
        assertAmount("500", q2.amount)
    }

    // SUBTRACT (25-35)
    @Test fun subtractSameUnitMass() {
        assertEquals(
            ExactQuantity.of("20", MeasurementUnit.GRAM),
            QuantityOperations.subtract(
                ExactQuantity.of("30", MeasurementUnit.GRAM),
                ExactQuantity.of("10", MeasurementUnit.GRAM),
            ).successValue(),
        )
    }

    @Test fun subtractSameUnitVolume() {
        assertEquals(
            ExactQuantity.of("20", MeasurementUnit.MILLILITER),
            QuantityOperations.subtract(
                ExactQuantity.of("30", MeasurementUnit.MILLILITER),
                ExactQuantity.of("10", MeasurementUnit.MILLILITER),
            ).successValue(),
        )
    }

    @Test fun subtractSameUnitCount() {
        assertEquals(
            ExactQuantity.of("20", MeasurementUnit.UNIT),
            QuantityOperations.subtract(
                ExactQuantity.of("30", MeasurementUnit.UNIT),
                ExactQuantity.of("10", MeasurementUnit.UNIT),
            ).successValue(),
        )
    }

    @Test fun subtract1KilogramAnd500GramReturns05Kilogram() {
        assertEquals(
            ExactQuantity.of("0.5", MeasurementUnit.KILOGRAM),
            QuantityOperations.subtract(
                oneKg,
                ExactQuantity.of("500", MeasurementUnit.GRAM),
            ).successValue(),
        )
    }

    @Test fun subtract1KilogramAnd05GramReturns9995Gram() {
        assertEquals(
            ExactQuantity.of("999.5", MeasurementUnit.GRAM),
            QuantityOperations.subtract(
                oneKg,
                ExactQuantity.of("0.5", MeasurementUnit.GRAM),
            ).successValue(),
        )
    }

    @Test fun subtract1LiterAnd05MilliliterReturns9995Milliliter() {
        assertEquals(
            ExactQuantity.of("999.5", MeasurementUnit.MILLILITER),
            QuantityOperations.subtract(
                oneLiter,
                ExactQuantity.of("0.5", MeasurementUnit.MILLILITER),
            ).successValue(),
        )
    }

    @Test fun subtractToExactlyZero() {
        assertAmount(
            "0",
            QuantityOperations.subtract(
                oneKg,
                ExactQuantity.of("1000", MeasurementUnit.GRAM),
            ).successValue().amount,
        )
    }

    @Test fun subtractZeroPreservesLeftQuantity() {
        assertEquals(
            oneKg,
            QuantityOperations.subtract(
                oneKg,
                ExactQuantity.of("0", MeasurementUnit.GRAM),
            ).successValue(),
        )
    }

    @Test fun subtractMoreThanAvailableReturnsNegativeQuantity() {
        assertEquals(
            DomainError.NegativeQuantity,
            QuantityOperations.subtract(
                ExactQuantity.of("100", MeasurementUnit.GRAM),
                ExactQuantity.of("200", MeasurementUnit.GRAM),
            ).failureError(),
        )
    }

    @Test fun subtractMassAndVolumeFails() {
        assertEquals(
            DomainError.IncompatibleQuantityDimensions,
            QuantityOperations.subtract(oneGram, oneLiter).failureError(),
        )
    }

    @Test fun subtractDoesNotModifyInputs() {
        val q1 = ExactQuantity.of("1", MeasurementUnit.KILOGRAM)
        QuantityOperations.subtract(q1, oneGram)
        assertAmount("1", q1.amount)
    }

    // CONSUME (36-44)
    @Test fun consumePartialSameUnit() {
        assertEquals(
            ExactQuantity.of("7", MeasurementUnit.UNIT),
            QuantityOperations.consume(
                ExactQuantity.of("10", MeasurementUnit.UNIT),
                ExactQuantity.of("3", MeasurementUnit.UNIT),
            ).successValue(),
        )
    }

    @Test fun consumeTotalSameUnit() {
        assertAmount(
            "0",
            QuantityOperations.consume(
                ExactQuantity.of("10", MeasurementUnit.UNIT),
                ExactQuantity.of("10", MeasurementUnit.UNIT),
            ).successValue().amount,
        )
    }

    @Test fun consumeZero() {
        assertEquals(
            oneUnit,
            QuantityOperations.consume(
                oneUnit,
                ExactQuantity.of("0", MeasurementUnit.UNIT),
            ).successValue(),
        )
    }

    @Test fun consume1KilogramBy500GramReturns05Kilogram() {
        assertEquals(
            ExactQuantity.of("0.5", MeasurementUnit.KILOGRAM),
            QuantityOperations.consume(
                oneKg,
                ExactQuantity.of("500", MeasurementUnit.GRAM),
            ).successValue(),
        )
    }

    @Test fun consume1KilogramBy05GramReturns9995Gram() {
        assertEquals(
            ExactQuantity.of("999.5", MeasurementUnit.GRAM),
            QuantityOperations.consume(
                oneKg,
                ExactQuantity.of("0.5", MeasurementUnit.GRAM),
            ).successValue(),
        )
    }

    @Test fun consume1LiterBy05MilliliterReturns9995Milliliter() {
        assertEquals(
            ExactQuantity.of("999.5", MeasurementUnit.MILLILITER),
            QuantityOperations.consume(
                oneLiter,
                ExactQuantity.of("0.5", MeasurementUnit.MILLILITER),
            ).successValue(),
        )
    }

    @Test fun consumeMoreThanAvailableReturnsInsufficientQuantity() {
        assertEquals(
            DomainError.InsufficientQuantity,
            QuantityOperations.consume(
                ExactQuantity.of("100", MeasurementUnit.GRAM),
                ExactQuantity.of("200", MeasurementUnit.GRAM),
            ).failureError(),
        )
    }

    @Test fun consumeIncompatibleDimensionFails() {
        assertEquals(
            DomainError.IncompatibleQuantityDimensions,
            QuantityOperations.consume(oneUnit, oneGram).failureError(),
        )
    }

    @Test fun consumeDoesNotModifyInputs() {
        val q1 = ExactQuantity.of("1", MeasurementUnit.KILOGRAM)
        QuantityOperations.consume(q1, oneGram)
        assertAmount("1", q1.amount)
    }

    // CORRECT (45-52)
    @Test fun correctToZero() {
        assertAmount(
            "0",
            QuantityOperations.correct(
                oneUnit,
                BigDecimal.ZERO,
                MeasurementUnit.UNIT,
            ).successValue().amount,
        )
    }

    @Test fun correctToHigherValue() {
        assertAmount(
            "10",
            QuantityOperations.correct(
                oneUnit,
                BigDecimal.TEN,
                MeasurementUnit.UNIT,
            ).successValue().amount,
        )
    }

    @Test fun correctToLowerValue() {
        assertAmount(
            "1",
            QuantityOperations.correct(
                oneUnit,
                BigDecimal.ONE,
                MeasurementUnit.UNIT,
            ).successValue().amount,
        )
    }

    @Test fun correctToEquivalentCompatibleUnit() {
        assertEquals(
            ExactQuantity.of("1000", MeasurementUnit.GRAM),
            QuantityOperations.correct(
                oneKg,
                BigDecimal("1000"),
                MeasurementUnit.GRAM,
            ).successValue(),
        )
    }

    @Test fun correctNegativeFails() {
        assertEquals(
            DomainError.NegativeQuantity,
            QuantityOperations.correct(
                oneGram,
                BigDecimal("-1"),
                MeasurementUnit.GRAM,
            ).failureError(),
        )
    }

    @Test fun correctFourDecimalsFails() {
        assertEquals(
            DomainError.TooManyDecimalPlaces,
            QuantityOperations.correct(
                oneGram,
                BigDecimal("1.1234"),
                MeasurementUnit.GRAM,
            ).failureError(),
        )
    }

    @Test fun correctIncompatibleDimensionFails() {
        assertEquals(
            DomainError.IncompatibleQuantityDimensions,
            QuantityOperations.correct(
                oneGram,
                BigDecimal.ONE,
                MeasurementUnit.UNIT,
            ).failureError(),
        )
    }

    @Test fun correctDoesNotModifyInput() {
        val q = ExactQuantity.of("10", MeasurementUnit.GRAM)
        QuantityOperations.correct(q, BigDecimal.ONE, MeasurementUnit.GRAM)
        assertAmount("10", q.amount)
    }

    // APPROXIMATE CONSUME (53-65)
    @Test fun consumeFullToHigh() {
        assertEquals(
            ApproximateLevel.HIGH,
            QuantityOperations.consumeApproximate(
                ApproximateQuantity(ApproximateLevel.FULL),
                ApproximateLevel.HIGH,
            ).successValue().level,
        )
    }

    @Test fun consumeFullToMedium() {
        assertEquals(
            ApproximateLevel.MEDIUM,
            QuantityOperations.consumeApproximate(
                ApproximateQuantity(ApproximateLevel.FULL),
                ApproximateLevel.MEDIUM,
            ).successValue().level,
        )
    }

    @Test fun consumeFullToLow() {
        assertEquals(
            ApproximateLevel.LOW,
            QuantityOperations.consumeApproximate(
                ApproximateQuantity(ApproximateLevel.FULL),
                ApproximateLevel.LOW,
            ).successValue().level,
        )
    }

    @Test fun consumeFullToAlmostEmpty() {
        assertEquals(
            ApproximateLevel.ALMOST_EMPTY,
            QuantityOperations.consumeApproximate(
                ApproximateQuantity(ApproximateLevel.FULL),
                ApproximateLevel.ALMOST_EMPTY,
            ).successValue().level,
        )
    }

    @Test fun consumeFullToEmpty() {
        assertEquals(
            ApproximateLevel.EMPTY,
            QuantityOperations.consumeApproximate(
                ApproximateQuantity(ApproximateLevel.FULL),
                ApproximateLevel.EMPTY,
            ).successValue().level,
        )
    }

    @Test fun consumeHighToLow() {
        assertEquals(
            ApproximateLevel.LOW,
            QuantityOperations.consumeApproximate(
                ApproximateQuantity(ApproximateLevel.HIGH),
                ApproximateLevel.LOW,
            ).successValue().level,
        )
    }

    @Test fun consumeLowToAlmostEmpty() {
        assertEquals(
            ApproximateLevel.ALMOST_EMPTY,
            QuantityOperations.consumeApproximate(
                ApproximateQuantity(ApproximateLevel.LOW),
                ApproximateLevel.ALMOST_EMPTY,
            ).successValue().level,
        )
    }

    @Test fun consumeLowToEmpty() {
        assertEquals(
            ApproximateLevel.EMPTY,
            QuantityOperations.consumeApproximate(
                ApproximateQuantity(ApproximateLevel.LOW),
                ApproximateLevel.EMPTY,
            ).successValue().level,
        )
    }

    @Test fun consumeLowToLowFails() {
        assertEquals(
            DomainError.ApproximateLevelDidNotDecrease,
            QuantityOperations.consumeApproximate(
                ApproximateQuantity(ApproximateLevel.LOW),
                ApproximateLevel.LOW,
            ).failureError(),
        )
    }

    @Test fun consumeLowToMediumFails() {
        assertEquals(
            DomainError.ApproximateLevelDidNotDecrease,
            QuantityOperations.consumeApproximate(
                ApproximateQuantity(ApproximateLevel.LOW),
                ApproximateLevel.MEDIUM,
            ).failureError(),
        )
    }

    @Test fun consumeEmptyToEmptyFails() {
        assertEquals(
            DomainError.ApproximateLevelDidNotDecrease,
            QuantityOperations.consumeApproximate(
                ApproximateQuantity(ApproximateLevel.EMPTY),
                ApproximateLevel.EMPTY,
            ).failureError(),
        )
    }

    @Test fun consumeEmptyToFullFails() {
        assertEquals(
            DomainError.ApproximateLevelDidNotDecrease,
            QuantityOperations.consumeApproximate(
                ApproximateQuantity(ApproximateLevel.EMPTY),
                ApproximateLevel.FULL,
            ).failureError(),
        )
    }

    @Test fun consumeApproximateDoesNotModifyInput() {
        val q = ApproximateQuantity(ApproximateLevel.FULL)
        QuantityOperations.consumeApproximate(q, ApproximateLevel.LOW)
        assertEquals(ApproximateLevel.FULL, q.level)
    }

    // APPROXIMATE CORRECT (66-71)
    @Test fun correctFullToEmpty() {
        assertEquals(
            ApproximateLevel.EMPTY,
            QuantityOperations.correctApproximate(ApproximateLevel.EMPTY).successValue().level,
        )
    }

    @Test fun correctEmptyToFull() {
        assertEquals(
            ApproximateLevel.FULL,
            QuantityOperations.correctApproximate(ApproximateLevel.FULL).successValue().level,
        )
    }

    @Test fun correctLowToLow() {
        assertEquals(
            ApproximateLevel.LOW,
            QuantityOperations.correctApproximate(ApproximateLevel.LOW).successValue().level,
        )
    }

    @Test fun correctHighToLow() {
        assertEquals(
            ApproximateLevel.LOW,
            QuantityOperations.correctApproximate(ApproximateLevel.LOW).successValue().level,
        )
    }

    @Test fun correctLowToHigh() {
        assertEquals(
            ApproximateLevel.HIGH,
            QuantityOperations.correctApproximate(ApproximateLevel.HIGH).successValue().level,
        )
    }

    @Test fun correctMediumToAlmostEmpty() {
        assertEquals(
            ApproximateLevel.ALMOST_EMPTY,
            QuantityOperations.correctApproximate(ApproximateLevel.ALMOST_EMPTY).successValue().level,
        )
    }
}
