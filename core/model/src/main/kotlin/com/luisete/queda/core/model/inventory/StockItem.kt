package com.luisete.queda.core.model.inventory

import com.luisete.queda.core.model.id.HouseholdId
import com.luisete.queda.core.model.id.ProductId
import com.luisete.queda.core.model.id.StockItemId
import com.luisete.queda.core.model.quantity.ExactQuantity

data class StockItem(
    val id: StockItemId,
    val householdId: HouseholdId,
    val productId: ProductId,
    val quantity: ExactQuantity,
)
