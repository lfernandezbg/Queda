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
        result = HASH_MULTIPLIER * result + unit.hashCode()
        return result
    }

    override fun toString(): String = "${amount.toPlainString()} ${unit.name}"

    companion object {
        const val MAX_DECIMAL_PLACES = 3

        private const val HASH_MULTIPLIER = 31

        fun isRepresentable(amount: BigDecimal): Boolean {
            if (amount.signum() < 0) return false
            return normalize(amount).scale() <= MAX_DECIMAL_PLACES
        }

        fun of(
            amount: BigDecimal,
            unit: MeasurementUnit,
        ): ExactQuantity {
            require(amount.signum() >= 0) {
                "Amount cannot be negative"
            }

            val normalized = normalize(amount)

            require(normalized.scale() <= MAX_DECIMAL_PLACES) {
                "Amount cannot have more than " +
                    "$MAX_DECIMAL_PLACES decimal places"
            }

            return ExactQuantity(
                amount = normalized,
                unit = unit,
            )
        }

        fun of(
            amount: String,
            unit: MeasurementUnit,
        ): ExactQuantity {
            val decimal =
                amount.toBigDecimalOrNull()
                    ?: throw IllegalArgumentException(
                        "Invalid amount format: $amount",
                    )

            return of(
                amount = decimal,
                unit = unit,
            )
        }

        private fun normalize(amount: BigDecimal): BigDecimal =
            if (amount.signum() == 0) {
                BigDecimal.ZERO
            } else {
                amount.stripTrailingZeros()
            }
    }
}

data class ApproximateQuantity(
    val level: ApproximateLevel,
) : StockQuantity
