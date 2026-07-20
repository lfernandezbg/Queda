package com.luisete.queda.core.data.inventory

import com.luisete.queda.core.database.AddExactInventoryItemDbResult
import com.luisete.queda.core.database.InventoryDao
import com.luisete.queda.core.database.InventoryItemProjection
import com.luisete.queda.core.database.ProductEntity
import com.luisete.queda.core.database.StockItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeInventoryDao : InventoryDao {
    private val flow = MutableSharedFlow<List<InventoryItemProjection>>(replay = 1)
    val products = mutableListOf<ProductEntity>()
    val stocks = mutableListOf<StockItemEntity>()
    var throwOnInsert: Exception? = null
    val observedHouseholdIds = mutableListOf<String>()

    fun emit(items: List<InventoryItemProjection>) {
        flow.tryEmit(items)
    }

    override fun observeExactInventoryItems(householdId: String): Flow<List<InventoryItemProjection>> {
        observedHouseholdIds.add(householdId)
        return flow
    }

    override suspend fun countProductsWithName(
        householdId: String,
        normalizedName: String,
    ): Int {
        return products.count { it.householdId == householdId && it.normalizedName == normalizedName }
    }

    override suspend fun insertProduct(product: ProductEntity) {
        products.add(product)
    }

    override suspend fun insertStockItem(stockItem: StockItemEntity) {
        stocks.add(stockItem)
    }

    override suspend fun getStockItemById(id: String): StockItemEntity? {
        return stocks.find { it.id == id }
    }

    override suspend fun updateStockItemQuantity(
        id: String,
        amount: String,
        unit: String,
    ) {
        val index = stocks.indexOfFirst { it.id == id }
        if (index != -1) {
            stocks[index] = stocks[index].copy(quantityAmount = amount, quantityUnit = unit)
        }
    }

    override suspend fun addExactInventoryItem(
        product: ProductEntity,
        stockItem: StockItemEntity,
    ): AddExactInventoryItemDbResult {
        throwOnInsert?.let { throw it }
        if (countProductsWithName(product.householdId, product.normalizedName) > 0) {
            return AddExactInventoryItemDbResult.DuplicateProductName
        }
        insertProduct(product)
        insertStockItem(stockItem)
        return AddExactInventoryItemDbResult.Added
    }
}
