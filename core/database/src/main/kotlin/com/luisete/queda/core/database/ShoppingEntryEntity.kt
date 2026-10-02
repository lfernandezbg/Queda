package com.luisete.queda.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shopping_entries",
    indices = [Index(value = ["householdId", "normalizedName"], unique = true)],
)
data class ShoppingEntryEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val displayName: String,
    val normalizedName: String,
    val purchased: Boolean,
    val createdAt: Long,
)

@Entity(
    tableName = "pending_shopping_operations",
    indices = [Index(value = ["householdId", "createdAt"])],
)
data class PendingShoppingOperationEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val entryId: String,
    val action: String,
    val desiredPurchased: Boolean,
    val createdAt: Long,
)
