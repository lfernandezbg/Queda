package com.luisete.queda.core.domain.inventory

import com.luisete.queda.core.model.barcode.Barcode
import com.luisete.queda.core.model.barcode.BarcodeCreationResult
import javax.inject.Inject

class ResolveScannedBarcodeUseCase
    @Inject
    constructor(
        private val repository: InventoryRepository,
    ) {
        suspend operator fun invoke(rawBarcode: String): ResolveScannedBarcodeResult {
            return when (val barcodeResult = Barcode.create(rawBarcode)) {
                is BarcodeCreationResult.Success -> {
                    when (val findResult = repository.findItemByBarcode(barcodeResult.barcode)) {
                        is FindItemByBarcodeResult.Found ->
                            ResolveScannedBarcodeResult.ExistingItem(findResult.item.stockItem.id)

                        FindItemByBarcodeResult.NotFound ->
                            ResolveScannedBarcodeResult.NewBarcode(barcodeResult.barcode)

                        FindItemByBarcodeResult.StorageFailure ->
                            ResolveScannedBarcodeResult.StorageFailure
                    }
                }

                BarcodeCreationResult.Blank ->
                    ResolveScannedBarcodeResult.InvalidBarcode(BarcodeValidationError.BLANK)

                BarcodeCreationResult.NonDigit ->
                    ResolveScannedBarcodeResult.InvalidBarcode(BarcodeValidationError.NON_DIGIT)

                BarcodeCreationResult.UnsupportedFormat ->
                    ResolveScannedBarcodeResult.InvalidBarcode(BarcodeValidationError.UNSUPPORTED_FORMAT)

                BarcodeCreationResult.InvalidCheckDigit ->
                    ResolveScannedBarcodeResult.InvalidBarcode(BarcodeValidationError.INVALID_CHECK_DIGIT)
            }
        }
    }
