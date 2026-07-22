package com.luisete.queda.core.testing

import com.luisete.queda.core.model.barcode.Barcode
import com.luisete.queda.core.model.barcode.BarcodeCreationResult
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
import com.luisete.queda.core.model.quantity.StockQuantity
import java.math.BigDecimal

object InventoryTestData {
    val householdId = HouseholdId.from("test-household")

    fun createProduct(
        id: String = "p1",
        name: String = "Test Product",
        barcode: String? = null,
    ): Product {
        val productName =
            when (val res = ProductName.create(name)) {
                is ProductNameCreationResult.Success -> res.productName
                else -> throw IllegalArgumentException("Invalid name for test data: $name")
            }
        val barcodeDomain =
            barcode?.let {
                when (val res = Barcode.create(it)) {
                    is BarcodeCreationResult.Success -> res.barcode
                    else -> throw IllegalArgumentException("Invalid barcode for test data: $it")
                }
            }
        return Product(
            id = ProductId.from(id),
            householdId = householdId,
            name = productName,
            barcode = barcodeDomain,
        )
    }

    fun createStockItem(
        id: String = "s1",
        productId: String = "p1",
        quantity: StockQuantity = ExactQuantity.of("1", MeasurementUnit.UNIT),
    ): StockItem =
        StockItem(
            id = StockItemId.from(id),
            householdId = householdId,
            productId = ProductId.from(productId),
            quantity = quantity,
        )

    @Suppress("LongParameterList")
    fun createInventoryItem(
        name: String = "Test Product",
        amount: String = "1",
        unit: MeasurementUnit = MeasurementUnit.UNIT,
        productId: String = "p1",
        stockItemId: String = "s1",
        barcode: String? = null,
        quantity: StockQuantity? = null,
    ): InventoryItem {
        val productName =
            when (val res = ProductName.create(name)) {
                is ProductNameCreationResult.Success -> res.productName
                else -> throw IllegalArgumentException("Invalid name for test data: $name")
            }
        val barcodeDomain =
            barcode?.let {
                when (val res = Barcode.create(it)) {
                    is BarcodeCreationResult.Success -> res.barcode
                    else -> throw IllegalArgumentException("Invalid barcode for test data: $it")
                }
            }
        val product =
            Product(
                id = ProductId.from(productId),
                householdId = householdId,
                name = productName,
                barcode = barcodeDomain,
            )
        val finalQuantity = quantity ?: ExactQuantity.of(BigDecimal(amount), unit)
        val stockItem =
            StockItem(
                id = StockItemId.from(stockItemId),
                householdId = householdId,
                productId = product.id,
                quantity = finalQuantity,
            )
        return InventoryItem(product, stockItem)
    }
}
