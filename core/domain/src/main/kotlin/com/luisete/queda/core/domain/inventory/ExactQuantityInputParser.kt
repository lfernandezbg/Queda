package com.luisete.queda.core.domain.inventory

import com.luisete.queda.core.model.quantity.ExactQuantity
import com.luisete.queda.core.model.quantity.MeasurementUnit
import java.math.BigDecimal

object ExactQuantityInputParser {
    @Suppress("ReturnCount", "ComplexMethod", "ComplexCondition")
    fun parse(
        rawAmount: String,
        unit: MeasurementUnit,
    ): ExactQuantityInputResult {
        var temp = rawAmount
        while (temp.startsWith(' ')) {
            temp = temp.substring(1)
        }
        while (temp.endsWith(' ')) {
            temp = temp.substring(0, temp.length - 1)
        }

        if (temp.isEmpty()) return ExactQuantityInputResult.Blank

        if (temp.any { it == ' ' || it == '\t' || it == '\n' || it == '\r' }) {
            return ExactQuantityInputResult.InvalidFormat
        }

        val hasPoint = temp.contains('.')
        val hasComma = temp.contains(',')

        if (hasPoint && hasComma) return ExactQuantityInputResult.InvalidFormat

        if (temp.any { !it.isDigit() && it != '.' && it != ',' }) return ExactQuantityInputResult.InvalidFormat

        if (hasPoint || hasComma) {
            val sep = if (hasPoint) '.' else ','
            val firstIndex = temp.indexOf(sep)
            val lastIndex = temp.lastIndexOf(sep)
            if (firstIndex != lastIndex) return ExactQuantityInputResult.InvalidFormat
            if (firstIndex == 0 || firstIndex == temp.length - 1) return ExactQuantityInputResult.InvalidFormat
        }

        val normalized = temp.replace(',', '.')
        val amount = BigDecimal(normalized).stripTrailingZeros()

        if (amount.signum() <= 0) return ExactQuantityInputResult.NotPositive
        if (amount.scale() > ExactQuantity.MAX_DECIMAL_PLACES) return ExactQuantityInputResult.TooManyDecimalPlaces

        return ExactQuantityInputResult.Success(ExactQuantity.of(amount, unit))
    }
}

sealed interface ExactQuantityInputResult {
    data class Success(val quantity: ExactQuantity) : ExactQuantityInputResult

    data object Blank : ExactQuantityInputResult

    data object InvalidFormat : ExactQuantityInputResult

    data object NotPositive : ExactQuantityInputResult

    data object TooManyDecimalPlaces : ExactQuantityInputResult
}
