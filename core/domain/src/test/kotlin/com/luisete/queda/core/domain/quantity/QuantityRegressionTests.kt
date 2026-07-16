package com.luisete.queda.core.domain.quantity

import com.luisete.queda.core.model.quantity.ApproximateLevel
import com.luisete.queda.core.model.quantity.ApproximateQuantity
import com.luisete.queda.core.model.quantity.ExactQuantity
import com.luisete.queda.core.model.quantity.MeasurementUnit
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class QuantityRegressionTests {
    @Test
    fun regressionAddHalfGramToOneKilogramMustNotFail() {
        // Regression guard for Phase 1.1.
        val q1 = ExactQuantity.of("1", MeasurementUnit.KILOGRAM)
        val q2 = ExactQuantity.of("0.5", MeasurementUnit.GRAM)
        val res = QuantityOperations.add(q1, q2).successValue()
        assertEquals(BigDecimal("1000.5"), res.amount)
    }

    @Test
    fun regressionSubtractHalfGramFromOneKilogramMustNotFail() {
        // Regression guard for Phase 1.1.
        val q1 = ExactQuantity.of("1", MeasurementUnit.KILOGRAM)
        val q2 = ExactQuantity.of("0.5", MeasurementUnit.GRAM)
        val res = QuantityOperations.subtract(q1, q2).successValue()
        assertEquals(BigDecimal("999.5"), res.amount)
    }

    @Test
    fun regressionConsumeHalfGramFromOneKilogramMustNotFail() {
        // Regression guard for Phase 1.1.
        val q1 = ExactQuantity.of("1", MeasurementUnit.KILOGRAM)
        val q2 = ExactQuantity.of("0.5", MeasurementUnit.GRAM)
        QuantityOperations.consume(q1, q2).successValue()
    }

    @Test
    fun regressionAddHalfMilliliterToOneLiterMustNotFail() {
        // Regression guard for Phase 1.1.
        val q1 = ExactQuantity.of("1", MeasurementUnit.LITER)
        val q2 = ExactQuantity.of("0.5", MeasurementUnit.MILLILITER)
        QuantityOperations.add(q1, q2).successValue()
    }

    @Test
    fun regressionSubtractHalfMilliliterFromOneLiterMustNotFail() {
        // Regression guard for Phase 1.1.
        val q1 = ExactQuantity.of("1", MeasurementUnit.LITER)
        val q2 = ExactQuantity.of("0.5", MeasurementUnit.MILLILITER)
        QuantityOperations.subtract(q1, q2).successValue()
    }

    @Test
    fun regressionConsumeHalfMilliliterFromOneLiterMustNotFail() {
        // Regression guard for Phase 1.1.
        val q1 = ExactQuantity.of("1", MeasurementUnit.LITER)
        val q2 = ExactQuantity.of("0.5", MeasurementUnit.MILLILITER)
        QuantityOperations.consume(q1, q2).successValue()
    }

    @Test
    fun regressionFallbackAtSmallestSupportedPrecisionMustNotRound() {
        val q1 = ExactQuantity.of("1", MeasurementUnit.KILOGRAM)
        val q2 = ExactQuantity.of("0.001", MeasurementUnit.GRAM)
        val res = QuantityOperations.add(q1, q2).successValue()
        assertEquals(BigDecimal("1000.001"), res.amount)
        assertEquals(MeasurementUnit.GRAM, res.unit)
    }

    @Test
    fun regressionZeroMustNotKeepNegativeScale() {
        // Regression guard for Phase 1.1.
        val q = ExactQuantity.of(BigDecimal("0.000"), MeasurementUnit.GRAM)
        assertEquals(0, q.amount.scale())
    }

    @Test
    fun regressionTrailingZerosMustNotBreakEquality() {
        // Regression guard for Phase 1.1.
        val q1 = ExactQuantity.of("1.0", MeasurementUnit.GRAM)
        val q2 = ExactQuantity.of("1.00", MeasurementUnit.GRAM)
        assertEquals(q1, q2)
    }

    @Test
    fun regressionExplicitConversionMustNotRound() {
        // Regression guard for Phase 1.1.
        val q = ExactQuantity.of("1", MeasurementUnit.GRAM)
        val res = QuantityOperations.convert(q, MeasurementUnit.KILOGRAM).successValue()
        assertEquals(BigDecimal("0.001"), res.amount)
    }

    @Test
    fun regressionMixedDimensionsMustNeverSucceed() {
        // Regression guard for Phase 1.1.
        val q1 = ExactQuantity.of("1", MeasurementUnit.UNIT)
        val q2 = ExactQuantity.of("1", MeasurementUnit.GRAM)
        QuantityOperations.add(q1, q2).failureError()
    }

    @Test
    fun regressionApproximateSameLevelMustNeverBeConsumption() {
        // Regression guard for Phase 1.1.
        val q = ApproximateQuantity(ApproximateLevel.LOW)
        QuantityOperations.consumeApproximate(q, ApproximateLevel.LOW).failureError()
    }
}
