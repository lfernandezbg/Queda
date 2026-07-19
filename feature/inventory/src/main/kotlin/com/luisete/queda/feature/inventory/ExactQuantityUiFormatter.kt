package com.luisete.queda.feature.inventory

import com.luisete.queda.core.model.quantity.ExactQuantity

object ExactQuantityUiFormatter {
    fun format(quantity: ExactQuantity): String {
        return quantity.amount.toPlainString().replace('.', ',')
    }
}
