package com.luisete.queda.feature.inventory

import com.luisete.queda.core.testing.InventoryTestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class InventoryUiStateTest {
    @Test
    fun uiModelIdUsesStockItemIdNotProductId() {
        val productId = "p-123"
        val stockItemId = "s-456"
        val inventoryItem =
            InventoryTestData.createInventoryItem(
                productId = productId,
                stockItemId = stockItemId,
            )

        val uiModel = inventoryItem.toUiModel()

        assertEquals(stockItemId, uiModel.id)
        assertNotEquals(productId, uiModel.id)
    }
}
