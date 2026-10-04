package com.luisete.queda.core.database

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
@Suppress("TooManyFunctions")
interface InventoryDao {
    @Transaction
    @Query(
        """
        SELECT
            p.id AS productId,
            p.householdId AS productHouseholdId,
            p.displayName AS productDisplayName,
            p.normalizedName AS productNormalizedName,
            p.barcode AS productBarcode,
            s.id AS stockItemId,
            s.householdId AS stockHouseholdId,
            s.productId AS stockProductId,
            s.trackingMode AS trackingMode,
            s.quantityAmount AS quantityAmount,
            s.quantityUnit AS quantityUnit,
            s.isPresent AS isPresent,
            s.locationId AS locationId, s.foodType AS foodType, s.label AS label,
            s.preparedOn AS preparedOn, s.bestBefore AS bestBefore
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

    @Query("SELECT * FROM products WHERE householdId = :householdId AND barcode = :barcode")
    suspend fun getProductByBarcode(
        householdId: String,
        barcode: String,
    ): ProductEntity?

    @Transaction
    @Query(
        """
        SELECT
            p.id AS productId,
            p.householdId AS productHouseholdId,
            p.displayName AS productDisplayName,
            p.normalizedName AS productNormalizedName,
            p.barcode AS productBarcode,
            s.id AS stockItemId,
            s.householdId AS stockHouseholdId,
            s.productId AS stockProductId,
            s.trackingMode AS trackingMode,
            s.quantityAmount AS quantityAmount,
            s.quantityUnit AS quantityUnit,
            s.isPresent AS isPresent,
            s.locationId AS locationId, s.foodType AS foodType, s.label AS label,
            s.preparedOn AS preparedOn, s.bestBefore AS bestBefore
        FROM products AS p
        INNER JOIN stock_items AS s
            ON s.productId = p.id
            AND s.householdId = p.householdId
        WHERE p.householdId = :householdId AND s.householdId = :householdId AND p.barcode = :barcode
        LIMIT 1
        """,
    )
    suspend fun getItemByBarcode(
        householdId: String,
        barcode: String,
    ): InventoryItemProjection?

    @Query(
        """
        SELECT p.id AS productId, p.householdId AS productHouseholdId,
            p.displayName AS productDisplayName, p.normalizedName AS productNormalizedName,
            p.barcode AS productBarcode,
            s.id AS stockItemId, s.householdId AS stockHouseholdId,
            s.productId AS stockProductId, s.trackingMode AS trackingMode,
            s.quantityAmount AS quantityAmount, s.quantityUnit AS quantityUnit,
            s.isPresent AS isPresent, s.locationId AS locationId,
            s.foodType AS foodType, s.label AS label,
            s.preparedOn AS preparedOn, s.bestBefore AS bestBefore
        FROM products AS p INNER JOIN stock_items AS s
          ON s.productId = p.id AND s.householdId = p.householdId
        WHERE p.householdId = :householdId AND p.barcode = :barcode
        ORDER BY s.preparedOn, s.id
        """,
    )
    suspend fun getItemsByBarcode(
        householdId: String,
        barcode: String,
    ): List<InventoryItemProjection>

    @Insert
    suspend fun insertProduct(product: ProductEntity)

    @Insert
    suspend fun insertStockItem(stockItem: StockItemEntity)

    @Query("SELECT * FROM stock_items WHERE id = :id")
    suspend fun getStockItemById(id: String): StockItemEntity?

    @Query("UPDATE stock_items SET quantityAmount = :amount, quantityUnit = :unit WHERE id = :id")
    suspend fun updateStockItemQuantity(
        id: String,
        amount: String,
        unit: String,
    )

    @Query("UPDATE stock_items SET isPresent = :isPresent WHERE id = :id")
    suspend fun updateStockItemPresence(
        id: String,
        isPresent: Boolean,
    )

    @Suppress("ReturnCount")
    @Transaction
    suspend fun addInventoryItem(
        product: ProductEntity,
        stockItem: StockItemEntity,
    ): AddExactInventoryItemDbResult {
        require(product.id == stockItem.productId)
        require(product.householdId == stockItem.householdId)

        if (countProductsWithName(product.householdId, product.normalizedName) > 0) {
            return AddExactInventoryItemDbResult.DuplicateProductName
        }

        if (product.barcode != null && getProductByBarcode(product.householdId, product.barcode) != null) {
            return AddExactInventoryItemDbResult.DuplicateBarcode
        }

        try {
            insertProduct(product)
        } catch (e: SQLiteConstraintException) {
            if (countProductsWithName(product.householdId, product.normalizedName) > 0) {
                return AddExactInventoryItemDbResult.DuplicateProductName
            }
            if (product.barcode != null && getProductByBarcode(product.householdId, product.barcode) != null) {
                return AddExactInventoryItemDbResult.DuplicateBarcode
            }
            throw e
        }

        insertStockItem(stockItem)
        return AddExactInventoryItemDbResult.Added
    }

    @Suppress("ReturnCount")
    @Transaction
    suspend fun addExactInventoryItem(
        product: ProductEntity,
        stockItem: StockItemEntity,
    ): AddExactInventoryItemDbResult = addInventoryItem(product, stockItem)
}
