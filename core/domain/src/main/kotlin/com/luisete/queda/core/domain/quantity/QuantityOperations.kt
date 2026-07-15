package com.luisete.queda.core.domain.quantity

import com.luisete.queda.core.domain.result.DomainError
import com.luisete.queda.core.domain.result.DomainResult
import com.luisete.queda.core.model.quantity.ApproximateLevel
import com.luisete.queda.core.model.quantity.ApproximateQuantity
import com.luisete.queda.core.model.quantity.ExactQuantity
import com.luisete.queda.core.model.quantity.MeasurementUnit
import java.math.BigDecimal

@Suppress("SwallowedException", "ReturnCount", "MaxLineLength")
object QuantityOperations {
    private val KILO_FACTOR = BigDecimal("1000")

    fun convert(
        quantity: ExactQuantity,
        targetUnit: MeasurementUnit,
    ): DomainResult<ExactQuantity> {
        if (quantity.unit == targetUnit) return DomainResult.Success(quantity)
        if (quantity.unit.dimension != targetUnit.dimension) {
            return DomainResult.Failure(DomainError.IncompatibleQuantityDimensions)
        }

        val amount = quantity.amount
        val convertedAmount =
            when {
                quantity.unit == MeasurementUnit.GRAM && targetUnit == MeasurementUnit.KILOGRAM ->
                    amount.divide(KILO_FACTOR)

                quantity.unit == MeasurementUnit.KILOGRAM && targetUnit == MeasurementUnit.GRAM ->
                    amount.multiply(KILO_FACTOR)

                quantity.unit == MeasurementUnit.MILLILITER && targetUnit == MeasurementUnit.LITER ->
                    amount.divide(KILO_FACTOR)

                quantity.unit == MeasurementUnit.LITER && targetUnit == MeasurementUnit.MILLILITER ->
                    amount.multiply(KILO_FACTOR)

                else -> return DomainResult.Failure(DomainError.IncompatibleQuantityDimensions)
            }

        return try {
            DomainResult.Success(ExactQuantity.of(convertedAmount, targetUnit))
        } catch (e: IllegalArgumentException) {
            DomainResult.Failure(DomainError.TooManyDecimalPlaces)
        } catch (e: ArithmeticException) {
            // In case of non-terminating decimal expansion, though not possible with 1000
            DomainResult.Failure(DomainError.TooManyDecimalPlaces)
        }
    }

    fun add(
        q1: ExactQuantity,
        q2: ExactQuantity,
    ): DomainResult<ExactQuantity> {
        val convertedQ2 = convert(q2, q1.unit)
        return when (convertedQ2) {
            is DomainResult.Success -> {
                try {
                    DomainResult.Success(ExactQuantity.of(q1.amount.add(convertedQ2.value.amount), q1.unit))
                } catch (e: IllegalArgumentException) {
                    DomainResult.Failure(DomainError.TooManyDecimalPlaces)
                }
            }

            is DomainResult.Failure -> convertedQ2
        }
    }

    fun subtract(
        q1: ExactQuantity,
        q2: ExactQuantity,
    ): DomainResult<ExactQuantity> {
        val convertedQ2 = convert(q2, q1.unit)
        return when (convertedQ2) {
            is DomainResult.Success -> {
                val resultAmount = q1.amount.subtract(convertedQ2.value.amount)
                if (resultAmount < BigDecimal.ZERO) {
                    DomainResult.Failure(DomainError.NegativeQuantity)
                } else {
                    try {
                        DomainResult.Success(ExactQuantity.of(resultAmount, q1.unit))
                    } catch (e: IllegalArgumentException) {
                        DomainResult.Failure(DomainError.TooManyDecimalPlaces)
                    }
                }
            }

            is DomainResult.Failure -> convertedQ2
        }
    }

    fun consume(
        available: ExactQuantity,
        toConsume: ExactQuantity,
    ): DomainResult<ExactQuantity> {
        val result = subtract(available, toConsume)
        return if (result is DomainResult.Failure && result.error == DomainError.NegativeQuantity) {
            DomainResult.Failure(DomainError.InsufficientQuantity)
        } else {
            result
        }
    }

    fun correct(
        newAmount: BigDecimal,
        newUnit: MeasurementUnit,
    ): DomainResult<ExactQuantity> {
        return try {
            DomainResult.Success(ExactQuantity.of(newAmount, newUnit))
        } catch (e: IllegalArgumentException) {
            if (newAmount < BigDecimal.ZERO) {
                DomainResult.Failure(DomainError.NegativeQuantity)
            } else {
                DomainResult.Failure(DomainError.TooManyDecimalPlaces)
            }
        }
    }

    fun consumeApproximate(
        current: ApproximateQuantity,
        targetLevel: ApproximateLevel,
    ): DomainResult<ApproximateQuantity> {
        if (targetLevel.order >= current.level.order) {
            return DomainResult.Failure(DomainError.ApproximateLevelDidNotDecrease)
        }
        return DomainResult.Success(ApproximateQuantity(targetLevel))
    }

    fun correctApproximate(newLevel: ApproximateLevel): DomainResult<ApproximateQuantity> {
        return DomainResult.Success(ApproximateQuantity(newLevel))
    }
}
