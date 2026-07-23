package com.luisete.queda.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_items",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["householdId"]),
        Index(value = ["productId"]),
    ],
)
data class StockItemEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val productId: String,
    val trackingMode: String,
    val quantityAmount: String?,
    val quantityUnit: String?,
    val isPresent: Boolean?,
)
