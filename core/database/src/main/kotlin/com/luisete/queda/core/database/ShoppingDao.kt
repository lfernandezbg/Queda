package com.luisete.queda.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_entries WHERE householdId = :householdId ORDER BY purchased ASC, createdAt DESC, id")
    fun observe(householdId: String): Flow<List<ShoppingEntryEntity>>

    @Query("SELECT * FROM shopping_entries WHERE householdId = :householdId AND normalizedName = :name LIMIT 1")
    suspend fun byName(
        householdId: String,
        name: String,
    ): ShoppingEntryEntity?

    @Query("SELECT * FROM shopping_entries WHERE householdId = :householdId AND id = :id LIMIT 1")
    suspend fun byId(
        householdId: String,
        id: String,
    ): ShoppingEntryEntity?

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ShoppingEntryEntity)

    @Query("UPDATE shopping_entries SET purchased = :purchased WHERE householdId = :householdId AND id = :id")
    suspend fun setPurchased(
        householdId: String,
        id: String,
        purchased: Boolean,
    )

    @Insert
    suspend fun enqueue(operation: PendingShoppingOperationEntity)

    @Query("SELECT * FROM pending_shopping_operations WHERE householdId = :householdId ORDER BY createdAt, id")
    suspend fun pending(householdId: String): List<PendingShoppingOperationEntity>

    @Query("SELECT COUNT(*) FROM pending_shopping_operations WHERE householdId = :householdId AND entryId = :id")
    suspend fun pendingForEntry(
        householdId: String,
        id: String,
    ): Int

    @Query("DELETE FROM pending_shopping_operations WHERE id = :id")
    suspend fun acknowledge(id: String)
}
