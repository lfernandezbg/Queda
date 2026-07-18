package com.luisete.queda.core.testing

import com.luisete.queda.core.model.id.HouseholdId
import com.luisete.queda.core.model.id.ProductId
import com.luisete.queda.core.model.id.StockItemId
import com.luisete.queda.core.model.inventory.InventoryItem
import com.luisete.queda.core.model.inventory.StockItem
import com.luisete.queda.core.model.product.Product
import com.luisete.queda.core.model.product.ProductName
import com.luisete.queda.core.model.product.ProductNameCreationResult
import com.luisete.queda.core.model.quantity.ExactQuantity
import com.luisete.queda.core.model.quantity.MeasurementUnit
import java.math.BigDecimal

object InventoryTestData {
    val householdId = HouseholdId.from("test-household")

    fun createInventoryItem(
        name: String = "Test Product",
        amount: String = "1",
        unit: MeasurementUnit = MeasurementUnit.UNIT,
        productId: String = "p1",
        stockItemId: String = "s1",
    ): InventoryItem {
        val productName =
            when (val res = ProductName.create(name)) {
                is ProductNameCreationResult.Success -> res.productName
                else -> throw IllegalArgumentException("Invalid name for test data: $name")
            }
        val product =
            Product(
                id = ProductId.from(productId),
                householdId = householdId,
                name = productName,
            )
        val stockItem =
            StockItem(
                id = StockItemId.from(stockItemId),
                householdId = householdId,
                productId = product.id,
                quantity = ExactQuantity.of(BigDecimal(amount), unit),
            )
        return InventoryItem(product, stockItem)
    }
}
