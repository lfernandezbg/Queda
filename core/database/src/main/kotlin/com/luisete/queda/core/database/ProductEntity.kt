package com.luisete.queda.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["householdId", "normalizedName"], unique = true),
    ],
)
data class ProductEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val displayName: String,
    val normalizedName: String,
)
