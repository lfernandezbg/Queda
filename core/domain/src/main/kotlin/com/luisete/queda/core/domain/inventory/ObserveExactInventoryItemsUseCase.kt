package com.luisete.queda.core.domain.inventory

import com.luisete.queda.core.model.inventory.InventoryItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveExactInventoryItemsUseCase
    @Inject
    constructor(
        private val repository: InventoryRepository,
        private val householdProvider: CurrentHouseholdIdProvider,
    ) {
        operator fun invoke(): Flow<List<InventoryItem>> {
            val householdId = householdProvider.currentHouseholdId()
            return repository.observeExactInventoryItems(householdId)
        }
    }
