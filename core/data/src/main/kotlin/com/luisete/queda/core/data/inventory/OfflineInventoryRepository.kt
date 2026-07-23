package com.luisete.queda.core.data.inventory

import androidx.room.withTransaction
import com.luisete.queda.core.database.AddExactInventoryItemDbResult
import com.luisete.queda.core.database.InventoryDao
import com.luisete.queda.core.database.QuedaDatabase
import com.luisete.queda.core.domain.inventory.AddExactItemRepositoryResult
import com.luisete.queda.core.domain.inventory.FindItemByBarcodeResult
import com.luisete.queda.core.domain.inventory.InventoryRepository
import com.luisete.queda.core.domain.inventory.QuantityMutationResult
import com.luisete.queda.core.domain.quantity.QuantityOperations
import com.luisete.queda.core.domain.result.DomainError
import com.luisete.queda.core.domain.result.DomainResult
import com.luisete.queda.core.domain.result.Failure
import com.luisete.queda.core.domain.result.Success
import com.luisete.queda.core.model.barcode.Barcode
import com.luisete.queda.core.model.id.HouseholdId
import com.luisete.queda.core.model.id.StockItemId
import com.luisete.queda.core.model.inventory.InventoryItem
import com.luisete.queda.core.model.inventory.StockItem
import com.luisete.queda.core.model.inventory.StockTrackingMode
import com.luisete.queda.core.model.product.Product
import com.luisete.queda.core.model.quantity.ExactQuantity
import com.luisete.queda.core.model.quantity.MeasurementUnit
import com.luisete.queda.core.model.quantity.PresenceQuantity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import javax.inject.Inject

class OfflineInventoryRepository
    @Inject
    constructor(
        private val database: QuedaDatabase,
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
                    AddExactInventoryItemDbResult.DuplicateBarcode ->
                        AddExactItemRepositoryResult.DuplicateBarcode
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AddExactItemRepositoryResult.StorageFailure
            }

        override suspend fun consumeExactQuantity(
            stockItemId: StockItemId,
            toConsume: ExactQuantity,
        ): QuantityMutationResult =
            mutateQuantity(stockItemId) { current ->
                QuantityOperations.consume(current, toConsume)
            }

        override suspend fun correctExactQuantity(
            stockItemId: StockItemId,
            newQuantity: ExactQuantity,
        ): QuantityMutationResult =
            mutateQuantity(stockItemId) { current ->
                QuantityOperations.correct(
                    current,
                    newQuantity.amount,
                    newQuantity.unit,
                )
            }

        @Suppress("TooGenericExceptionCaught", "SwallowedException")
        private suspend fun mutateQuantity(
            stockItemId: StockItemId,
            operation: (ExactQuantity) -> DomainResult<ExactQuantity>,
        ): QuantityMutationResult =
            try {
                database.withTransaction {
                    val entity =
                        inventoryDao.getStockItemById(stockItemId.value)
                            ?: return@withTransaction QuantityMutationResult.Failure(DomainError.ProductNotFound)

                    if (entity.trackingMode != StockTrackingMode.EXACT.name) {
                        return@withTransaction QuantityMutationResult.Failure(DomainError.IncompatibleMode)
                    }

                    val currentQuantity =
                        ExactQuantity.of(
                            BigDecimal(requireNotNull(entity.quantityAmount)),
                            MeasurementUnit.valueOf(requireNotNull(entity.quantityUnit)),
                        )

                    when (val result = operation(currentQuantity)) {
                        is Success -> {
                            inventoryDao.updateStockItemQuantity(
                                id = stockItemId.value,
                                amount = result.value.amount.toPlainString(),
                                unit = result.value.unit.name,
                            )
                            QuantityMutationResult.Success(result.value)
                        }

                        is Failure -> QuantityMutationResult.Failure(result.error)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                QuantityMutationResult.Failure(DomainError.StorageFailure)
            }

        @Suppress("TooGenericExceptionCaught", "SwallowedException")
        override suspend fun setPresence(
            stockItemId: StockItemId,
            isPresent: Boolean,
        ): QuantityMutationResult =
            try {
                database.withTransaction {
                    val entity =
                        inventoryDao.getStockItemById(stockItemId.value)
                            ?: return@withTransaction QuantityMutationResult.Failure(DomainError.ProductNotFound)

                    if (entity.trackingMode != StockTrackingMode.PRESENCE.name) {
                        return@withTransaction QuantityMutationResult.Failure(DomainError.IncompatibleMode)
                    }

                    inventoryDao.updateStockItemPresence(
                        id = stockItemId.value,
                        isPresent = isPresent,
                    )
                    QuantityMutationResult.Success(PresenceQuantity(isPresent))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                QuantityMutationResult.Failure(DomainError.StorageFailure)
            }

        @Suppress("TooGenericExceptionCaught", "SwallowedException")
        override suspend fun findItemByBarcode(barcode: Barcode): FindItemByBarcodeResult =
            try {
                val entity = inventoryDao.getItemByBarcode(barcode.value)
                if (entity != null) {
                    FindItemByBarcodeResult.Found(entity.toDomain())
                } else {
                    FindItemByBarcodeResult.NotFound
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                FindItemByBarcodeResult.StorageFailure
            }
    }
