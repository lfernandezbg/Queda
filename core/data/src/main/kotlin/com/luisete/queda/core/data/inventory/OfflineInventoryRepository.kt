package com.luisete.queda.core.data.inventory

import com.luisete.queda.core.database.AddExactInventoryItemDbResult
import com.luisete.queda.core.database.InventoryDao
import com.luisete.queda.core.domain.inventory.AddExactItemRepositoryResult
import com.luisete.queda.core.domain.inventory.InventoryRepository
import com.luisete.queda.core.model.id.HouseholdId
import com.luisete.queda.core.model.inventory.InventoryItem
import com.luisete.queda.core.model.inventory.StockItem
import com.luisete.queda.core.model.product.Product
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OfflineInventoryRepository
    @Inject
    constructor(
        private val inventoryDao: InventoryDao,
    ) : InventoryRepository {
        override fun observeExactInventoryItems(householdId: HouseholdId): Flow<List<InventoryItem>> =
            inventoryDao.observeExactInventoryItems(householdId.value)
                .map { list -> list.map { it.toDomain() } }

        @Suppress("TooGenericExceptionCaught", "SwallowedException")
        override suspend fun addExactInventoryItem(
            product: Product,
            stockItem: StockItem,
        ): AddExactItemRepositoryResult =
            try {
                when (
                    inventoryDao.addExactInventoryItem(
                        product.toEntity(),
                        stockItem.toEntity(),
                    )
                ) {
                    AddExactInventoryItemDbResult.Added -> AddExactItemRepositoryResult.Added
                    AddExactInventoryItemDbResult.DuplicateProductName ->
                        AddExactItemRepositoryResult.DuplicateProductName
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AddExactItemRepositoryResult.StorageFailure
            }
    }
