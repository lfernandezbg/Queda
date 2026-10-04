package com.luisete.queda.core.database

import androidx.room.ColumnInfo
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
    @ColumnInfo(defaultValue = "NULL") val locationId: String? = null,
    @ColumnInfo(defaultValue = "'FOOD'") val foodType: String = "FOOD",
    @ColumnInfo(defaultValue = "NULL") val label: String? = null,
    @ColumnInfo(defaultValue = "NULL") val preparedOn: String? = null,
    @ColumnInfo(defaultValue = "NULL") val bestBefore: String? = null,
)
