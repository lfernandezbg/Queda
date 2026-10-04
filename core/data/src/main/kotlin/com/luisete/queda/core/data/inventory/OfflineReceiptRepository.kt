package com.luisete.queda.core.data.inventory

import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import com.luisete.queda.core.data.household.HouseholdSyncEngine
import com.luisete.queda.core.database.QuedaDatabase
import com.luisete.queda.core.database.ReceiptDraftEntity
import com.luisete.queda.core.domain.inventory.CurrentHouseholdIdProvider
import com.luisete.queda.core.domain.inventory.ExactQuantityInputParser
import com.luisete.queda.core.domain.inventory.ExactQuantityInputResult
import com.luisete.queda.core.domain.inventory.ReceiptRepository
import com.luisete.queda.core.domain.inventory.ReceiptResult
import com.luisete.queda.core.model.inventory.ReceiptDraft
import com.luisete.queda.core.model.inventory.StockDetails
import com.luisete.queda.core.model.product.ProductName
import com.luisete.queda.core.model.product.ProductNameCreationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OfflineReceiptRepository
    @Inject
    constructor(
        private val database: QuedaDatabase,
        private val householdProvider: CurrentHouseholdIdProvider,
        private val sync: HouseholdSyncEngine,
    ) : ReceiptRepository {
        private val dao = database.managementDao()

        private fun household(): String = householdProvider.currentHouseholdId().value

        override fun observeDraft(): Flow<ReceiptDraft?> {
            return dao.draft(household()).map { it?.let(ReceiptDraftCodec::decode) }
        }

        override suspend fun save(draft: ReceiptDraft): ReceiptResult =
            write {
                val hid = household()
                val previous = dao.receipt(hid, draft.fingerprint)
                when {
                    previous?.imported == true || previous != null && previous.id != draft.id -> ReceiptResult.DUPLICATE
                    else -> {
                        dao.saveDraft(
                            ReceiptDraftEntity(
                                draft.id,
                                hid,
                                draft.fingerprint,
                                ReceiptDraftCodec.encode(draft),
                                false,
                                previous?.createdAt ?: System.currentTimeMillis(),
                            ),
                        )
                        ReceiptResult.SAVED
                    }
                }
            }

        override suspend fun discard(id: String) {
            dao.discardDraft(household(), id)
        }

        override suspend fun import(draft: ReceiptDraft): ReceiptResult =
            write {
                val hid = household()
                val existing = dao.receipt(hid, draft.fingerprint)
                val selected = draft.lines.filter { it.selected }
                when {
                    existing?.imported == true -> ReceiptResult.DUPLICATE
                    existing == null || existing.id != draft.id -> ReceiptResult.INVALID
                    selected.size !in 1..MAX_IMPORT || draft.lines.map { it.id }.distinct().size != draft.lines.size ||
                        selected.any { line ->
                            ProductName.create(line.name) !is ProductNameCreationResult.Success ||
                                !line.reviewed ||
                                ExactQuantityInputParser.parse(
                                    line.quantity,
                                    line.unit,
                                ) !is ExactQuantityInputResult.Success ||
                                line.locationId?.let { dao.location(hid, it)?.archived != false } == true
                        }
                    -> ReceiptResult.INVALID
                    else -> {
                        val writer = InventoryLotWriter(database)
                        selected.forEach { line ->
                            val quantity =
                                (
                                    ExactQuantityInputParser.parse(
                                        line.quantity,
                                        line.unit,
                                    ) as ExactQuantityInputResult.Success
                                ).quantity
                            writer.add(hid, line.name, quantity, StockDetails(locationId = line.locationId))
                        }
                        // Keep only the fingerprint after import; payment text is no longer needed.
                        dao.saveDraft(existing.copy(imported = true, payload = "[]"))
                        ReceiptResult.IMPORTED
                    }
                }
            }

        private suspend fun write(action: suspend () -> ReceiptResult): ReceiptResult =
            try {
                val result = database.withTransaction { action() }
                if (result == ReceiptResult.IMPORTED) sync.onLocalChange()
                result
            } catch (_: SQLiteException) {
                ReceiptResult.STORAGE_FAILURE
            }

        companion object {
            private const val MAX_IMPORT = 60
        }
    }
