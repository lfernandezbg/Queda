package com.luisete.queda.core.data.inventory

import com.luisete.queda.core.database.PendingSyncOperationEntity
import com.luisete.queda.core.database.ProductEntity
import com.luisete.queda.core.database.QuedaDatabase
import com.luisete.queda.core.database.StockItemEntity
import com.luisete.queda.core.database.SyncPayloadCodec
import com.luisete.queda.core.model.inventory.StockDetails
import com.luisete.queda.core.model.product.ProductName
import com.luisete.queda.core.model.product.ProductNameCreationResult
import com.luisete.queda.core.model.quantity.ExactQuantity
import java.util.UUID

/** Called only within the caller's Room transaction. Canonical products are shared; stock lots are independent. */
internal class InventoryLotWriter(private val database: QuedaDatabase) {
    suspend fun add(
        hid: String,
        name: String,
        quantity: ExactQuantity,
        details: StockDetails,
    ) {
        val canonical = (ProductName.create(name) as ProductNameCreationResult.Success).productName
        val existing = database.managementDao().productNamed(hid, canonical.normalizedKey)
        val product =
            existing ?: ProductEntity(
                UUID.randomUUID().toString(),
                hid,
                canonical.displayValue,
                canonical.normalizedKey,
                null,
            )
        if (existing == null) database.inventoryDao().insertProduct(product)
        val item =
            StockItemEntity(
                UUID.randomUUID().toString(),
                hid,
                product.id,
                "EXACT",
                quantity.amount.toPlainString(),
                quantity.unit.name,
                null,
            ).withDetails(details)
        database.inventoryDao().insertStockItem(item)
        database.syncDao().enqueue(
            PendingSyncOperationEntity(
                UUID.randomUUID().toString(),
                hid,
                item.id,
                "ADD_LOT",
                SyncPayloadCodec.encode(product, item),
                System.currentTimeMillis(),
            ),
        )
    }
}
