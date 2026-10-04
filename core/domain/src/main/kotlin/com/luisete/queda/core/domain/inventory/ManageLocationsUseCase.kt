package com.luisete.queda.core.domain.inventory

import com.luisete.queda.core.model.inventory.StorageLocation
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface LocationsRepository {
    fun observeLocations(): Flow<List<StorageLocation>>

    suspend fun saveLocation(
        id: String?,
        name: String,
        archived: Boolean,
    ): StockWriteResult

    suspend fun resolveConflict(
        id: String,
        keepLocal: Boolean,
    ): StockWriteResult
}

class ManageLocationsUseCase
    @Inject
    constructor(private val repository: LocationsRepository) {
        fun observe(): Flow<List<StorageLocation>> = repository.observeLocations()

        suspend fun save(
            id: String?,
            name: String,
            archived: Boolean = false,
        ): StockWriteResult =
            if (name.trim().length !in 1..MAX_NAME) {
                StockWriteResult.INVALID
            } else {
                repository.saveLocation(id, name.trim(), archived)
            }

        suspend fun resolve(
            id: String,
            keepLocal: Boolean,
        ): StockWriteResult = if (id.isBlank()) StockWriteResult.INVALID else repository.resolveConflict(id, keepLocal)

        companion object {
            private const val MAX_NAME = 40
        }
    }
