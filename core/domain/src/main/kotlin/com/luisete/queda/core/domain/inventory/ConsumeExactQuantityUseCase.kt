package com.luisete.queda.core.domain.inventory

import com.luisete.queda.core.model.id.StockItemId
import com.luisete.queda.core.model.quantity.ExactQuantity
import javax.inject.Inject

class ConsumeExactQuantityUseCase
    @Inject
    constructor(
        private val inventoryRepository: InventoryRepository,
    ) {
        suspend operator fun invoke(
            stockItemId: StockItemId,
            toConsume: ExactQuantity,
        ): QuantityMutationResult = inventoryRepository.consumeExactQuantity(stockItemId, toConsume)
    }
