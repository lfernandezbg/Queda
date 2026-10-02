package com.luisete.queda.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pending_sync_operations",
    indices = [Index(value = ["householdId", "createdAt"])],
)
data class PendingSyncOperationEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val stockItemId: String,
    val action: String,
    val payload: String,
    val createdAt: Long,
)
