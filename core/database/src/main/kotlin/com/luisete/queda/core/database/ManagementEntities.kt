package com.luisete.queda.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "storage_locations", indices = [Index(value = ["householdId", "normalizedName"], unique = true)])
data class LocationEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val name: String,
    val normalizedName: String,
    val archived: Boolean,
    @ColumnInfo(defaultValue = "0") val revision: Long = 0,
    @ColumnInfo(defaultValue = "0") val syncPending: Boolean = false,
    @ColumnInfo(defaultValue = "NULL") val conflictPayload: String? = null,
)

@Entity(tableName = "receipt_drafts", indices = [Index(value = ["householdId", "fingerprint"], unique = true)])
data class ReceiptDraftEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val fingerprint: String,
    val payload: String,
    val imported: Boolean,
    val createdAt: Long,
)
