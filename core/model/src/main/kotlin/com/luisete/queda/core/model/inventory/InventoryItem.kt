package com.luisete.queda.core.model.inventory

import com.luisete.queda.core.model.product.Product

data class InventoryItem(
    val product: Product,
    val stockItem: StockItem,
) {
    init {
        require(product.householdId == stockItem.householdId) {
            "Product and StockItem must belong to the same household"
        }
        require(product.id == stockItem.productId) {
            "StockItem must reference the correct Product ID"
        }
    }
}
