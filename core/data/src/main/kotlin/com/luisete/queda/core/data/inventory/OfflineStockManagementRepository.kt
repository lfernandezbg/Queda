package com.luisete.queda.core.data.inventory

import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import com.luisete.queda.core.data.household.HouseholdSyncEngine
import com.luisete.queda.core.database.PendingSyncOperationEntity
import com.luisete.queda.core.database.QuedaDatabase
import com.luisete.queda.core.database.StockItemEntity
import com.luisete.queda.core.domain.inventory.CurrentHouseholdIdProvider
import com.luisete.queda.core.domain.inventory.ManageStockUseCase
import com.luisete.queda.core.domain.inventory.SelectedStock
import com.luisete.queda.core.domain.inventory.StockManagementRepository
import com.luisete.queda.core.domain.inventory.StockWriteResult
import com.luisete.queda.core.domain.quantity.QuantityOperations
import com.luisete.queda.core.domain.result.Success
import com.luisete.queda.core.model.inventory.StockDetails
import com.luisete.queda.core.model.product.ProductName
import com.luisete.queda.core.model.product.ProductNameCreationResult
import com.luisete.queda.core.model.quantity.ExactQuantity
import com.luisete.queda.core.model.quantity.PresenceQuantity
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

class OfflineStockManagementRepository
    @Inject
    constructor(
        private val database: QuedaDatabase,
        private val householdProvider: CurrentHouseholdIdProvider,
        private val sync: HouseholdSyncEngine,
    ) : StockManagementRepository {
        private val dao = database.managementDao()

        private val household: String get() = householdProvider.currentHouseholdId().value

        override suspend fun editDetails(
            id: String,
            expected: StockDetails,
            desired: StockDetails,
        ): StockWriteResult =
            write {
                val hid = household
                val item = dao.stock(hid, id)
                when {
                    item == null || item.details() != expected -> StockWriteResult.STALE
                    !locationAssignable(hid, desired.locationId, expected.locationId) -> StockWriteResult.INVALID
                    item.trackingMode == "PRESENCE" && desired.foodType.name == "PREPARED" -> StockWriteResult.INVALID
                    else -> {
                        val updated = item.withDetails(desired)
                        dao.updateStock(updated)
                        enqueue(updated, "DETAILS", detailsPayload(expected, desired))
                        StockWriteResult.SAVED
                    }
                }
            }

        @Suppress("CyclomaticComplexMethod")
        override suspend fun editItem(
            id: String,
            expected: StockDetails,
            desired: StockDetails,
            expectedName: String,
            desiredName: String,
        ): StockWriteResult {
            if (expectedName == desiredName) return editDetails(id, expected, desired)
            return write {
                val hid = household
                val item = dao.stock(hid, id) ?: return@write StockWriteResult.STALE
                val product = database.syncDao().product(item.productId) ?: return@write StockWriteResult.STALE
                val parsed =
                    ProductName.create(desiredName) as? ProductNameCreationResult.Success
                        ?: return@write StockWriteResult.INVALID
                val name = parsed.productName
                if (item.details() != expected || product.householdId != hid ||
                    product.displayName != expectedName
                ) {
                    return@write StockWriteResult.STALE
                }
                if (!locationAssignable(hid, desired.locationId, expected.locationId) ||
                    item.trackingMode == "PRESENCE" && desired.foodType.name == "PREPARED"
                ) {
                    return@write StockWriteResult.INVALID
                }
                val duplicate = dao.productNamed(hid, name.normalizedKey)
                if (duplicate != null && duplicate.id != product.id) return@write StockWriteResult.DUPLICATE
                val lots = database.syncDao().stockItems(hid).filter { it.productId == product.id }
                if (lots.size !in 1..ManageStockUseCase.MAX_SELECTION) return@write StockWriteResult.INVALID
                database.syncDao().updateProduct(
                    product.copy(displayName = name.displayValue, normalizedName = name.normalizedKey),
                )
                dao.updateStock(item.withDetails(desired))
                val batchId = UUID.randomUUID().toString()
                lots.forEach { lot ->
                    val payload =
                        JSONObject().put("productId", product.id)
                            .put("expectedName", product.displayName)
                            .put("expectedNormalizedName", product.normalizedName)
                            .put("desiredName", name.displayValue)
                            .put("desiredNormalizedName", name.normalizedKey)
                            .apply {
                                if (lot.id == id) {
                                    put("expected", detailsJson(expected))
                                    put("desired", detailsJson(desired))
                                }
                            }.toString()
                    enqueue(lot, "RENAME", payload, batchId)
                }
                StockWriteResult.SAVED
            }
        }

        override suspend fun addLot(
            name: String,
            quantity: ExactQuantity,
            details: StockDetails,
        ): StockWriteResult =
            write {
                val hid = household
                if (!locationAssignable(hid, details.locationId)) {
                    StockWriteResult.INVALID
                } else {
                    InventoryLotWriter(database).add(hid, name, quantity, details)
                    StockWriteResult.SAVED
                }
            }

        override suspend fun consumeSelection(items: List<SelectedStock>): StockWriteResult =
            mutateSelection(
                items,
                null,
                true,
            )

        override suspend fun moveSelection(
            items: List<SelectedStock>,
            locationId: String?,
        ): StockWriteResult = mutateSelection(items, locationId, false)

        private suspend fun mutateSelection(
            items: List<SelectedStock>,
            locationId: String?,
            consume: Boolean,
        ): StockWriteResult =
            write {
                val hid = household
                val stocks = items.map { dao.stock(hid, it.id) }
                val valid =
                    stocks.zip(items).all { (stock, selected) ->
                        stock != null && stock.quantity() == selected.expectedQuantity
                    }
                when {
                    !valid -> StockWriteResult.STALE
                    !consume && !locationAssignable(hid, locationId) -> StockWriteResult.INVALID
                    consume && stocks.any { !consumable(checkNotNull(it)) } -> StockWriteResult.INVALID
                    else -> {
                        val updates =
                            stocks.zip(items).map { (source, selected) ->
                                plannedUpdate(checkNotNull(source), selected, locationId, consume)
                            }
                        if (updates.any { it == null }) return@write StockWriteResult.INVALID
                        val batchId = UUID.randomUUID().toString()
                        updates.filterNotNull().forEach { (updated, payload) ->
                            dao.updateStock(updated)
                            enqueue(updated, if (consume) "CONSUME_ALL" else "DETAILS", payload, batchId)
                        }
                        StockWriteResult.SAVED
                    }
                }
            }

        @Suppress("ReturnCount")
        private fun plannedUpdate(
            item: StockItemEntity,
            selected: SelectedStock,
            locationId: String?,
            consume: Boolean,
        ): Pair<StockItemEntity, String>? {
            if (!consume) {
                val updated = item.copy(locationId = locationId)
                return updated to detailsPayload(item.details(), updated.details())
            }
            val requested = selected.toConsume
            val remainder =
                if (requested == null) {
                    null
                } else {
                    val available = item.quantity() as? ExactQuantity ?: return null
                    when (val result = QuantityOperations.consume(available, requested)) {
                        is Success -> result.value
                        else -> return null
                    }
                }
            val updated =
                item.copy(
                    quantityAmount =
                        if (item.trackingMode == "EXACT") remainder?.amount?.toPlainString() ?: "0" else null,
                    quantityUnit = remainder?.unit?.name ?: item.quantityUnit,
                    isPresent = if (item.trackingMode == "PRESENCE") false else null,
                )
            val payload =
                JSONObject().put("expectedAmount", item.quantityAmount)
                    .put("expectedUnit", item.quantityUnit)
                    .put("expectedPresence", item.isPresent)
                    .apply {
                        requested?.let {
                            put("consumeAmount", it.amount.toPlainString())
                            put("consumeUnit", it.unit.name)
                        }
                    }.toString()
            return updated to payload
        }

        private suspend fun locationAssignable(
            hid: String,
            id: String?,
            previous: String? = null,
        ): Boolean = id == null || dao.location(hid, id)?.let { !it.archived || id == previous } == true

        private suspend fun enqueue(
            item: StockItemEntity,
            action: String,
            payload: String,
            batchId: String = "",
        ) {
            database.syncDao().enqueue(
                PendingSyncOperationEntity(
                    UUID.randomUUID().toString(),
                    item.householdId,
                    item.id,
                    action,
                    payload,
                    System.currentTimeMillis(),
                    batchId,
                ),
            )
        }

        private suspend fun write(action: suspend () -> StockWriteResult): StockWriteResult =
            try {
                val result = database.withTransaction { action() }
                if (result == StockWriteResult.SAVED) sync.onLocalChange()
                result
            } catch (_: SQLiteException) {
                StockWriteResult.STORAGE_FAILURE
            }
    }

internal fun detailsPayload(
    expected: StockDetails,
    desired: StockDetails,
): String =
    JSONObject()
        .put("expected", detailsJson(expected)).put("desired", detailsJson(desired)).toString()

internal fun detailsJson(details: StockDetails): JSONObject =
    JSONObject().apply {
        put("locationId", details.locationId ?: JSONObject.NULL)
        put("foodType", details.foodType.name)
        put("label", details.label ?: JSONObject.NULL)
        put("preparedOn", details.preparedOn?.toString() ?: JSONObject.NULL)
        put("bestBefore", details.bestBefore?.toString() ?: JSONObject.NULL)
    }

private fun consumable(item: StockItemEntity): Boolean =
    when (val quantity = item.quantity()) {
        is ExactQuantity -> quantity.amount.signum() > 0
        is PresenceQuantity -> quantity.isPresent
        else -> false
    }
