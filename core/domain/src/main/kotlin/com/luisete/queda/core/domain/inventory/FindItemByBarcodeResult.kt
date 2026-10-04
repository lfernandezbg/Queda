package com.luisete.queda.core.domain.inventory

import com.luisete.queda.core.model.inventory.InventoryItem

sealed interface FindItemByBarcodeResult {
    data class Found(
        val item: InventoryItem,
        val candidates: List<InventoryItem> = listOf(item),
    ) : FindItemByBarcodeResult

    data object NotFound : FindItemByBarcodeResult

    data object StorageFailure : FindItemByBarcodeResult
}
