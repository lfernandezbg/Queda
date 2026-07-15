package com.luisete.queda.core.model.quantity

import java.math.BigDecimal

sealed interface StockQuantity

class ExactQuantity private constructor(
    val amount: BigDecimal,
    val unit: MeasurementUnit,
) : StockQuantity {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExactQuantity) return false
        return amount.compareTo(other.amount) == 0 && unit == other.unit
    }

    override fun hashCode(): Int {
        var result = amount.stripTrailingZeros().hashCode()
        result = 31 * result + unit.hashCode()
        return result
    }

    override fun toString(): String = "${amount.toPlainString()} ${unit.name}"

    companion object {
        private const val MAX_DECIMALS = 3

        fun of(
            amount: BigDecimal,
            unit: MeasurementUnit,
        ): ExactQuantity {
            require(amount >= BigDecimal.ZERO) { "Amount cannot be negative" }

            val normalized =
                if (amount.compareTo(BigDecimal.ZERO) == 0) {
                    BigDecimal.ZERO.setScale(0)
                } else {
                    amount.stripTrailingZeros()
                }

            require(normalized.scale() <= MAX_DECIMALS) {
                "Amount cannot have more than $MAX_DECIMALS decimal places"
            }

            return ExactQuantity(normalized, unit)
        }

        fun of(
            amount: String,
            unit: MeasurementUnit,
        ): ExactQuantity {
            return try {
                of(BigDecimal(amount), unit)
            } catch (e: NumberFormatException) {
                throw IllegalArgumentException("Invalid amount format: $amount", e)
            }
        }
    }
}

data class ApproximateQuantity(
    val level: ApproximateLevel,
) : StockQuantity
