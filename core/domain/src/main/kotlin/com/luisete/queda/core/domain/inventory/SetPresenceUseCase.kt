package com.luisete.queda.core.domain.inventory

import com.luisete.queda.core.model.id.StockItemId
import javax.inject.Inject

class SetPresenceUseCase
    @Inject
    constructor(
        private val repository: InventoryRepository,
    ) {
        suspend operator fun invoke(
            stockItemId: StockItemId,
            isPresent: Boolean,
        ): QuantityMutationResult = repository.setPresence(stockItemId, isPresent)
    }
