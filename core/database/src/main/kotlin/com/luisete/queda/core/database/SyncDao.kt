@file:Suppress("detekt:TooManyFunctions", "detekt:MaxLineLength")

package com.luisete.queda.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncDao {
    @Insert
    suspend fun enqueue(operation: PendingSyncOperationEntity)

    @Query("SELECT * FROM pending_sync_operations WHERE householdId = :householdId ORDER BY createdAt, id")
    suspend fun pending(householdId: String): List<PendingSyncOperationEntity>

    @Query("SELECT COUNT(*) FROM pending_sync_operations WHERE householdId = :householdId")
    fun pendingCount(householdId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_sync_operations WHERE householdId = :householdId AND stockItemId = :stockItemId")
    suspend fun pendingForItem(
        householdId: String,
        stockItemId: String,
    ): Int

    @Query("DELETE FROM pending_sync_operations WHERE id = :id")
    suspend fun acknowledge(id: String)

    @Query("DELETE FROM pending_sync_operations WHERE householdId = :householdId AND stockItemId = :stockItemId")
    suspend fun discardPending(
        householdId: String,
        stockItemId: String,
    )

    @Query("DELETE FROM stock_items WHERE id = :id")
    suspend fun deleteStockItem(id: String)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProduct(id: String)

    @Query("SELECT * FROM products WHERE householdId = :householdId")
    suspend fun products(householdId: String): List<ProductEntity>

    @Query("SELECT * FROM stock_items WHERE householdId = :householdId")
    suspend fun stockItems(householdId: String): List<StockItemEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun product(id: String): ProductEntity?

    @Query("SELECT * FROM stock_items WHERE id = :id")
    suspend fun stockItem(id: String): StockItemEntity?

    @Insert
    suspend fun insertProduct(product: ProductEntity)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Insert
    suspend fun insertStockItem(item: StockItemEntity)

    @Query(
        "UPDATE stock_items SET quantityAmount = :amount, quantityUnit = :unit, " +
            "isPresent = :isPresent WHERE id = :id AND householdId = :householdId",
    )
    suspend fun setRemoteStock(
        id: String,
        householdId: String,
        amount: String?,
        unit: String?,
        isPresent: Boolean?,
    )

    @Query("UPDATE products SET householdId = :newId WHERE householdId = :oldId")
    suspend fun moveProducts(
        oldId: String,
        newId: String,
    )

    @Query("UPDATE stock_items SET householdId = :newId WHERE householdId = :oldId")
    suspend fun moveStock(
        oldId: String,
        newId: String,
    )

    @Transaction
    suspend fun importLegacy(householdId: String) {
        val products = products("local-household-v1")
        val stocks = stockItems("local-household-v1")
        moveProducts("local-household-v1", householdId)
        moveStock("local-household-v1", householdId)
        val byId = products.associateBy { it.id }
        stocks.forEach { item ->
            val product = checkNotNull(byId[item.productId])
            enqueue(
                PendingSyncOperationEntity(
                    id = "legacy-${item.id}",
                    householdId = householdId,
                    stockItemId = item.id,
                    action = "ADD",
                    payload =
                        SyncPayloadCodec.encode(
                            product.copy(householdId = householdId),
                            item.copy(householdId = householdId),
                        ),
                    createdAt = 0L,
                ),
            )
        }
    }
}
