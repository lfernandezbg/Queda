package com.luisete.queda.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
@Suppress("TooManyFunctions")
interface ManagementDao {
    @Query("SELECT * FROM storage_locations WHERE householdId = :hid ORDER BY archived, normalizedName")
    fun locations(hid: String): Flow<List<LocationEntity>>

    @Query("SELECT * FROM storage_locations WHERE id = :id AND householdId = :hid")
    suspend fun location(
        hid: String,
        id: String,
    ): LocationEntity?

    @Query("SELECT * FROM storage_locations WHERE householdId = :hid AND normalizedName = :name")
    suspend fun locationNamed(
        hid: String,
        name: String,
    ): LocationEntity?

    @Query("SELECT * FROM storage_locations WHERE householdId = :hid AND syncPending = 1")
    suspend fun pendingLocations(hid: String): List<LocationEntity>

    @Upsert
    suspend fun saveLocation(location: LocationEntity)

    @Query("SELECT * FROM stock_items WHERE id = :id AND householdId = :hid")
    suspend fun stock(
        hid: String,
        id: String,
    ): StockItemEntity?

    @Update
    suspend fun updateStock(item: StockItemEntity)

    @Query("SELECT * FROM products WHERE householdId = :hid AND normalizedName = :name LIMIT 1")
    suspend fun productNamed(
        hid: String,
        name: String,
    ): ProductEntity?

    @Query("SELECT * FROM receipt_drafts WHERE householdId = :hid AND imported = 0 ORDER BY createdAt DESC LIMIT 1")
    fun draft(hid: String): Flow<ReceiptDraftEntity?>

    @Query("SELECT * FROM receipt_drafts WHERE householdId = :hid AND fingerprint = :fingerprint")
    suspend fun receipt(
        hid: String,
        fingerprint: String,
    ): ReceiptDraftEntity?

    @Upsert
    suspend fun saveDraft(draft: ReceiptDraftEntity)

    @Query("DELETE FROM receipt_drafts WHERE householdId = :hid AND id = :id AND imported = 0")
    suspend fun discardDraft(
        hid: String,
        id: String,
    )
}
