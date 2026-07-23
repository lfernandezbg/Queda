package com.luisete.queda.core.database

data class InventoryItemProjection(
    val productId: String,
    val productHouseholdId: String,
    val productDisplayName: String,
    val productNormalizedName: String,
    val productBarcode: String?,
    val stockItemId: String,
    val stockHouseholdId: String,
    val stockProductId: String,
    val trackingMode: String,
    val quantityAmount: String?,
    val quantityUnit: String?,
    val isPresent: Boolean?,
)
