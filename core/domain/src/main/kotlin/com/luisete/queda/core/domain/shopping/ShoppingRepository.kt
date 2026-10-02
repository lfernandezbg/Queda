package com.luisete.queda.core.domain.shopping

import com.luisete.queda.core.model.id.HouseholdId
import com.luisete.queda.core.model.id.ShoppingEntryId
import com.luisete.queda.core.model.shopping.ShoppingEntry
import kotlinx.coroutines.flow.Flow

interface ShoppingRepository {
    fun observe(householdId: HouseholdId): Flow<List<ShoppingEntry>>

    suspend fun add(
        householdId: HouseholdId,
        name: String,
        normalizedName: String,
    ): ShoppingResult

    suspend fun setPurchased(
        householdId: HouseholdId,
        id: ShoppingEntryId,
        purchased: Boolean,
    ): ShoppingResult
}

sealed interface ShoppingResult {
    data object Saved : ShoppingResult

    data object AlreadyExists : ShoppingResult

    data object NotFound : ShoppingResult

    data object StorageFailure : ShoppingResult
}
