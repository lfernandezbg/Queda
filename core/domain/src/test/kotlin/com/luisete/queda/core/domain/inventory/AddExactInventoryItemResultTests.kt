package com.luisete.queda.core.domain.inventory

import org.junit.Test

class AddExactInventoryItemResultTests {
    @Test(expected = IllegalArgumentException::class)
    fun invalidInputRejectsBothReasonsNull() {
        AddExactInventoryItemResult.InvalidInput(
            nameReason = null,
            quantityReason = null,
        )
    }

    @Test
    fun invalidInputAllowsOnlyNameReason() {
        AddExactInventoryItemResult.InvalidInput(
            nameReason = ProductNameCreationError.Blank,
            quantityReason = null,
        )
    }

    @Test
    fun invalidInputAllowsOnlyQuantityReason() {
        AddExactInventoryItemResult.InvalidInput(
            nameReason = null,
            quantityReason = ExactQuantityInputError.Blank,
        )
    }

    @Test
    fun invalidInputAllowsBothReasons() {
        AddExactInventoryItemResult.InvalidInput(
            nameReason = ProductNameCreationError.Blank,
            quantityReason = ExactQuantityInputError.Blank,
        )
    }
}
