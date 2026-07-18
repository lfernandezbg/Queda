package com.luisete.queda.core.domain.inventory

import com.luisete.queda.core.model.inventory.InventoryItem

sealed interface AddExactInventoryItemResult {
    data class Added(val inventoryItem: InventoryItem) : AddExactInventoryItemResult

    data class InvalidInput(
        val nameReason: ProductNameCreationError?,
        val quantityReason: ExactQuantityInputError?,
    ) : AddExactInventoryItemResult {
        init {
            require(nameReason != null || quantityReason != null) {
                "At least one reason must be provided for InvalidInput"
            }
        }
    }

    data object DuplicateProductName : AddExactInventoryItemResult

    data object StorageFailure : AddExactInventoryItemResult
}

sealed interface ProductNameCreationError {
    data object Blank : ProductNameCreationError

    data object TooLong : ProductNameCreationError

    data object ContainsForbiddenCharacter : ProductNameCreationError
}

sealed interface ExactQuantityInputError {
    data object Blank : ExactQuantityInputError

    data object InvalidFormat : ExactQuantityInputError

    data object NotPositive : ExactQuantityInputError

    data object TooManyDecimalPlaces : ExactQuantityInputError
}
