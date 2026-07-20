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

class QuantityDomainIntegrationTests {
    private fun assertAmount(
        expected: String,
        actual: BigDecimal,
    ) {
        assertTrue(
            "Expected $expected but was ${actual.toPlainString()}",
            BigDecimal(expected).compareTo(actual) == 0,
        )
    }

    @Test
    fun pantryMassSequenceMaintainsExactValue() {
        val start = ExactQuantity.of("2", MeasurementUnit.KILOGRAM)
        val step1 =
            QuantityOperations.consume(
                start,
                ExactQuantity.of("250", MeasurementUnit.GRAM),
            ).successValue()
        val step2 =
            QuantityOperations.add(
                step1,
                ExactQuantity.of("0.5", MeasurementUnit.KILOGRAM),
            ).successValue()
        val step3 =
            QuantityOperations.subtract(
                step2,
                ExactQuantity.of("100", MeasurementUnit.GRAM),
            ).successValue()

        assertEquals(
            ExactQuantity.of("2.15", MeasurementUnit.KILOGRAM),
            step3,
        )
    }

    @Test
    fun baseUnitFallbackCanReturnToWholeBaseValue() {
        val start = ExactQuantity.of("1", MeasurementUnit.KILOGRAM)
        val step1 =
            QuantityOperations.consume(
                start,
                ExactQuantity.of("0.5", MeasurementUnit.GRAM),
            ).successValue()
        assertEquals(MeasurementUnit.GRAM, step1.unit)
        assertAmount("999.5", step1.amount)

        val step2 =
            QuantityOperations.add(
                step1,
                ExactQuantity.of("0.5", MeasurementUnit.GRAM),
            ).successValue()
        assertAmount("1000", step2.amount)
        assertEquals(MeasurementUnit.GRAM, step2.unit)
    }

    @Test
    fun volumeSequenceMaintainsExactValue() {
        val start = ExactQuantity.of("2", MeasurementUnit.LITER)
        val step1 =
            QuantityOperations.consume(
                start,
                ExactQuantity.of("250.5", MeasurementUnit.MILLILITER),
            ).successValue()
        val step2 =
            QuantityOperations.add(
                step1,
                ExactQuantity.of("0.5", MeasurementUnit.LITER),
            ).successValue()

        assertEquals(
            ExactQuantity.of("2249.5", MeasurementUnit.MILLILITER),
            step2,
        )
    }

    @Test
    fun incompatibleOperationDoesNotAlterOriginalQuantities() {
        val q1 = ExactQuantity.of("1", MeasurementUnit.UNIT)
        val q2 = ExactQuantity.of("1", MeasurementUnit.GRAM)
        assertEquals(
            DomainError.IncompatibleQuantityDimensions,
            QuantityOperations.add(q1, q2).failureError(),
        )
        assertAmount("1", q1.amount)
        assertAmount("1", q2.amount)
    }

    @Test
    fun failedConsumptionDoesNotAlterAvailableQuantity() {
        val q1 = ExactQuantity.of("100", MeasurementUnit.GRAM)
        val q2 = ExactQuantity.of("200", MeasurementUnit.GRAM)
        assertEquals(
            DomainError.AmountMustBeLowerThanCurrent,
            QuantityOperations.consume(q1, q2).failureError(),
        )
        assertAmount("100", q1.amount)
    }

    @Test
    fun explicitPrecisionFailureDoesNotAlterInput() {
        val q = ExactQuantity.of("0.001", MeasurementUnit.GRAM)
        assertEquals(
            DomainError.TooManyDecimalPlaces,
            QuantityOperations.convert(q, MeasurementUnit.KILOGRAM).failureError(),
        )
        assertAmount("0.001", q.amount)
    }

    @Test
    fun approximateFullToHighToLowToEmptySequence() {
        val q1 = ApproximateQuantity(ApproximateLevel.FULL)
        val q2 =
            QuantityOperations.consumeApproximate(
                q1,
                ApproximateLevel.HIGH,
            ).successValue()
        val q3 =
            QuantityOperations.consumeApproximate(
                q2,
                ApproximateLevel.LOW,
            ).successValue()
        val q4 =
            QuantityOperations.consumeApproximate(
                q3,
                ApproximateLevel.EMPTY,
            ).successValue()
        assertEquals(ApproximateLevel.EMPTY, q4.level)
    }

    @Test
    fun approximateInvalidIncreaseLeavesOriginalUnchanged() {
        val q = ApproximateQuantity(ApproximateLevel.LOW)
        assertEquals(
            DomainError.ApproximateLevelDidNotDecrease,
            QuantityOperations.consumeApproximate(
                q,
                ApproximateLevel.HIGH,
            ).failureError(),
        )
        assertEquals(ApproximateLevel.LOW, q.level)
    }

    @Test
    fun correctionFromGramToCompatibleKilogramWithDifferentValuePreservesValue() {
        val start = ExactQuantity.of("1000", MeasurementUnit.GRAM)
        val corrected =
            QuantityOperations.correct(
                start,
                BigDecimal.valueOf(2),
                MeasurementUnit.KILOGRAM,
            ).successValue()
        assertEquals(
            ExactQuantity.of("2", MeasurementUnit.KILOGRAM),
            corrected,
        )
    }

    @Test
    fun repeatedCompatibleConversionsDoNotAccumulateRounding() {
        var q = ExactQuantity.of("1", MeasurementUnit.KILOGRAM)
        repeat(10) {
            q =
                QuantityOperations.convert(
                    q,
                    MeasurementUnit.GRAM,
                ).successValue()
            q =
                QuantityOperations.convert(
                    q,
                    MeasurementUnit.KILOGRAM,
                ).successValue()
        }
        assertEquals(ExactQuantity.of("1", MeasurementUnit.KILOGRAM), q)
    }

    @Test
    fun zeroConsumptionFails() {
        val q = ExactQuantity.of("1", MeasurementUnit.KILOGRAM)
        val zero = ExactQuantity.of(BigDecimal.ZERO, MeasurementUnit.GRAM)
        assertEquals(
            DomainError.AmountMustBePositive,
            QuantityOperations.consume(q, zero).failureError(),
        )
    }

    @Test
    fun equalOperationsProduceDeterministicResults() {
        val q1 = ExactQuantity.of("1", MeasurementUnit.KILOGRAM)
        val q2 = ExactQuantity.of("500", MeasurementUnit.GRAM)
        val res1 = QuantityOperations.add(q1, q2).successValue()
        val res2 = QuantityOperations.add(q1, q2).successValue()
        assertEquals(res1, res2)
    }
}
