package com.luisete.queda.core.domain.inventory

import com.luisete.queda.core.model.id.HouseholdId
import com.luisete.queda.core.model.id.StockItemId
import com.luisete.queda.core.model.inventory.InventoryItem
import com.luisete.queda.core.model.inventory.StockItem
import com.luisete.queda.core.model.product.Product
import com.luisete.queda.core.model.quantity.ExactQuantity
import kotlinx.coroutines.flow.Flow

interface InventoryRepository {
    fun observeExactInventoryItems(householdId: HouseholdId): Flow<List<InventoryItem>>

    suspend fun addExactInventoryItem(
        product: Product,
        stockItem: StockItem,
    ): AddExactItemRepositoryResult

    suspend fun consumeExactQuantity(
        stockItemId: StockItemId,
        toConsume: ExactQuantity,
    ): QuantityMutationResult

    suspend fun correctExactQuantity(
        stockItemId: StockItemId,
        newQuantity: ExactQuantity,
    ): QuantityMutationResult
}
