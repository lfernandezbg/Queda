package com.luisete.queda.core.data.inventory

import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import com.luisete.queda.core.data.household.HouseholdSyncEngine
import com.luisete.queda.core.database.LocationEntity
import com.luisete.queda.core.database.QuedaDatabase
import com.luisete.queda.core.domain.inventory.CurrentHouseholdIdProvider
import com.luisete.queda.core.domain.inventory.LocationsRepository
import com.luisete.queda.core.domain.inventory.StockWriteResult
import com.luisete.queda.core.model.inventory.StorageLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

class OfflineLocationsRepository
    @Inject
    constructor(
        private val database: QuedaDatabase,
        private val householdProvider: CurrentHouseholdIdProvider,
        private val sync: HouseholdSyncEngine,
    ) : LocationsRepository {
        private val dao = database.managementDao()
        private val household: String get() = householdProvider.currentHouseholdId().value

        override fun observeLocations(): Flow<List<StorageLocation>> =
            dao.locations(household).map { rows ->
                rows.map { StorageLocation(it.id, it.name, it.archived, it.conflictPayload != null) }
            }

        override suspend fun saveLocation(
            id: String?,
            name: String,
            archived: Boolean,
        ): StockWriteResult =
            write {
                val normalized = name.trim().lowercase(Locale.ROOT)
                val duplicate = dao.locationNamed(household, normalized)
                val current = id?.let { dao.location(household, it) }
                when {
                    name.trim().length !in 1..MAX_NAME -> StockWriteResult.INVALID
                    id != null && current == null -> StockWriteResult.STALE
                    duplicate != null && duplicate.id != id -> StockWriteResult.DUPLICATE
                    else -> {
                        val candidate = stableLocationId(household, normalized)
                        val newId =
                            id ?: if (dao.location(household, candidate) == null) {
                                candidate
                            } else {
                                UUID.randomUUID().toString()
                            }
                        dao.saveLocation(
                            LocationEntity(
                                newId,
                                household,
                                name.trim(),
                                normalized,
                                archived,
                                current?.revision ?: 0,
                                syncPending = true,
                                conflictPayload = current?.conflictPayload,
                            ),
                        )
                        StockWriteResult.SAVED
                    }
                }
            }

        override suspend fun resolveConflict(
            id: String,
            keepLocal: Boolean,
        ): StockWriteResult =
            write {
                val current = dao.location(household, id)
                val payload = current?.conflictPayload
                if (current == null || payload == null) {
                    StockWriteResult.STALE
                } else {
                    val remote = JSONObject(payload)
                    val desired =
                        if (keepLocal) {
                            current.copy(
                                revision = remote.getLong("revision"),
                                syncPending = true,
                                conflictPayload = null,
                            )
                        } else {
                            current.copy(
                                name = remote.getString("name"),
                                normalizedName = remote.getString("normalizedName"),
                                archived = remote.getBoolean("archived"),
                                revision = remote.getLong("revision"),
                                syncPending = false,
                                conflictPayload = null,
                            )
                        }
                    val duplicate = dao.locationNamed(household, desired.normalizedName)
                    if (duplicate != null && duplicate.id != id) {
                        StockWriteResult.DUPLICATE
                    } else {
                        dao.saveLocation(desired)
                        StockWriteResult.SAVED
                    }
                }
            }

        private suspend fun write(action: suspend () -> StockWriteResult): StockWriteResult =
            try {
                val result = database.withTransaction { action() }
                if (result == StockWriteResult.SAVED) sync.onLocalChange()
                result
            } catch (_: SQLiteException) {
                StockWriteResult.STORAGE_FAILURE
            }

        companion object {
            private const val MAX_NAME = 40
        }
    }

private fun stableLocationId(
    household: String,
    normalized: String,
): String =
    MessageDigest.getInstance("SHA-256")
        .digest("$household:$normalized".toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and HEX_MASK) }

private const val HEX_MASK = 0xff
