package com.luisete.queda.core.domain.quantity

import com.luisete.queda.core.domain.result.Success
import com.luisete.queda.core.model.quantity.ApproximateLevel
import com.luisete.queda.core.model.quantity.ApproximateQuantity
import com.luisete.queda.core.model.quantity.ExactQuantity
import com.luisete.queda.core.model.quantity.MeasurementUnit
import com.luisete.queda.core.model.quantity.QuantityDimension
import io.kotest.common.ExperimentalKotest
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalKotest::class)
class QuantityOperationsPropertyTests {
    private val config = PropTestConfig(iterations = 1000, seed = 20260715L)

    private val amountArb: Arb<BigDecimal> =
        arbitrary {
            val unscaledValue = Arb.int(0, 1_000_000_000).bind().toLong()
            val scale = Arb.int(0, 3).bind()
            BigDecimal.valueOf(unscaledValue, scale)
        }

    private val unitArb: Arb<MeasurementUnit> = Arb.enum<MeasurementUnit>()

    private val exactQuantityArb: Arb<ExactQuantity> =
        arbitrary {
            ExactQuantity.of(amountArb.bind(), unitArb.bind())
        }

    @Test
    fun addZeroIsIdentityForEveryUnit() =
        runTest {
            checkAll(config, exactQuantityArb) { q ->
                val zero = ExactQuantity.of(BigDecimal.ZERO, q.unit)
                QuantityOperations.add(q, zero).successValue() shouldBe q
            }
        }

    @Test
    fun subtractSelfProducesZeroForEveryUnit() =
        runTest {
            checkAll(config, exactQuantityArb) { q ->
                QuantityOperations.subtract(q, q).successValue().amount.signum() shouldBe 0
            }
        }

    @Test
    fun validSubtractionNeverProducesNegativeValue() =
        runTest {
            val pairArb =
                arbitrary {
                    val q1 = exactQuantityArb.bind()
                    val q1Base = toBase(q1.amount, q1.unit)
                    val unscaled = Arb.int(0, 1_000_000_000).bind().toLong()
                    val scale = Arb.int(0, 3).bind()
                    val q2AmountInBase = BigDecimal.valueOf(unscaled, scale).min(q1Base)
                    val q2 = ExactQuantity.of(q2AmountInBase, baseUnitFor(q1.unit.dimension))
                    q1 to q2
                }
            checkAll(config, pairArb) { (q1, q2) ->
                val signum = QuantityOperations.subtract(q1, q2).successValue().amount.signum()
                signum shouldBeGreaterOrEqualTo 0
            }
        }

    @Test
    fun validConsumptionNeverProducesNegativeValue() =
        runTest {
            val nonZeroQuantityArb =
                exactQuantityArb.map {
                    if (it.amount.signum() == 0) ExactQuantity.of(BigDecimal.ONE, it.unit) else it
                }
            checkAll(config, nonZeroQuantityArb) { q ->
                val half =
                    q.amount.divide(
                        BigDecimal.valueOf(2),
                        ExactQuantity.MAX_DECIMAL_PLACES,
                        java.math.RoundingMode.DOWN,
                    )
                if (half.signum() > 0) {
                    val toConsume = ExactQuantity.of(half, q.unit)
                    QuantityOperations.consume(q, toConsume).successValue().amount.signum() shouldBe 1
                }
            }
        }

    @Test
    fun validConsumptionNeverIncreasesAvailableQuantity() =
        runTest {
            val pairArb =
                arbitrary {
                    val q1 = exactQuantityArb.bind()
                    val q1Base = toBase(q1.amount, q1.unit)
                    if (q1Base.signum() <= 0) {
                        return@arbitrary (q1 to ExactQuantity.of(BigDecimal.ONE, q1.unit))
                    }
                    val unscaled = Arb.int(1, 1_000_000_000).bind().toLong()
                    val scale = Arb.int(0, 3).bind()
                    val q2AmountInBase =
                        BigDecimal.valueOf(
                            unscaled,
                            scale,
                        ).min(q1Base.divide(BigDecimal.valueOf(2), 3, java.math.RoundingMode.DOWN))
                    val q2 =
                        if (q2AmountInBase.signum() <= 0) {
                            ExactQuantity.of(
                                BigDecimal.ONE,
                                baseUnitFor(q1.unit.dimension),
                            )
                        } else {
                            ExactQuantity.of(q2AmountInBase, baseUnitFor(q1.unit.dimension))
                        }
                    q1 to q2
                }
            checkAll(config, pairArb) { (q1, q2) ->
                val q1Base = toBase(q1.amount, q1.unit)
                val q2Base = toBase(q2.amount, q2.unit)
                if (q1.amount.signum() > 0 && q2.amount.signum() > 0 && q2Base < q1Base) {
                    val result = QuantityOperations.consume(q1, q2).successValue()
                    val resBase = toBase(result.amount, result.unit)
                    resBase shouldBeLessOrEqualTo q1Base
                }
            }
        }

    @Test
    fun gramKilogramRoundTripPreservesValueWhenRepresentable() =
        runTest {
            val gramIntegerArb = Arb.int(0, 1_000_000).map { BigDecimal.valueOf(it.toLong()) }
            checkAll(config, gramIntegerArb) { amount ->
                val q = ExactQuantity.of(amount, MeasurementUnit.GRAM)
                val toKg = QuantityOperations.convert(q, MeasurementUnit.KILOGRAM).successValue()
                val backToG = QuantityOperations.convert(toKg, MeasurementUnit.GRAM).successValue()
                backToG shouldBe q
            }
        }

    @Test
    fun milliliterLiterRoundTripPreservesValueWhenRepresentable() =
        runTest {
            val mlIntegerArb = Arb.int(0, 1_000_000).map { BigDecimal.valueOf(it.toLong()) }
            checkAll(config, mlIntegerArb) { amount ->
                val q = ExactQuantity.of(amount, MeasurementUnit.MILLILITER)
                val toL = QuantityOperations.convert(q, MeasurementUnit.LITER).successValue()
                val backToMl = QuantityOperations.convert(toL, MeasurementUnit.MILLILITER).successValue()
                backToMl shouldBe q
            }
        }

    @Test
    fun successfulConversionPreservesDimension() =
        runTest {
            val compatibleUnitArb =
                arbitrary {
                    val unit1 = Arb.enum<MeasurementUnit>().bind()
                    val unit2 = Arb.enum<MeasurementUnit>().bind()
                    val finalUnit2 = if (unit1.dimension == unit2.dimension) unit2 else unit1

                    val amount =
                        if (unit1 == MeasurementUnit.GRAM || unit1 == MeasurementUnit.MILLILITER) {
                            if (finalUnit2 == MeasurementUnit.KILOGRAM || finalUnit2 == MeasurementUnit.LITER) {
                                BigDecimal.valueOf(Arb.int(0, 1_000_000).bind().toLong())
                            } else {
                                amountArb.bind()
                            }
                        } else {
                            amountArb.bind()
                        }

                    ExactQuantity.of(amount, unit1) to finalUnit2
                }
            checkAll(config, compatibleUnitArb) { (q, targetUnit) ->
                val res = QuantityOperations.convert(q, targetUnit).successValue()
                res.unit.dimension shouldBe q.unit.dimension
            }
        }

    @Test
    fun addThenSubtractSameOperandRestoresCanonicalValue() =
        runTest {
            val smallAmountArb = Arb.int(0, 100_000).map { BigDecimal.valueOf(it.toLong(), 3) }
            checkAll(config, exactQuantityArb, smallAmountArb) { q, opAmount ->
                val op = ExactQuantity.of(opAmount, q.unit)
                val added = QuantityOperations.add(q, op).successValue()
                val subtracted = QuantityOperations.subtract(added, op).successValue()
                val b1 = toBase(subtracted.amount, subtracted.unit)
                val b2 = toBase(q.amount, q.unit)
                b1.subtract(b2).signum() shouldBe 0
            }
        }

    @Test
    fun addIsCommutativeWhenComparedInBaseUnit() =
        runTest {
            val pairArb =
                arbitrary {
                    val unit1 = unitArb.bind()
                    val unit2 = Arb.enum<MeasurementUnit>().bind()
                    val finalUnit2 = if (unit1.dimension == unit2.dimension) unit2 else unit1
                    val val1 = Arb.int(0, 500_000_000).bind().toLong()
                    val val2 = Arb.int(0, 500_000_000).bind().toLong()
                    val q1 = ExactQuantity.of(BigDecimal.valueOf(val1, 3), unit1)
                    val q2 = ExactQuantity.of(BigDecimal.valueOf(val2, 3), finalUnit2)
                    q1 to q2
                }
            checkAll(config, pairArb) { (q1, q2) ->
                val res1 = QuantityOperations.add(q1, q2).successValue()
                val res2 = QuantityOperations.add(q2, q1).successValue()
                val b1 = toBase(res1.amount, res1.unit)
                val b2 = toBase(res2.amount, res2.unit)
                b1.subtract(b2).signum() shouldBe 0
            }
        }

    @Test
    fun operationsDoNotMutateInputObjects() =
        runTest {
            checkAll(config, exactQuantityArb) { q ->
                val amountBefore = q.amount
                val unitBefore = q.unit
                QuantityOperations.convert(q, q.unit)
                QuantityOperations.add(q, q)
                QuantityOperations.subtract(q, q)
                QuantityOperations.consume(q, q)
                QuantityOperations.correct(q, q.amount, q.unit)

                q.amount shouldBe amountBefore
                q.unit shouldBe unitBefore

                val aq = ApproximateQuantity(ApproximateLevel.MEDIUM)
                QuantityOperations.consumeApproximate(aq, ApproximateLevel.LOW)
                QuantityOperations.correctApproximate(ApproximateLevel.HIGH)
                aq.level shouldBe ApproximateLevel.MEDIUM
            }
        }

    @Test
    fun approximateConsumptionAlwaysDecreasesOrder() =
        runTest {
            val levelPairArb =
                arbitrary {
                    val l1 =
                        Arb.choice(
                            Arb.constant(ApproximateLevel.FULL),
                            Arb.constant(ApproximateLevel.HIGH),
                            Arb.constant(ApproximateLevel.MEDIUM),
                            Arb.constant(ApproximateLevel.LOW),
                            Arb.constant(ApproximateLevel.ALMOST_EMPTY),
                        ).bind()
                    val l2 = Arb.enum<ApproximateLevel>().bind()
                    if (l2.order < l1.order) l1 to l2 else l1 to ApproximateLevel.EMPTY
                }
            checkAll(config, levelPairArb) { (l1, l2) ->
                val res = QuantityOperations.consumeApproximate(ApproximateQuantity(l1), l2).successValue()
                res.level.order shouldBeLess l1.order
            }
        }

    @Test
    fun approximateCorrectionPreservesRequestedTarget() =
        runTest {
            checkAll(config, Arb.enum<ApproximateLevel>()) { level ->
                QuantityOperations.correctApproximate(level).successValue().level shouldBe level
            }
        }

    @Test
    fun baseUnitFallbackNeverLosesPrecision() =
        runTest {
            val fractionalAmountArb =
                arbitrary {
                    BigDecimal.valueOf(Arb.int(1, 999).bind().toLong(), 3)
                }
            val fallbackCaseArb: Arb<Triple<ExactQuantity, ExactQuantity, MeasurementUnit>> =
                arbitrary {
                    val frac = fractionalAmountArb.bind()
                    if (Arb.int(0, 1).bind() == 0) {
                        val q1 = ExactQuantity.of(BigDecimal.ONE, MeasurementUnit.KILOGRAM)
                        val q2 = ExactQuantity.of(frac, MeasurementUnit.GRAM)
                        Triple(q1, q2, MeasurementUnit.GRAM)
                    } else {
                        val q1 = ExactQuantity.of(BigDecimal.ONE, MeasurementUnit.LITER)
                        val q2 = ExactQuantity.of(frac, MeasurementUnit.MILLILITER)
                        Triple(q1, q2, MeasurementUnit.MILLILITER)
                    }
                }

            checkAll(config, fallbackCaseArb) { (left, right, expectedUnit) ->
                val result = QuantityOperations.add(left, right).successValue()
                result.unit shouldBe expectedUnit
                val expected = BigDecimal("1000").add(right.amount)
                result.amount.compareTo(expected) shouldBe 0
            }
        }

    @Test
    fun successfulResultsAlwaysRespectMaximumThreeDecimals() =
        runTest {
            val operationArb = Arb.int(0, 4)
            checkAll(config, exactQuantityArb, operationArb) { quantity: ExactQuantity, operation: Int ->
                val zero = ExactQuantity.of(BigDecimal.ZERO, quantity.unit)
                val result =
                    when (operation) {
                        0 -> QuantityOperations.convert(quantity, quantity.unit)
                        1 -> QuantityOperations.add(quantity, zero)
                        2 -> QuantityOperations.subtract(quantity, zero)
                        3 -> {
                            val availableInBase = toBase(quantity.amount, quantity.unit)
                            if (availableInBase.signum() > 0) {
                                val half =
                                    availableInBase.divide(
                                        BigDecimal.valueOf(2),
                                        3,
                                        java.math.RoundingMode.DOWN,
                                    )
                                if (half.signum() > 0) {
                                    QuantityOperations.consume(
                                        quantity,
                                        ExactQuantity.of(half, baseUnitFor(quantity.unit.dimension)),
                                    )
                                } else {
                                    Success(quantity)
                                }
                            } else {
                                Success(quantity)
                            }
                        }

                        else -> {
                            if (quantity.amount.signum() <= 0) {
                                QuantityOperations.correct(quantity, BigDecimal.ONE, quantity.unit)
                            } else {
                                val differentAmount = quantity.amount.add(BigDecimal.ONE)
                                QuantityOperations.correct(quantity, differentAmount, quantity.unit)
                            }
                        }
                    }.successValue()
                result.amount.scale() shouldBeLessOrEqualTo ExactQuantity.MAX_DECIMAL_PLACES
            }
        }

    @Test
    fun subtractionResultIsNeverNegative() =
        runTest {
            val pairArb =
                arbitrary {
                    val q1 = exactQuantityArb.bind()
                    val q1Base = toBase(q1.amount, q1.unit)
                    val unscaled = Arb.int(0, 1_000_000_000).bind().toLong()
                    val scale = Arb.int(0, 3).bind()
                    val q2AmountInBase = BigDecimal.valueOf(unscaled, scale).min(q1Base)
                    val q2 = ExactQuantity.of(q2AmountInBase, baseUnitFor(q1.unit.dimension))
                    q1 to q2
                }
            checkAll(config, pairArb) { (q1, q2) ->
                val result = QuantityOperations.subtract(q1, q2).successValue()
                result.amount.signum() shouldBeGreaterOrEqualTo 0
            }
        }

    @Test
    fun consumptionResultIsAlwaysPositive() =
        runTest {
            val pairArb =
                arbitrary {
                    val q1 = exactQuantityArb.bind()
                    val q1Base = toBase(q1.amount, q1.unit)
                    if (q1Base.signum() <= 0) return@arbitrary null
                    val half = q1Base.divide(BigDecimal.valueOf(2), 3, java.math.RoundingMode.DOWN)
                    if (half.signum() <= 0) return@arbitrary null
                    val toConsume = ExactQuantity.of(half, baseUnitFor(q1.unit.dimension))
                    q1 to toConsume
                }
            checkAll(config, pairArb) { pair ->
                if (pair != null) {
                    val (q1, q2) = pair
                    QuantityOperations.consume(q1, q2).successValue().amount.signum() shouldBe 1
                }
            }
        }

    private fun toBase(
        amount: BigDecimal,
        unit: MeasurementUnit,
    ): BigDecimal =
        if (unit == MeasurementUnit.KILOGRAM || unit == MeasurementUnit.LITER) {
            amount.multiply(BigDecimal.valueOf(1000))
        } else {
            amount
        }

    private fun baseUnitFor(dimension: QuantityDimension) =
        when (dimension) {
            QuantityDimension.MASS -> MeasurementUnit.GRAM
            QuantityDimension.VOLUME -> MeasurementUnit.MILLILITER
            QuantityDimension.COUNT -> MeasurementUnit.UNIT
        }

    private infix fun Int.shouldBeGreaterOrEqualTo(other: Int) {
        if (this >= other) return
        throw AssertionError("$this < $other")
    }

    private infix fun Int.shouldBeLess(other: Int) {
        if (this < other) return
        throw AssertionError("$this >= $other")
    }

    private infix fun BigDecimal.shouldBeLessOrEqualTo(other: BigDecimal) {
        if (this <= other) return
        throw AssertionError("$this > $other")
    }

    private infix fun Int.shouldBeLessOrEqualTo(other: Int) {
        if (this <= other) return
        throw AssertionError("$this > $other")
    }
}
