package com.luisete.queda.core.model.inventory

import com.luisete.queda.core.model.id.HouseholdId
import com.luisete.queda.core.model.id.ProductId
import com.luisete.queda.core.model.id.StockItemId
import com.luisete.queda.core.model.product.Product
import com.luisete.queda.core.model.product.ProductName
import com.luisete.queda.core.model.product.ProductNameCreationResult
import com.luisete.queda.core.model.quantity.ExactQuantity
import com.luisete.queda.core.model.quantity.MeasurementUnit
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class InventoryModelTests {
    private val householdId = HouseholdId.from("h1")
    private val productId = ProductId.from("p1")
    private val productName = (ProductName.create("Milk") as ProductNameCreationResult.Success).productName
    private val product = Product(productId, householdId, productName)
    private val quantity = ExactQuantity.of(BigDecimal.TEN, MeasurementUnit.UNIT)
    private val stockItemId = StockItemId.from("s1")
    private val stockItem = StockItem(stockItemId, householdId, productId, quantity)

    @Test
    fun productPreservesIdentifiersAndName() {
        assertEquals(productId, product.id)
        assertEquals(householdId, product.householdId)
        assertEquals(productName, product.name)
    }

    @Test
    fun stockItemPreservesIdentifiersAndQuantity() {
        assertEquals(stockItemId, stockItem.id)
        assertEquals(householdId, stockItem.householdId)
        assertEquals(productId, stockItem.productId)
        assertEquals(quantity, stockItem.quantity)
    }

    @Test
    fun inventoryItemAcceptsMatchingProductAndStock() {
        val inventoryItem = InventoryItem(product, stockItem)
        assertEquals(product, inventoryItem.product)
        assertEquals(stockItem, inventoryItem.stockItem)
    }

    @Test(expected = IllegalArgumentException::class)
    fun inventoryItemRejectsDifferentHousehold() {
        val otherHousehold = HouseholdId.from("h2")
        val invalidStockItem = stockItem.copy(householdId = otherHousehold)
        InventoryItem(product, invalidStockItem)
    }

    @Test(expected = IllegalArgumentException::class)
    fun inventoryItemRejectsDifferentProductId() {
        val otherProduct = product.copy(id = ProductId.from("p2"))
        InventoryItem(otherProduct, stockItem)
    }

    @Test
    fun inventoryItemPreservesExactQuantityWithoutConversion() {
        val inventoryItem = InventoryItem(product, stockItem)
        assertEquals(quantity, inventoryItem.stockItem.quantity)
    }
}
