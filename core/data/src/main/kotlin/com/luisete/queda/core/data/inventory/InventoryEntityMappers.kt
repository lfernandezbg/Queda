package com.luisete.queda.core.data.inventory

import com.luisete.queda.core.database.InventoryItemProjection
import com.luisete.queda.core.database.ProductEntity
import com.luisete.queda.core.database.StockItemEntity
import com.luisete.queda.core.model.barcode.Barcode
import com.luisete.queda.core.model.barcode.BarcodeCreationResult
import com.luisete.queda.core.model.id.HouseholdId
import com.luisete.queda.core.model.id.ProductId
import com.luisete.queda.core.model.id.StockItemId
import com.luisete.queda.core.model.inventory.InventoryItem
import com.luisete.queda.core.model.inventory.StockItem
import com.luisete.queda.core.model.inventory.StockTrackingMode
import com.luisete.queda.core.model.product.Product
import com.luisete.queda.core.model.product.ProductName
import com.luisete.queda.core.model.product.ProductNameCreationResult
import com.luisete.queda.core.model.quantity.ExactQuantity
import com.luisete.queda.core.model.quantity.MeasurementUnit
import com.luisete.queda.core.model.quantity.PresenceQuantity
import java.math.BigDecimal

fun Product.toEntity(): ProductEntity =
    ProductEntity(
        id = id.value,
        householdId = householdId.value,
        displayName = name.displayValue,
        normalizedName = name.normalizedKey,
        barcode = barcode?.value,
    )

fun StockItem.toEntity(): StockItemEntity =
    when (val q = quantity) {
        is ExactQuantity ->
            StockItemEntity(
                id = id.value,
                householdId = householdId.value,
                productId = productId.value,
                trackingMode = StockTrackingMode.EXACT.name,
                quantityAmount = q.amount.toPlainString(),
                quantityUnit = q.unit.name,
                isPresent = null,
            )

        is PresenceQuantity ->
            StockItemEntity(
                id = id.value,
                householdId = householdId.value,
                productId = productId.value,
                trackingMode = StockTrackingMode.PRESENCE.name,
                quantityAmount = null,
                quantityUnit = null,
                isPresent = q.isPresent,
            )

        else -> error("Quantity type ${q::class.simpleName} is not supported in persistence")
    }

fun InventoryItemProjection.toDomain(): InventoryItem {
    val productDomain = toProductDomain()
    val quantity = toQuantityDomain()

    validateProjection()

    val stockItemDomain =
        StockItem(
            id = StockItemId.from(stockItemId),
            householdId = HouseholdId.from(stockHouseholdId),
            productId = ProductId.from(stockProductId),
            quantity = quantity,
        )

    return InventoryItem(productDomain, stockItemDomain)
}

private fun InventoryItemProjection.toProductDomain(): Product {
    val productName =
        when (val res = ProductName.create(productDisplayName)) {
            is ProductNameCreationResult.Success -> res.productName
            else -> error("Persisted product name is invalid: $productDisplayName")
        }

    if (productName.displayValue != productDisplayName) {
        error("Persisted display name is not canonical: $productDisplayName")
    }

    if (productName.normalizedKey != productNormalizedName) {
        error("Persisted normalized name mismatch: expected $productNormalizedName, got ${productName.normalizedKey}")
    }

    val barcode =
        productBarcode?.let {
            when (val res = Barcode.create(it)) {
                is BarcodeCreationResult.Success -> res.barcode
                else -> error("Persisted barcode is invalid: $it")
            }
        }

    return Product(
        id = ProductId.from(productId),
        householdId = HouseholdId.from(productHouseholdId),
        name = productName,
        barcode = barcode,
    )
}

private fun InventoryItemProjection.toQuantityDomain(): com.luisete.queda.core.model.quantity.StockQuantity =
    when (trackingMode) {
        StockTrackingMode.EXACT.name -> {
            val amount =
                requireNotNull(quantityAmount) {
                    "Exact quantity must have an amount"
                }
            val unit =
                requireNotNull(quantityUnit) {
                    "Exact quantity must have a unit"
                }
            val q =
                ExactQuantity.of(
                    BigDecimal(amount),
                    MeasurementUnit.valueOf(unit),
                )

            if (q.amount.toPlainString() != amount) {
                error("Persisted quantity amount is not canonical: $amount")
            }
            q
        }

        StockTrackingMode.PRESENCE.name -> {
            PresenceQuantity(
                isPresent =
                    requireNotNull(isPresent) {
                        "Presence quantity must have isPresent value"
                    },
            )
        }

        else -> error("Unknown tracking mode: $trackingMode")
    }

private fun InventoryItemProjection.validateProjection() {
    if (stockProductId != productId) {
        error("Persisted stock product ID mismatch: expected $productId, got $stockProductId")
    }

    if (stockHouseholdId != productHouseholdId) {
        error("Persisted stock household ID mismatch: expected $productHouseholdId, got $stockHouseholdId")
    }
}
