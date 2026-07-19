package com.luisete.queda.core.database

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Transaction
    @Query(
        """
        SELECT
            p.id AS productId,
            p.householdId AS productHouseholdId,
            p.displayName AS productDisplayName,
            p.normalizedName AS productNormalizedName,
            s.id AS stockItemId,
            s.householdId AS stockHouseholdId,
            s.productId AS stockProductId,
            s.quantityAmount AS quantityAmount,
            s.quantityUnit AS quantityUnit
        FROM products AS p
        INNER JOIN stock_items AS s
            ON s.productId = p.id
            AND s.householdId = p.householdId
        WHERE p.householdId = :householdId
            AND s.householdId = :householdId
        ORDER BY
            p.normalizedName ASC,
            p.id ASC,
            s.id ASC
        """,
    )
    fun observeExactInventoryItems(householdId: String): Flow<List<InventoryItemProjection>>

    @Query("SELECT COUNT(*) FROM products WHERE householdId = :householdId AND normalizedName = :normalizedName")
    suspend fun countProductsWithName(
        householdId: String,
        normalizedName: String,
    ): Int

    @Insert
    suspend fun insertProduct(product: ProductEntity)

    @Insert
    suspend fun insertStockItem(stockItem: StockItemEntity)

    @Suppress("ReturnCount")
    @Transaction
    suspend fun addExactInventoryItem(
        product: ProductEntity,
        stockItem: StockItemEntity,
    ): AddExactInventoryItemDbResult {
        require(product.id == stockItem.productId)
        require(product.householdId == stockItem.householdId)

        if (countProductsWithName(product.householdId, product.normalizedName) > 0) {
            return AddExactInventoryItemDbResult.DuplicateProductName
        }

        try {
            insertProduct(product)
        } catch (e: SQLiteConstraintException) {
            if (countProductsWithName(product.householdId, product.normalizedName) > 0) {
                return AddExactInventoryItemDbResult.DuplicateProductName
            }
            throw e
        }

        insertStockItem(stockItem)
        return AddExactInventoryItemDbResult.Added
    }
}
