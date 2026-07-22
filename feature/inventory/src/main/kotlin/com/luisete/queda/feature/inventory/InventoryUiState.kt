package com.luisete.queda.feature.inventory

import com.luisete.queda.core.model.inventory.InventoryItem
import com.luisete.queda.core.model.quantity.ExactQuantity
import com.luisete.queda.core.model.quantity.MeasurementUnit

sealed interface InventoryUiState {
    data object Loading : InventoryUiState

    data object Empty : InventoryUiState

    data class Content(
        val items: List<InventoryItemUiModel>,
        val quantityAction: QuantityActionUiState = QuantityActionUiState.Closed,
    ) : InventoryUiState

    data object Error : InventoryUiState
}

sealed interface QuantityActionUiState {
    data object Closed : QuantityActionUiState

    data class ActionSelection(
        val item: InventoryItemUiModel,
    ) : QuantityActionUiState

    data class ConsumeEditing(
        val item: InventoryItemUiModel,
        val amountInput: String = "",
        val selectedUnit: MeasurementUnit,
        val preview: QuantityPreviewUiModel? = null,
        val error: QuantityActionError? = null,
        val isSubmitting: Boolean = false,
    ) : QuantityActionUiState

    data class CorrectEditing(
        val item: InventoryItemUiModel,
        val amountInput: String = "",
        val selectedUnit: MeasurementUnit,
        val preview: QuantityPreviewUiModel? = null,
        val error: QuantityActionError? = null,
        val isSubmitting: Boolean = false,
    ) : QuantityActionUiState
}

data class QuantityPreviewUiModel(
    val amountFormatted: String,
    val unit: MeasurementUnit,
)

enum class QuantityActionError {
    INVALID_AMOUNT,
    INCOMPATIBLE_UNIT,
    MUST_BE_POSITIVE,
    MUST_BE_LOWER_THAN_CURRENT,
    STORAGE_FAILURE,
    UNCHANGED,
    PRODUCT_NOT_FOUND,
}

data class InventoryItemUiModel(
    val id: String,
    val name: String,
    val amountFormatted: String,
    val quantity: ExactQuantity,
    val barcode: String? = null,
)

fun InventoryItem.toUiModel(): InventoryItemUiModel =
    InventoryItemUiModel(
        id = stockItem.id.value,
        name = product.name.displayValue,
        amountFormatted = ExactQuantityUiFormatter.format(stockItem.quantity),
        quantity = stockItem.quantity,
        barcode = product.barcode?.value,
    )
