package com.luisete.queda.core.domain.quantity

import com.luisete.queda.core.domain.result.DomainError
import com.luisete.queda.core.domain.result.DomainResult
import com.luisete.queda.core.domain.result.Failure
import com.luisete.queda.core.domain.result.Success
import com.luisete.queda.core.model.quantity.ApproximateLevel
import com.luisete.queda.core.model.quantity.ApproximateQuantity
import com.luisete.queda.core.model.quantity.ExactQuantity
import com.luisete.queda.core.model.quantity.MeasurementUnit
import com.luisete.queda.core.model.quantity.QuantityDimension
import java.math.BigDecimal

@Suppress("TooManyFunctions")
object QuantityOperations {
    private val BASE_FACTOR = BigDecimal("1000")

    fun convert(
        quantity: ExactQuantity,
        targetUnit: MeasurementUnit,
    ): DomainResult<ExactQuantity> =
        when {
            quantity.unit == targetUnit -> Success(quantity)
            quantity.unit.dimension != targetUnit.dimension ->
                Failure(
                    DomainError.IncompatibleQuantityDimensions,
                )
            else -> {
                val amountInBase =
                    toBaseUnitAmount(
                        amount = quantity.amount,
                        unit = quantity.unit,
                    )
                val targetAmount =
                    fromBaseUnitAmount(
                        amountInBase = amountInBase,
                        targetUnit = targetUnit,
                    )
                createExactResult(
                    amount = targetAmount,
                    unit = targetUnit,
                )
            }
        }

    fun add(
        left: ExactQuantity,
        right: ExactQuantity,
    ): DomainResult<ExactQuantity> {
        if (left.unit.dimension != right.unit.dimension) {
            return Failure(
                DomainError.IncompatibleQuantityDimensions,
            )
        }

        val resultInBase =
            toBaseUnitAmount(left.amount, left.unit)
                .add(
                    toBaseUnitAmount(
                        right.amount,
                        right.unit,
                    ),
                )

        return resolveMixedResult(
            amountInBase = resultInBase,
            preferredUnit = left.unit,
        )
    }

    fun subtract(
        left: ExactQuantity,
        right: ExactQuantity,
    ): DomainResult<ExactQuantity> =
        when {
            left.unit.dimension != right.unit.dimension ->
                Failure(
                    DomainError.IncompatibleQuantityDimensions,
                )
            else -> {
                val resultInBase =
                    toBaseUnitAmount(left.amount, left.unit)
                        .subtract(toBaseUnitAmount(right.amount, right.unit))

                if (resultInBase.signum() < 0) {
                    Failure(DomainError.NegativeQuantity)
                } else {
                    resolveMixedResult(
                        amountInBase = resultInBase,
                        preferredUnit = left.unit,
                    )
                }
            }
        }

    fun consume(
        available: ExactQuantity,
        toConsume: ExactQuantity,
    ): DomainResult<ExactQuantity> =
        when {
            available.unit.dimension != toConsume.unit.dimension ->
                Failure(
                    DomainError.IncompatibleQuantityDimensions,
                )
            else -> {
                val availableInBase =
                    toBaseUnitAmount(
                        available.amount,
                        available.unit,
                    )
                val consumptionInBase =
                    toBaseUnitAmount(
                        toConsume.amount,
                        toConsume.unit,
                    )

                if (consumptionInBase > availableInBase) {
                    Failure(DomainError.InsufficientQuantity)
                } else {
                    resolveMixedResult(
                        amountInBase = availableInBase.subtract(consumptionInBase),
                        preferredUnit = available.unit,
                    )
                }
            }
        }

    fun correct(
        current: ExactQuantity,
        newAmount: BigDecimal,
        newUnit: MeasurementUnit,
    ): DomainResult<ExactQuantity> {
        if (current.unit.dimension != newUnit.dimension) {
            return Failure(
                DomainError.IncompatibleQuantityDimensions,
            )
        }

        return createExactResult(
            amount = newAmount,
            unit = newUnit,
        )
    }

    fun consumeApproximate(
        current: ApproximateQuantity,
        targetLevel: ApproximateLevel,
    ): DomainResult<ApproximateQuantity> {
        if (targetLevel.order >= current.level.order) {
            return Failure(
                DomainError.ApproximateLevelDidNotDecrease,
            )
        }

        return Success(
            ApproximateQuantity(targetLevel),
        )
    }

    fun correctApproximate(targetLevel: ApproximateLevel): DomainResult<ApproximateQuantity> =
        Success(
            ApproximateQuantity(targetLevel),
        )

    private fun createExactResult(
        amount: BigDecimal,
        unit: MeasurementUnit,
    ): DomainResult<ExactQuantity> =
        when {
            amount.signum() < 0 -> Failure(DomainError.NegativeQuantity)
            !ExactQuantity.isRepresentable(amount) -> Failure(DomainError.TooManyDecimalPlaces)
            else ->
                Success(
                    ExactQuantity.of(
                        amount = amount,
                        unit = unit,
                    ),
                )
        }

    private fun resolveMixedResult(
        amountInBase: BigDecimal,
        preferredUnit: MeasurementUnit,
    ): DomainResult<ExactQuantity> {
        val amountInPreferred =
            fromBaseUnitAmount(
                amountInBase = amountInBase,
                targetUnit = preferredUnit,
            )

        if (
            ExactQuantity.isRepresentable(
                amountInPreferred,
            )
        ) {
            return Success(
                ExactQuantity.of(
                    amount = amountInPreferred,
                    unit = preferredUnit,
                ),
            )
        }

        val baseUnit =
            baseUnitFor(preferredUnit.dimension)

        return createExactResult(
            amount = amountInBase,
            unit = baseUnit,
        )
    }

    private fun toBaseUnitAmount(
        amount: BigDecimal,
        unit: MeasurementUnit,
    ): BigDecimal =
        when (unit) {
            MeasurementUnit.KILOGRAM,
            MeasurementUnit.LITER,
            -> amount.multiply(BASE_FACTOR)

            else -> amount
        }

    private fun fromBaseUnitAmount(
        amountInBase: BigDecimal,
        targetUnit: MeasurementUnit,
    ): BigDecimal =
        when (targetUnit) {
            MeasurementUnit.KILOGRAM,
            MeasurementUnit.LITER,
            -> amountInBase.divide(BASE_FACTOR)

            else -> amountInBase
        }

    private fun baseUnitFor(dimension: QuantityDimension): MeasurementUnit =
        when (dimension) {
            QuantityDimension.COUNT ->
                MeasurementUnit.UNIT

            QuantityDimension.MASS ->
                MeasurementUnit.GRAM

            QuantityDimension.VOLUME ->
                MeasurementUnit.MILLILITER
        }
}
