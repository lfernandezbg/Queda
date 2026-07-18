package com.luisete.queda.feature.inventory

import com.luisete.queda.core.model.inventory.InventoryItem
import com.luisete.queda.core.model.quantity.MeasurementUnit

sealed interface InventoryUiState {
    data object Loading : InventoryUiState

    data object Empty : InventoryUiState

    data class Content(
        val items: List<InventoryItemUiModel>,
    ) : InventoryUiState

    data object Error : InventoryUiState
}

data class InventoryItemUiModel(
    val id: String,
    val name: String,
    val amountFormatted: String,
    val unit: MeasurementUnit,
)

fun InventoryItem.toUiModel(): InventoryItemUiModel =
    InventoryItemUiModel(
        id = stockItem.id.value,
        name = product.name.displayValue,
        amountFormatted = ExactQuantityUiFormatter.format(stockItem.quantity),
        unit = stockItem.quantity.unit,
    )
