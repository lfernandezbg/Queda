package com.luisete.queda.core.domain.inventory

import com.luisete.queda.core.model.barcode.Barcode
import com.luisete.queda.core.model.id.StockItemId

sealed interface ResolveScannedBarcodeResult {
    data class NewBarcode(val barcode: Barcode) : ResolveScannedBarcodeResult

    data class ExistingItem(val stockItemId: StockItemId) : ResolveScannedBarcodeResult

    data class InvalidBarcode(val reason: BarcodeValidationError) : ResolveScannedBarcodeResult

    data object StorageFailure : ResolveScannedBarcodeResult
}

enum class BarcodeValidationError {
    BLANK,
    NON_DIGIT,
    UNSUPPORTED_FORMAT,
    INVALID_CHECK_DIGIT,
}
