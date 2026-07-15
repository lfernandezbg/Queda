package com.luisete.queda.core.domain.quantity

import com.luisete.queda.core.domain.result.DomainError
import com.luisete.queda.core.domain.result.DomainResult
import com.luisete.queda.core.model.quantity.ApproximateLevel
import com.luisete.queda.core.model.quantity.ApproximateQuantity
import com.luisete.queda.core.model.quantity.ExactQuantity
import com.luisete.queda.core.model.quantity.MeasurementUnit
import com.luisete.queda.core.model.quantity.QuantityDimension
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.bigDecimal
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.constant
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class QuantityOperationsTests {
    @Test
    fun `convert grams to kilograms`() {
        val q = ExactQuantity.of("1000", MeasurementUnit.GRAM)
        val result = QuantityOperations.convert(q, MeasurementUnit.KILOGRAM)
        assertTrue(result is DomainResult.Success)
        assertEquals(ExactQuantity.of("1", MeasurementUnit.KILOGRAM), (result as DomainResult.Success).value)
    }

    @Test
    fun `convert kilograms to grams`() {
        val q = ExactQuantity.of("1.5", MeasurementUnit.KILOGRAM)
        val result = QuantityOperations.convert(q, MeasurementUnit.GRAM)
        assertTrue(result is DomainResult.Success)
        assertEquals(ExactQuantity.of("1500", MeasurementUnit.GRAM), (result as DomainResult.Success).value)
    }

    @Test
    fun `convert incompatible dimensions fails`() {
        val q = ExactQuantity.of("1", MeasurementUnit.UNIT)
        val result = QuantityOperations.convert(q, MeasurementUnit.GRAM)
        assertTrue(result is DomainResult.Failure)
        assertEquals(DomainError.IncompatibleQuantityDimensions, (result as DomainResult.Failure).error)
    }

    @Test
    fun `add compatible quantities`() {
        val q1 = ExactQuantity.of("500", MeasurementUnit.GRAM)
        val q2 = ExactQuantity.of("0.5", MeasurementUnit.KILOGRAM)
        val result = QuantityOperations.add(q1, q2)
        assertTrue(result is DomainResult.Success)
        assertEquals(ExactQuantity.of("1000", MeasurementUnit.GRAM), (result as DomainResult.Success).value)
    }

    @Test
    fun `subtract compatible quantities`() {
        val q1 = ExactQuantity.of("1", MeasurementUnit.LITER)
        val q2 = ExactQuantity.of("250", MeasurementUnit.MILLILITER)
        val result = QuantityOperations.subtract(q1, q2)
        assertTrue(result is DomainResult.Success)
        assertEquals(ExactQuantity.of("0.75", MeasurementUnit.LITER), (result as DomainResult.Success).value)
    }

    @Test
    fun `subtract more than available fails`() {
        val q1 = ExactQuantity.of("100", MeasurementUnit.GRAM)
        val q2 = ExactQuantity.of("200", MeasurementUnit.GRAM)
        val result = QuantityOperations.subtract(q1, q2)
        assertTrue(result is DomainResult.Failure)
        assertEquals(DomainError.NegativeQuantity, (result as DomainResult.Failure).error)
    }

    @Test
    fun `consume more than available returns InsufficientQuantity`() {
        val available = ExactQuantity.of("100", MeasurementUnit.GRAM)
        val toConsume = ExactQuantity.of("200", MeasurementUnit.GRAM)
        val result = QuantityOperations.consume(available, toConsume)
        assertTrue(result is DomainResult.Failure)
        assertEquals(DomainError.InsufficientQuantity, (result as DomainResult.Failure).error)
    }

    @Test
    fun `consume approximate level decreasing is valid`() {
        val q = ApproximateQuantity(ApproximateLevel.FULL)
        val result = QuantityOperations.consumeApproximate(q, ApproximateLevel.HIGH)
        assertTrue(result is DomainResult.Success)
        assertEquals(ApproximateLevel.HIGH, (result as DomainResult.Success).value.level)
    }

    @Test
    fun `consume approximate level increasing or same is invalid`() {
        val q = ApproximateQuantity(ApproximateLevel.LOW)
        val result = QuantityOperations.consumeApproximate(q, ApproximateLevel.MEDIUM)
        assertTrue(result is DomainResult.Failure)
        assertEquals(DomainError.ApproximateLevelDidNotDecrease, (result as DomainResult.Failure).error)
    }

    @Test
    fun `property tests for exact quantities`() {
        runBlocking {
            val arbExactQuantity =
                arbitrary {
                    val amount =
                        Arb.bigDecimal(BigDecimal.ZERO, BigDecimal("100000")).bind()
                            .setScale(3, RoundingMode.HALF_UP)
                    val unit =
                        Arb.choice(
                            Arb.constant(MeasurementUnit.UNIT),
                            Arb.constant(MeasurementUnit.GRAM),
                            Arb.constant(MeasurementUnit.KILOGRAM),
                            Arb.constant(MeasurementUnit.MILLILITER),
                            Arb.constant(MeasurementUnit.LITER),
                        ).bind()
                    ExactQuantity.of(amount, unit)
                }

            // Summing zero preserves amount
            checkAll(arbExactQuantity) { q ->
                val zero = ExactQuantity.of(BigDecimal.ZERO, q.unit)
                val result = QuantityOperations.add(q, zero)
                if (result is DomainResult.Success) {
                    result.value shouldBe q
                }
            }

            // Subtraction from self results in zero
            checkAll(arbExactQuantity) { q ->
                val result = QuantityOperations.subtract(q, q)
                if (result is DomainResult.Success) {
                    result.value shouldBe ExactQuantity.of(BigDecimal.ZERO, q.unit)
                }
            }

            // Conversion preserves dimension
            checkAll(arbExactQuantity) { q ->
                val targetUnit =
                    when (q.unit.dimension) {
                        QuantityDimension.MASS -> MeasurementUnit.KILOGRAM
                        QuantityDimension.VOLUME -> MeasurementUnit.LITER
                        QuantityDimension.COUNT -> MeasurementUnit.UNIT
                    }
                val result = QuantityOperations.convert(q, targetUnit)
                if (result is DomainResult.Success) {
                    result.value.unit.dimension shouldBe q.unit.dimension
                }
            }
        }
    }
}
