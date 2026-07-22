package com.luisete.queda.core.domain.inventory

import com.luisete.queda.core.model.barcode.Barcode
import com.luisete.queda.core.model.barcode.BarcodeCreationResult
import com.luisete.queda.core.model.id.ProductId
import com.luisete.queda.core.model.id.StockItemId
import com.luisete.queda.core.model.inventory.InventoryItem
import com.luisete.queda.core.model.inventory.StockItem
import com.luisete.queda.core.model.inventory.StockTrackingMode
import com.luisete.queda.core.model.product.Product
import com.luisete.queda.core.model.product.ProductName
import com.luisete.queda.core.model.product.ProductNameCreationResult
import com.luisete.queda.core.model.quantity.MeasurementUnit
import com.luisete.queda.core.model.quantity.PresenceQuantity
import javax.inject.Inject

class AddExactInventoryItemUseCase
    @Inject
    constructor(
        private val repository: InventoryRepository,
        private val householdProvider: CurrentHouseholdIdProvider,
    ) {
        @Suppress("ReturnCount", "LongMethod", "CyclomaticComplexMethod")
        suspend operator fun invoke(
            rawName: String,
            rawQuantity: String,
            unit: MeasurementUnit,
            rawBarcode: String? = null,
            trackingMode: StockTrackingMode = StockTrackingMode.EXACT,
        ): AddExactInventoryItemResult {
            val nameResult = ProductName.create(rawName)
            val quantityResult =
                if (trackingMode == StockTrackingMode.EXACT) {
                    ExactQuantityInputParser.parse(rawQuantity, unit)
                } else {
                    null
                }

            val barcodeResult =
                rawBarcode?.let {
                    Barcode.create(it)
                }

            val isNameInvalid = nameResult !is ProductNameCreationResult.Success
            val isQuantityInvalid =
                trackingMode == StockTrackingMode.EXACT &&
                    quantityResult !is ExactQuantityInputResult.Success
            val isBarcodeInvalid =
                barcodeResult is BarcodeCreationResult.InvalidCheckDigit ||
                    barcodeResult is BarcodeCreationResult.UnsupportedFormat ||
                    barcodeResult is BarcodeCreationResult.NonDigit

            if (isNameInvalid || isQuantityInvalid || isBarcodeInvalid) {
                return AddExactInventoryItemResult.InvalidInput(
                    nameReason =
                        if (nameResult is ProductNameCreationResult.Success) {
                            null
                        } else {
                            nameResult.toError()
                        },
                    quantityReason =
                        if (quantityResult == null || quantityResult is ExactQuantityInputResult.Success) {
                            null
                        } else {
                            quantityResult.toError()
                        },
                )
            }

            val householdId = householdProvider.currentHouseholdId()
            val productId = ProductId.newId()
            val stockItemId = StockItemId.newId()

            val barcode = (barcodeResult as? BarcodeCreationResult.Success)?.barcode

            val product =
                Product(
                    id = productId,
                    householdId = householdId,
                    name = nameResult.productName,
                    barcode = barcode,
                )

            val quantity =
                if (trackingMode == StockTrackingMode.EXACT) {
                    (quantityResult as ExactQuantityInputResult.Success).quantity
                } else {
                    PresenceQuantity(isPresent = true)
                }

            val stockItem =
                StockItem(
                    id = stockItemId,
                    householdId = householdId,
                    productId = productId,
                    quantity = quantity,
                )
            val inventoryItem = InventoryItem(product, stockItem)

            return when (repository.addExactInventoryItem(product, stockItem)) {
                AddExactItemRepositoryResult.Added -> AddExactInventoryItemResult.Added(inventoryItem)
                AddExactItemRepositoryResult.DuplicateProductName -> AddExactInventoryItemResult.DuplicateProductName
                AddExactItemRepositoryResult.DuplicateBarcode -> AddExactInventoryItemResult.DuplicateBarcode
                AddExactItemRepositoryResult.StorageFailure -> AddExactInventoryItemResult.StorageFailure
            }
        }

        private fun ProductNameCreationResult.toError(): ProductNameCreationError =
            when (this) {
                ProductNameCreationResult.Blank -> ProductNameCreationError.Blank
                ProductNameCreationResult.TooLong -> ProductNameCreationError.TooLong
                ProductNameCreationResult.ContainsForbiddenCharacter ->
                    ProductNameCreationError.ContainsForbiddenCharacter
                is ProductNameCreationResult.Success ->
                    throw IllegalArgumentException("Success cannot be mapped to error")
            }

        private fun ExactQuantityInputResult.toError(): ExactQuantityInputError =
            when (this) {
                ExactQuantityInputResult.Blank -> ExactQuantityInputError.Blank
                ExactQuantityInputResult.InvalidFormat -> ExactQuantityInputError.InvalidFormat
                ExactQuantityInputResult.NotPositive -> ExactQuantityInputError.NotPositive
                ExactQuantityInputResult.TooManyDecimalPlaces -> ExactQuantityInputError.TooManyDecimalPlaces
                is ExactQuantityInputResult.Success ->
                    throw IllegalArgumentException("Success cannot be mapped to error")
            }
    }
